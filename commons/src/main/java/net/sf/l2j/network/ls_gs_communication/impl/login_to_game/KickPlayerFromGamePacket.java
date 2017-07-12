package net.sf.l2j.network.ls_gs_communication.impl.login_to_game;

import net.sf.l2j.network.ls_gs_communication.AServerCommunicationPacket;

/**
 * Пакет, который позволяет ЛогинСерверу выкинуть персонажа из игры на ГеймСервере.
 * В частности во время повторного логина аккаунтом, который уже в игре.
 *
 * @author Johnson / 12.07.2017
 */
public class KickPlayerFromGamePacket extends AServerCommunicationPacket {
    private String login;

    /** Конструктор для принимаемого пакета. */
    public KickPlayerFromGamePacket(byte[] readBuffer) {
        super(readBuffer);
    }

    /** Конструктор для отправляемого пакета. */
    public KickPlayerFromGamePacket(String login) {
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
        writeC(0x04);
        writeS(login);
    }

    public String getLogin() {
        return login;
    }

    @Override
    public String toString() {
        return "KickPlayerFromGamePacket{" +
                "login='" + login + '\'' +
                '}';
    }
}
