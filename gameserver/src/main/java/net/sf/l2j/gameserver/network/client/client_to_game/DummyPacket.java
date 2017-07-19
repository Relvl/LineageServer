package net.sf.l2j.gameserver.network.client.client_to_game;

@Deprecated
public final class DummyPacket extends L2GameClientPacket {
    @Override
    protected void readImpl() { }

    @Override
    public void runImpl() { }
}