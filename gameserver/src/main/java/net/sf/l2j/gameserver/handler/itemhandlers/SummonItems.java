package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SummonItemsData;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2XmassTreeInstance;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.EItemLocation;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.SummonItem;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.*;
import net.sf.l2j.gameserver.util.Broadcast;

public class SummonItems implements IItemHandler {
    @Override
    public void useItem(L2Playable playable, L2ItemInstance item, boolean forceUse) {
        if (!(playable instanceof L2PcInstance)) { return; }

        L2PcInstance player = (L2PcInstance) playable;

        if (player.isSitting()) {
            player.sendPacket(SystemMessageId.CANT_MOVE_SITTING);
            return;
        }

        if (player.inObserverMode()) { return; }

        if (player.isAllSkillsDisabled() || player.isCastingNow()) { return; }

        SummonItem sitem = SummonItemsData.getSummonItem(item.getItemId());

        if ((player.getPet() != null || player.isMounted()) && sitem.isPetSummon()) {
            player.sendPacket(SystemMessageId.SUMMON_ONLY_ONE);
            return;
        }

        if (player.isAttackingNow()) {
            player.sendPacket(SystemMessageId.YOU_CANNOT_SUMMON_IN_COMBAT);
            return;
        }

        int npcId = sitem.getNpcId();
        if (npcId == 0) { return; }

        NpcTemplate npcTemplate = NpcTable.getInstance().getTemplate(npcId);
        if (npcTemplate == null) { return; }

        player.stopMove(null);

        switch (sitem.getType()) {
            case 0: // static summons (like Christmas tree)
                try {
                    for (L2XmassTreeInstance ch : player.getKnownList().getKnownTypeInRadius(L2XmassTreeInstance.class, 1200)) {
                        if (npcTemplate.isSpecialTree()) {
                            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANNOT_SUMMON_S1_AGAIN).addCharName(ch));
                            return;
                        }
                    }

                    if (player.getInventory().destroyItem(EItemProcessPurpose.SUMMON, item, 1, null, false) != null) {
                        L2Spawn spawn = new L2Spawn(npcTemplate);
                        spawn.setLocx(player.getX());
                        spawn.setLocy(player.getY());
                        spawn.setLocz(player.getZ());
                        spawn.stopRespawn();

                        L2Npc npc = spawn.doSpawn(true);
                        npc.setTitle(player.getName());
                        npc.setIsRunning(false); // broadcast info
                    }
                }
                catch (Exception e) {
                    player.sendPacket(SystemMessageId.TARGET_CANT_FOUND);
                }
                break;
            case 1: // pet summons
                L2Object oldTarget = player.getTarget();
                player.setTarget(player);
                Broadcast.toSelfAndKnownPlayers(player, new MagicSkillUse(player, 2046, 1, 5000, 0));
                player.setTarget(oldTarget);
                player.sendPacket(new SetupGauge(0, 5000));
                player.sendPacket(SystemMessageId.SUMMON_A_PET);
                player.setIsCastingNow(true);

                ThreadPoolManager.getInstance().scheduleGeneral(new PetSummonFinalizer(player, npcTemplate, item), 5000);
                break;
            case 2: // wyvern
                player.mount(sitem.getNpcId(), item.getObjectId(), true);
                break;
        }
    }

    static class PetSummonFeedWait implements Runnable {
        private final L2PcInstance _activeChar;
        private final L2PetInstance _petSummon;

        PetSummonFeedWait(L2PcInstance activeChar, L2PetInstance petSummon) {
            _activeChar = activeChar;
            _petSummon = petSummon;
        }

        @Override
        public void run() {
            try {
                if (_petSummon.getCurrentFed() <= 0) { _petSummon.unSummon(_activeChar); }
                else { _petSummon.startFeed(); }
            }
            catch (Exception e) {
                LOGGER.error("", e);
            }
        }
    }

    // TODO: this should be inside skill handler
    static class PetSummonFinalizer implements Runnable {
        private final L2PcInstance _activeChar;
        private final L2ItemInstance _item;
        private final NpcTemplate _npcTemplate;

        PetSummonFinalizer(L2PcInstance activeChar, NpcTemplate npcTemplate, L2ItemInstance item) {
            _activeChar = activeChar;
            _npcTemplate = npcTemplate;
            _item = item;
        }

        @Override
        public void run() {
            try {
                _activeChar.sendPacket(new MagicSkillLaunched(_activeChar, 2046, 1));
                _activeChar.setIsCastingNow(false);

                // check for summon item validity
                if (_item == null || _item.getOwnerId() != _activeChar.getObjectId() || _item.getLocation() != EItemLocation.INVENTORY) { return; }

                L2PetInstance petSummon = L2PetInstance.spawnPet(_npcTemplate, _activeChar, _item);
                if (petSummon == null) { return; }

                petSummon.setShowSummonAnimation(true);
                petSummon.setTitle(_activeChar.getName());

                if (!petSummon.isRespawned()) {
                    petSummon.setCurrentHp(petSummon.getMaxHp());
                    petSummon.setCurrentMp(petSummon.getMaxMp());
                    petSummon.getStat().setExp(petSummon.getExpForThisLevel());
                    petSummon.setCurrentFed(petSummon.getMaxFed());
                }

                petSummon.setRunning();

                if (!petSummon.isRespawned()) { petSummon.store(); }

                _activeChar.setPet(petSummon);

                petSummon.spawnMe(_activeChar.getX() + 50, _activeChar.getY() + 100, _activeChar.getZ());
                petSummon.startFeed();
                _item.setEnchantLevel(petSummon.getLevel());

                if (petSummon.getCurrentFed() <= 0) { ThreadPoolManager.getInstance().scheduleGeneral(new PetSummonFeedWait(_activeChar, petSummon), 60000); }
                else { petSummon.startFeed(); }

                petSummon.setFollow(true);

                petSummon.getOwner().sendPacket(new PetItemList(petSummon));
                petSummon.broadcastStatusUpdate();
            }
            catch (Exception e) {
                LOGGER.error("", e);
            }
        }
    }
}
