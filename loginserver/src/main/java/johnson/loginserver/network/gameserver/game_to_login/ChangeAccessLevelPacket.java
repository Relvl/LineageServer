package johnson.loginserver.network.gameserver.game_to_login;

import net.sf.l2j.network.ABaseReceivablePacket;

public class ChangeAccessLevelPacket extends ABaseReceivablePacket {

    private final int level;
    private final String login;

    public ChangeAccessLevelPacket(byte[] decrypt) {
        super(decrypt);
        level = readD();
        login = readS();
    }

    public String getAccount() {
        return login;
    }

    public int getLevel() {
        return level;
    }

}