package net.sf.l2j.gameserver.communitybbs.deprecated;

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

    public static void separateAndSend(String html, L2PcInstance player) {
        if (html == null || player == null) { return; }

        if (html.length() < 4090) {
            player.sendPacket(new ShowBoard(html, "101"));
            player.sendPacket(ShowBoard.STATIC_SHOWBOARD_102);
            player.sendPacket(ShowBoard.STATIC_SHOWBOARD_103);
        }
        else if (html.length() < 8180) {
            player.sendPacket(new ShowBoard(html.substring(0, 4090), "101"));
            player.sendPacket(new ShowBoard(html.substring(4090, html.length()), "102"));
            player.sendPacket(ShowBoard.STATIC_SHOWBOARD_103);
        }
        else if (html.length() < 12270) {
            player.sendPacket(new ShowBoard(html.substring(0, 4090), "101"));
            player.sendPacket(new ShowBoard(html.substring(4090, 8180), "102"));
            player.sendPacket(new ShowBoard(html.substring(8180, html.length()), "103"));
        }
    }

    protected static void send1001(String html, L2PcInstance player) {
        if (html.length() < 8180) {
            player.sendPacket(new ShowBoard(html, "1001"));
        }
    }

    protected static void send1002(L2PcInstance player) {
        send1002(player, " ", " ", "0");
    }

    protected static void send1002(L2PcInstance activeChar, String string, String string2, String string3) {
        List<String> strings = new ArrayList<>();
        strings.add("0");
        strings.add("0");
        strings.add("0");
        strings.add("0");
        strings.add("0");
        strings.add("0");
        strings.add(activeChar.getName());
        strings.add(Integer.toString(activeChar.getObjectId()));
        strings.add(activeChar.getAccountName());
        strings.add("9");
        strings.add(string2);
        strings.add(string2);
        strings.add(string);
        strings.add(string3);
        strings.add(string3);
        strings.add("0");
        strings.add("0");
        activeChar.sendPacket(new ShowBoard(strings));
    }

    public void parseCmd(String command, L2PcInstance activeChar) {
        separateAndSend("<html><body><br><br><center>The command: " + command + " isn't implemented.</center></body></html>", activeChar);
    }

    public void parseWrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar) {
        separateAndSend("<html><body><br><br><center>The command: " + ar1 + " isn't implemented.</center></body></html>", activeChar);
    }

    protected void loadStaticHtm(String file, L2PcInstance activeChar) {
        separateAndSend(HtmCache.getInstance().getHtm(CB_PATH + getFolder() + file), activeChar);
    }

    protected String getFolder() { return ""; }
}