package net.sf.l2j.gameserver.communitybbs.Manager;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.client.game_to_client.ShowBoard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseBBSManager {
    protected static final String CB_PATH = "data/html/CommunityBoard/";
    protected static final Logger LOGGER = LoggerFactory.getLogger(BaseBBSManager.class);

    public static void separateAndSend(String html, L2PcInstance acha) {
        if (html == null || acha == null) { return; }

        if (html.length() < 4090) {
            acha.sendPacket(new ShowBoard(html, "101"));
            acha.sendPacket(ShowBoard.STATIC_SHOWBOARD_102);
            acha.sendPacket(ShowBoard.STATIC_SHOWBOARD_103);
        }
        else if (html.length() < 8180) {
            acha.sendPacket(new ShowBoard(html.substring(0, 4090), "101"));
            acha.sendPacket(new ShowBoard(html.substring(4090, html.length()), "102"));
            acha.sendPacket(ShowBoard.STATIC_SHOWBOARD_103);
        }
        else if (html.length() < 12270) {
            acha.sendPacket(new ShowBoard(html.substring(0, 4090), "101"));
            acha.sendPacket(new ShowBoard(html.substring(4090, 8180), "102"));
            acha.sendPacket(new ShowBoard(html.substring(8180, html.length()), "103"));
        }
    }

    protected static void send1001(String html, L2PcInstance acha) {
        if (html.length() < 8180) { acha.sendPacket(new ShowBoard(html, "1001")); }
    }

    protected static void send1002(L2PcInstance acha) {
        send1002(acha, " ", " ", "0");
    }

    protected static void send1002(L2PcInstance activeChar, String string, String string2, String string3) {
        List<String> _arg = new ArrayList<>();
        _arg.add("0");
        _arg.add("0");
        _arg.add("0");
        _arg.add("0");
        _arg.add("0");
        _arg.add("0");
        _arg.add(activeChar.getName());
        _arg.add(Integer.toString(activeChar.getObjectId()));
        _arg.add(activeChar.getAccountName());
        _arg.add("9");
        _arg.add(string2);
        _arg.add(string2);
        _arg.add(string);
        _arg.add(string3);
        _arg.add(string3);
        _arg.add("0");
        _arg.add("0");
        activeChar.sendPacket(new ShowBoard(_arg));
    }

    public void parseCmd(String command, L2PcInstance activeChar) {
        separateAndSend("<html><body><br><br><center>The command: " + command + " isn't implemented.</center></body></html>", activeChar);
    }

    public void parseWrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar) {
        separateAndSend("<html><body><br><br><center>The command: " + ar1 + " isn't implemented.</center></body></html>", activeChar);
    }

    /**
     * Loads an HTM located in the default CB path.
     *
     * @param file       : the file to load.
     * @param activeChar : the requester.
     */
    protected void loadStaticHtm(String file, L2PcInstance activeChar) {
        separateAndSend(HtmCache.getInstance().getHtm(CB_PATH + getFolder() + file), activeChar);
    }

    /**
     * That method is overidden in every board type. It allows to switch of folders following the board.
     *
     * @return the folder.
     */
    protected String getFolder() {
        return "";
    }
}