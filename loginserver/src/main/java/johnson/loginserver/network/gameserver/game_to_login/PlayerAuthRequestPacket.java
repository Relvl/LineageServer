package johnson.loginserver.network.gameserver.game_to_login;

import net.sf.l2j.commons.SessionKey;
import net.sf.l2j.network.ABaseReceivablePacket;

public class PlayerAuthRequestPacket extends ABaseReceivablePacket {

    private final String login;
    private final SessionKey sessionKey;

    public PlayerAuthRequestPacket(byte[] decrypt) {
        super(decrypt);
        login = readS();
        int playKey1 = readD();
        int playKey2 = readD();
        int loginKey1 = readD();
        int loginKey2 = readD();
        sessionKey = new SessionKey(loginKey1, loginKey2, playKey1, playKey2);
    }

    public String getAccount() {
        return login;
    }

    public SessionKey getKey() {
        return sessionKey;
    }

}