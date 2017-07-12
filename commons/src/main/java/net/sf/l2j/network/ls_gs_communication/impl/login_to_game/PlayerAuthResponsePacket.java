package net.sf.l2j.network.ls_gs_communication.impl.login_to_game;

import net.sf.l2j.network.ls_gs_communication.AServerCommunicationPacket;

/**
 * @author Johnson / 12.07.2017
 */
public class PlayerAuthResponsePacket extends AServerCommunicationPacket {
    private String login;
    private boolean authed;

    /** Конструктор для принимаемого пакета. */
    public PlayerAuthResponsePacket(byte[] readBuffer) {
        super(readBuffer);
    }

    /** Конструктор для отправляемого пакета. */
    public PlayerAuthResponsePacket(String login, boolean authed) {
        this.login = login;
        this.authed = authed;
    }

    /** Чтение тела пакета. */
    @Override
    protected void doRead() {
        login = readS();
        authed = readC() != 0;
    }

    /** Запись тела пакета. */
    @Override
    protected void doWrite() {
        writeC(0x03);
        writeS(login);
        writeC(authed ? 1 : 0);
    }

    public String getLogin() {
        return login;
    }

    public boolean isAuthed() {
        return authed;
    }

    @Override
    public String toString() {
        return "PlayerAuthResponsePacket{" +
                "login='" + login + '\'' +
                ", authed=" + authed +
                '}';
    }
}
