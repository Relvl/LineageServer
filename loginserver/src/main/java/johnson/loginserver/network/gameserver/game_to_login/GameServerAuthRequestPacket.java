package johnson.loginserver.network.gameserver.game_to_login;

import johnson.loginserver.network.gameserver.ABaseClientPacket;

public class GameServerAuthRequestPacket extends ABaseClientPacket {
    private final byte[] hexId;
    private final int desiredId;
    private final boolean hostReserved;
    private final int maxPlayers;
    private final int port;
    private final String externalHost;
    private final String internalHost;

    public GameServerAuthRequestPacket(byte[] decrypt) {
        super(decrypt);

        desiredId = readC();
        hostReserved = readC() != 0;
        externalHost = readS();
        internalHost = readS();
        port = readH();
        maxPlayers = readD();
        int size = readD();
        hexId = readB(size);
    }

    public byte[] getHexID() {
        return hexId;
    }

    public int getDesiredID() {
        return desiredId;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public String getExternalHost() {
        return externalHost;
    }

    public String getInternalHost() {
        return internalHost;
    }

    public int getPort() {
        return port;
    }
}
