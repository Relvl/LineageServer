package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeFlagInstance;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.templates.StatsSet;

public class L2SkillSiegeFlag extends L2Skill {
    private final boolean _isAdvanced;

    public L2SkillSiegeFlag(StatsSet set) {
        super(set);
        _isAdvanced = set.getBool("isAdvanced", false);
    }

    @Override
    public void useSkill(L2Character activeChar, L2Object[] targets) {
        if (!(activeChar instanceof L2PcInstance)) { return; }

        L2PcInstance player = activeChar.getActingPlayer();

        if (!player.isClanLeader()) { return; }
        if (!checkIfOkToPlaceFlag(player, true)) { return; }

        // Template initialization
        StatsSet npcDat = new StatsSet();

        npcDat.set("id", 35062);
        npcDat.set("type", "");

        npcDat.set("name", "Headquarters");
        npcDat.set("title", player.getClan().getName());

        npcDat.set("hp", (_isAdvanced) ? 100000 : 50000);
        npcDat.set("mp", 0);

        npcDat.set("radius", 10);
        npcDat.set("height", 80);

        npcDat.set("pAtk", 0);
        npcDat.set("mAtk", 0);
        npcDat.set("pDef", 500);
        npcDat.set("mDef", 500);

        npcDat.set("runSpd", 0); // Have to keep this, static object MUST BE 0 (critical error otherwise).

        // Spawn a new flag.
        L2SiegeFlagInstance flag = new L2SiegeFlagInstance(player, IdFactory.getInstance().getNextId(), new NpcTemplate(npcDat));
        flag.setCurrentHp(flag.getMaxHp());
        flag.setHeading(player.getHeading());
        flag.spawnMe(player.getX(), player.getY(), player.getZ() + 50);
    }

    /**
     * @param activeChar  The L2Character of the character placing the flag
     * @param isCheckOnly if false, it will send a notification to the player telling him why it failed
     * @return true if character clan place a flag
     */
    public static boolean checkIfOkToPlaceFlag(L2Character activeChar, boolean isCheckOnly) {
        if (!activeChar.isPlayer()) { return false; }

        L2PcInstance player = activeChar.getActingPlayer();
        Castle castle = CastleManager.getInstance().getCastle(activeChar);

        SystemMessage sm;
        if (castle == null || !castle.getSiege().isInProgress() || castle.getSiege().getAttackerClan(player.getClan()) == null) {
            sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(247);
        }
        else if (!player.isClanLeader()) {
            sm = SystemMessage.getSystemMessage(SystemMessageId.ONLY_CLAN_LEADER_CAN_ISSUE_COMMANDS);
        }
        else if (castle.getSiege().getAttackerClan(player.getClan()).getFlags().size() >= SiegeManager.FLAGS_MAX_COUNT) {
            sm = SystemMessage.getSystemMessage(SystemMessageId.NOT_ANOTHER_HEADQUARTERS);
        }
        else if (!player.isInsideZone(ZoneId.HQ)) {
            sm = SystemMessage.getSystemMessage(SystemMessageId.NOT_SET_UP_BASE_HERE);
        }
        else if (!player.getKnownList().getKnownTypeInRadius(L2SiegeFlagInstance.class, 400).isEmpty()) {
            sm = SystemMessage.getSystemMessage(SystemMessageId.HEADQUARTERS_TOO_CLOSE);
        }
        else { return true; }

        if (!isCheckOnly) { player.sendPacket(sm); }

        return false;
    }
}