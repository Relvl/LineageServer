package johnson.loginserver.network.gameserver.game_to_login;

import net.sf.l2j.network.ABaseReceivablePacket;

public class PlayerLogoutPacket extends ABaseReceivablePacket {

    private final String login;

    public PlayerLogoutPacket(byte[] decrypt) {
        super(decrypt);
        login = readS();
    }

    public String getAccount() {
        return login;
    }

}