package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.gameserver.model.actor.L2Playable;

public class RelationChanged extends L2GameServerPacket {
    public static final int RELATION_PVP_FLAG = 0x00002; // pvp ???
    public static final int RELATION_HAS_KARMA = 0x00004; // karma ???
    public static final int RELATION_LEADER = 0x00080; // leader
    public static final int RELATION_INSIEGE = 0x00200; // true if in siege
    public static final int RELATION_ATTACKER = 0x00400; // true when attacker
    public static final int RELATION_ALLY = 0x00800; // blue siege icon, cannot have if red
    public static final int RELATION_ENEMY = 0x01000; // true when red icon, doesn't matter with blue
    public static final int RELATION_MUTUAL_WAR = 0x08000; // double fist
    public static final int RELATION_1SIDED_WAR = 0x10000; // single fist

    private final int objectId;
    private final int relation;
    private final boolean autoAttackable;
    private final int karma;
    private final int pvpFlag;

    public RelationChanged(L2Playable cha, int relation, boolean autoattackable) {
        objectId = cha.getObjectId();
        this.relation = relation;
        autoAttackable = autoattackable;
        karma = cha.getKarma();
        pvpFlag = cha.getPvpFlag();
    }

    @Override
    protected final void writeImpl() {
        writeC(0xce);
        writeD(objectId);
        writeD(relation);
        writeD(autoAttackable);
        writeD(karma);
        writeD(pvpFlag);
    }
}