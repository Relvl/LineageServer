package johnson.loginserver.network.gameserver.game_to_login;

import johnson.loginserver.network.gameserver.ABaseClientPacket;

public class PlayerLogout extends ABaseClientPacket {

    private final String login;

    public PlayerLogout(byte[] decrypt) {
        super(decrypt);
        login = readS();
    }

    public String getAccount() {
        return login;
    }

}