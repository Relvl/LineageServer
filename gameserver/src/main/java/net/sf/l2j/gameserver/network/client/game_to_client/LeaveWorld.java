package net.sf.l2j.gameserver.network.client.game_to_client;

public class LeaveWorld extends L2GameServerPacket {
    public static final LeaveWorld STATIC_PACKET = new LeaveWorld();

    private LeaveWorld() {
    }

    @Override
    protected final void writeImpl() {
        writeC(0x7e);
    }
}