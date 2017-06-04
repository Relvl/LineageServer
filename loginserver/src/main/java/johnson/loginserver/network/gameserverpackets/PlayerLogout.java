package johnson.loginserver.network.gameserverpackets;

import johnson.loginserver.network.AClientBasePacket;

public class PlayerLogout extends AClientBasePacket {

    private final String login;

    public PlayerLogout(byte[] decrypt) {
        super(decrypt);
        login = readS();
    }

    public String getAccount() {
        return login;
    }

}