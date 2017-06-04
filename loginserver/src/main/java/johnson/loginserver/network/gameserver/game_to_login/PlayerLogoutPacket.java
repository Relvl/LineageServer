package johnson.loginserver.network.gameserver.game_to_login;

import johnson.loginserver.network.gameserver.ABaseClientPacket;

public class PlayerLogoutPacket extends ABaseClientPacket {

    private final String login;

    public PlayerLogoutPacket(byte[] decrypt) {
        super(decrypt);
        login = readS();
    }

    public String getAccount() {
        return login;
    }

}