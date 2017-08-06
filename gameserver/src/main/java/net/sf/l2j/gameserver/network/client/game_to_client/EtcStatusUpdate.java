package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.playerpart.variables.EPlayerVariableKey;
import net.sf.l2j.gameserver.templates.skills.L2EffectFlag;

public class EtcStatusUpdate extends L2GameServerPacket {
    private final L2PcInstance player;

    public EtcStatusUpdate(L2PcInstance activeChar) {
        player = activeChar;
    }

    @Override
    protected void writeImpl() {
        writeC(0xF3);
        writeD(player.getCharges());
        writeD(player.getWeightPenalty());
        writeD((player.isInRefusalMode() || player.isChatBanned()) ? 1 : 0);
        writeD(player.isInsideZone(ZoneId.DANGER_AREA) ? 1 : 0);
        writeD((player.getExpertiseWeaponPenalty() || player.getExpertiseArmorPenalty() > 0) ? 1 : 0);
        writeD(player.isAffected(L2EffectFlag.CHARM_OF_COURAGE) ? 1 : 0);
        writeD(player.variables().getInteger(EPlayerVariableKey.DEATH_PENALTY_LEVEL, 0));
    }
}