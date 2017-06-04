package johnson.loginserver.network.gameserverpackets;

import johnson.loginserver.network.AClientBasePacket;

public class ChangeAccessLevel extends AClientBasePacket {

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