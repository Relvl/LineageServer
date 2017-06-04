package johnson.loginserver.network.gameserver.game_to_login;

import johnson.loginserver.network.gameserver.ABaseClientPacket;

public class ChangeAccessLevel extends ABaseClientPacket {

    private final int level;
    private final String login;

    public ChangeAccessLevel(byte[] decrypt) {
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