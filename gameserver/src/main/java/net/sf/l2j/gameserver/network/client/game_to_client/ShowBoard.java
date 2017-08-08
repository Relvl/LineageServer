package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.commons.lang.StringUtil;

import java.util.List;

public class ShowBoard extends L2GameServerPacket {
    public static final ShowBoard CLOSEBOARD = new ShowBoard();
    public static final ShowBoard STATIC_SHOWBOARD_102 = new ShowBoard(null, "102");
    public static final ShowBoard STATIC_SHOWBOARD_103 = new ShowBoard(null, "103");

    private final StringBuilder html = new StringBuilder();
    private final boolean open;

    public ShowBoard(String htmlCode, String id) {
        open = true;
        StringUtil.append(html, id, "\u0008", htmlCode);
    }

    public ShowBoard(List<String> arg) {
        open = true;
        html.append("1002\u0008");
        for (String str : arg) {
            StringUtil.append(html, str, " \u0008");
        }
    }

    public ShowBoard() {
        open = false;
    }

    @Override
    protected final void writeImpl() {
        writeC(0x6e);
        writeC(open ? 0x01 : 0x00); // 1 to show, 0 to hide
        writeS("bypass _bbshome");
        writeS("bypass _bbsgetfav");
        writeS("bypass _bbsloc");
        writeS("bypass _bbsclan");
        writeS("bypass _bbsmemo");
        writeS("bypass _maillist_0_1_0_");
        writeS("bypass _friendlist_0_");
        writeS("bypass bbs_add_fav");
        writeS(html.toString());
    }
}