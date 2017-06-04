package johnson.loginserver.network.gameserverpackets;

import johnson.loginserver.network.AClientBasePacket;

public class GameServerAuth extends AClientBasePacket {
    private final byte[] hexId;
    private final int desiredId;
    private final boolean hostReserved;
    private final boolean acceptAlternativeId;
    private final int maxPlayers;
    private final int port;
    private final String externalHost;
    private final String internalHost;

    public GameServerAuth(byte[] decrypt) {
        super(decrypt);

        desiredId = readC();
        acceptAlternativeId = (readC() != 0);
        hostReserved = (readC() != 0);
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

    public boolean getHostReserved() {
        return hostReserved;
    }

    public int getDesiredID() {
        return desiredId;
    }

    public boolean acceptAlternateID() {
        return acceptAlternativeId;
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
