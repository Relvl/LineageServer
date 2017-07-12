package net.sf.l2j.network.ls_gs_communication.impl.login_to_game;

import net.sf.l2j.network.ls_gs_communication.AServerCommunicationPacket;

/**
 * @author Johnson / 12.07.2017
 */
public class LoginServerAuthResponsePacket extends AServerCommunicationPacket {
    private int serverId;

    /** Конструктор для принимаемого пакета. */
    public LoginServerAuthResponsePacket(byte[] readBuffer) {
        super(readBuffer);
    }

    /** Конструктор для отправляемого пакета. */
    public LoginServerAuthResponsePacket(int serverId) {
        this.serverId = serverId;
    }

    /** Чтение тела пакета. */
    @Override
    protected void doRead() {
        serverId = readC();
    }

    /** Запись тела пакета. */
    @Override
    protected void doWrite() {
        writeC(0x02);
        writeC(serverId);
    }

    public int getServerId() {
        return serverId;
    }

    @Override
    public String toString() {
        return "LoginServerAuthResponsePacket{" +
                "serverId=" + serverId +
                '}';
    }
}
