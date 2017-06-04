package johnson.loginserver.network.gameserverpackets;

import johnson.loginserver.SessionKey;
import johnson.loginserver.network.AClientBasePacket;

public class PlayerAuthRequest extends AClientBasePacket {

    private final String login;
    private final SessionKey sessionKey;

    public PlayerAuthRequest(byte[] decrypt) {
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