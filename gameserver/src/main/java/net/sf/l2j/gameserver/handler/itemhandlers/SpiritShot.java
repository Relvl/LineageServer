package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.ShotType;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.MagicSkillUse;
import net.sf.l2j.gameserver.util.Broadcast;

public class SpiritShot implements IItemHandler {
    @Override
    public void useItem(L2Playable playable, L2ItemInstance item, boolean forceUse) {
        if (!(playable instanceof L2PcInstance)) { return; }

        final L2PcInstance activeChar = (L2PcInstance) playable;
        final L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
        final Weapon weaponItem = activeChar.getActiveWeaponItem();
        final int itemId = item.getItemId();

        // Check if sps can be used
        if (weaponInst == null || weaponItem.getSpiritShotCount() == 0) {
            if (!activeChar.getAutoSoulShot().contains(itemId)) { activeChar.sendPacket(SystemMessageId.CANNOT_USE_SPIRITSHOTS); }
            return;
        }

        // Check if sps is already active
        if (activeChar.isChargedShot(ShotType.SPIRITSHOT)) { return; }

        if (weaponItem.getCrystalType() != item.getItem().getCrystalType()) {
            if (!activeChar.getAutoSoulShot().contains(itemId)) { activeChar.sendPacket(SystemMessageId.SPIRITSHOTS_GRADE_MISMATCH); }

            return;
        }

        // Consume sps if player has enough of them
        if (activeChar.getInventory().destroyItem(null, item, weaponItem.getSpiritShotCount(), null, false) == null) {
            if (!activeChar.disableAutoShot(itemId)) { activeChar.sendPacket(SystemMessageId.NOT_ENOUGH_SPIRITSHOTS); }
            return;
        }

        final IntIntHolder[] skills = item.getItem().getSkills();

        activeChar.sendPacket(SystemMessageId.ENABLED_SPIRITSHOT);
        activeChar.setChargedShot(ShotType.SPIRITSHOT, true);
        Broadcast.toSelfAndKnownPlayersInRadiusSq(activeChar, new MagicSkillUse(activeChar, activeChar, skills[0].getId(), 1, 0, 0), 360000);
    }
}