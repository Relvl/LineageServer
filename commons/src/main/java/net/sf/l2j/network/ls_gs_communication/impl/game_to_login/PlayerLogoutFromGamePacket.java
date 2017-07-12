package net.sf.l2j.network.ls_gs_communication.impl.game_to_login;

import net.sf.l2j.network.ls_gs_communication.AServerCommunicationPacket;

/**
 * @author Johnson / 12.07.2017
 */
public class PlayerLogoutFromGamePacket extends AServerCommunicationPacket {
    private String login;

    /** Конструктор для принимаемого пакета. */
    public PlayerLogoutFromGamePacket(byte[] readBuffer) {
        super(readBuffer);
    }

    /** Конструктор для отправляемого пакета. */
    public PlayerLogoutFromGamePacket(String login) {
        this.login = login;
    }

    /** Чтение тела пакета. */
    @Override
    protected void doRead() {
        login = readS();
    }

    /** Запись тела пакета. */
    @Override
    protected void doWrite() {
        writeC(0x03);
        writeS(login);
    }

    public String getLogin() {
        return login;
    }

    @Override
    public String toString() {
        return "PlayerLogoutFromGamePacket{" +
                "login='" + login + '\'' +
                '}';
    }
}
