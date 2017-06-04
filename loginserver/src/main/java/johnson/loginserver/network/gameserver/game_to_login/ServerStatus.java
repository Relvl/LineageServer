package johnson.loginserver.network.gameserver.game_to_login;

import johnson.loginserver.GameServerInfo;
import johnson.loginserver.GameServerTable;
import johnson.loginserver.network.gameserver.ABaseClientPacket;

public class ServerStatus extends ABaseClientPacket {

    public static final int SERVER_LIST_STATUS = 0x01;
    public static final int SERVER_LIST_CLOCK = 0x02;
    public static final int SERVER_LIST_SQUARE_BRACKET = 0x03;
    public static final int MAX_PLAYERS = 0x04;
    public static final int TEST_SERVER = 0x05;

    public static final int STATUS_AUTO = 0x00;
    public static final int STATUS_GOOD = 0x01;
    public static final int STATUS_NORMAL = 0x02;
    public static final int STATUS_FULL = 0x03;
    public static final int STATUS_DOWN = 0x04;
    public static final int STATUS_GM_ONLY = 0x05;

    public static final int ON = 0x01;
    public static final int OFF = 0x00;

    public ServerStatus(byte[] decrypt, int serverId) {
        super(decrypt);

        GameServerInfo gsi = GameServerTable.getInstance().getGameServer(serverId);
        if (gsi != null) {
            int size = readD();
            for (int i = 0; i < size; i++) {
                int type = readD();
                int value = readD();
                switch (type) {
                    case SERVER_LIST_STATUS:
                        gsi.setStatus(value);
                        break;
                    case SERVER_LIST_CLOCK:
                        gsi.setShowingClock(value == ON);
                        break;
                    case SERVER_LIST_SQUARE_BRACKET:
                        gsi.setShowingBrackets(value == ON);
                        break;
                    case TEST_SERVER:
                        gsi.setTestServer(value == ON);
                        break;
                    case MAX_PLAYERS:
                        gsi.setMaxPlayers(value);
                        break;
                }
            }
        }
    }
}