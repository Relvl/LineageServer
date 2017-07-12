package net.sf.l2j.network.ls_gs_communication.impl.game_to_login;

import net.sf.l2j.network.ls_gs_communication.AServerCommunicationPacket;

import java.util.Arrays;

/**
 * @author Johnson / 12.07.2017
 */
public class GameServerAuthRequestPacket extends AServerCommunicationPacket {
    private int desiredId;
    private boolean hostReserved;
    private String externalHost;
    private String internalHost;
    private int port;
    private int maxPlayers;
    private byte[] hexId;

    /** Конструктор для принимаемого пакета. */
    public GameServerAuthRequestPacket(byte[] readBuffer) {
        super(readBuffer);
    }

    /** Конструктор для отправляемого пакета. */
    public GameServerAuthRequestPacket() {
    }

    /** Чтение тела пакета. */
    @Override
    protected void doRead() {
        desiredId = readC();
        hostReserved = readC() != 0;
        externalHost = readS();
        internalHost = readS();
        port = readH();
        maxPlayers = readD();
        int size = readD();
        hexId = readB(size);
    }

    /** Запись тела пакета. */
    @Override
    protected void doWrite() {
        writeC(0x01);
        writeC(desiredId);
        writeC(hostReserved ? 0x01 : 0x00);
        writeS(externalHost);
        writeS(internalHost);
        writeH(port);
        writeD(maxPlayers);
        writeD(hexId.length);
        writeB(hexId);
    }

    public int getDesiredId() {
        return desiredId;
    }

    public void setDesiredId(int desiredId) {
        this.desiredId = desiredId;
    }

    public boolean isHostReserved() {
        return hostReserved;
    }

    public void setHostReserved(boolean hostReserved) {
        this.hostReserved = hostReserved;
    }

    public String getExternalHost() {
        return externalHost;
    }

    public void setExternalHost(String externalHost) {
        this.externalHost = externalHost;
    }

    public String getInternalHost() {
        return internalHost;
    }

    public void setInternalHost(String internalHost) {
        this.internalHost = internalHost;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public byte[] getHexId() {
        return hexId;
    }

    public void setHexId(byte[] hexId) {
        this.hexId = hexId;
    }

    @Override
    public String toString() {
        return "GameServerAuthRequestPacket{" +
                "desiredId=" + desiredId +
                ", hostReserved=" + hostReserved +
                ", externalHost='" + externalHost + '\'' +
                ", internalHost='" + internalHost + '\'' +
                ", port=" + port +
                ", maxPlayers=" + maxPlayers +
                ", hexId=" + Arrays.toString(hexId) +
                '}';
    }
}
