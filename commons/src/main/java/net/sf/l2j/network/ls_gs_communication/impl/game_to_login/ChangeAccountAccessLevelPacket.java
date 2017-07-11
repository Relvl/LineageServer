package net.sf.l2j.network.ls_gs_communication.impl.game_to_login;

import net.sf.l2j.network.ls_gs_communication.AServerCommunicationPacket;

/**
 * @author Johnson / 12.07.2017
 */
public class ChangeAccountAccessLevelPacket extends AServerCommunicationPacket {
    private String login;
    private int level;

    /** Конструктор для отправляемого пакета. */
    public ChangeAccountAccessLevelPacket(String login, int level) {
        this.login = login;
        this.level = level;
    }

    /** Чтение тела пакета. */
    @Override
    protected void doRead() {
        level = readD();
        login = readS();
    }

    /** Запись тела пакета. */
    @Override
    protected void doWrite() {
        writeC(0x04);
        writeD(level);
        writeS(login);
    }

    @Override
    public String toString() {
        return "ChangeAccountAccessLevelPacket{" +
                "login='" + login + '\'' +
                ", level=" + level +
                '}';
    }
}
