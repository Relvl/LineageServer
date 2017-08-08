package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.communitybbs.CommunityBoard;

public class RequestBBSwrite extends L2GameClientPacket {
    private String url;
    private String _arg1;
    private String _arg2;
    private String _arg3;
    private String _arg4;
    private String _arg5;

    @Override
    protected void readImpl() {
        url = readS();
        _arg1 = readS();
        _arg2 = readS();
        _arg3 = readS();
        _arg4 = readS();
        _arg5 = readS();
    }

    /*  >>> url = Mail
        >>> _arg1 = Send
        >>> _arg2 = %postId%
        >>> _arg3 = recipient
        >>> _arg4 = title
        >>> _arg5 = body
        */
    @Override
    protected void runImpl() {
        CommunityBoard.getInstance().handleWriteCommand(getClient().getActiveChar(), url, _arg1, _arg2, _arg3, _arg4, _arg5);
    }

    @Override
    public String toString() {
        return "RequestBBSwrite{" +
                "url='" + url + '\'' +
                ", _arg1='" + _arg1 + '\'' +
                ", _arg2='" + _arg2 + '\'' +
                ", _arg3='" + _arg3 + '\'' +
                ", _arg4='" + _arg4 + '\'' +
                ", _arg5='" + _arg5 + '\'' +
                '}';
    }
}