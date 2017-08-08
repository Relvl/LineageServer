package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.communitybbs.CommunityBoard;

public final class RequestShowBoard extends L2GameClientPacket {
    private int _unknown;

    @Override
    protected void readImpl() {
        _unknown = readD();
    }

    @Override
    protected void runImpl() {
        CommunityBoard.handleCommand(getClient().getActiveChar(), "_bbshome");
    }
}