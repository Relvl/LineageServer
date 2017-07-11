package net.sf.l2j.network.ls_gs_communication.impl.game_to_login;

import net.sf.l2j.commons.SessionKey;
import net.sf.l2j.network.ls_gs_communication.AServerCommunicationPacket;

/**
 * @author Johnson / 12.07.2017
 */
public class PlayerAuthRequestPacket extends AServerCommunicationPacket {
    private String login;
    private SessionKey sessionKey;

    /** Конструктор для отправляемого пакета. */
    public PlayerAuthRequestPacket(String login, SessionKey sessionKey) {
        this.login = login;
        this.sessionKey = sessionKey;
    }

    /** Чтение тела пакета. */
    @Override
    protected void doRead() {
        login = readS();
        int playKey1 = readD();
        int playKey2 = readD();
        int loginKey1 = readD();
        int loginKey2 = readD();
        sessionKey = new SessionKey(loginKey1, loginKey2, playKey1, playKey2);
    }

    /** Запись тела пакета. */
    @Override
    protected void doWrite() {
        writeC(0x05);
        writeS(login);
        writeD(sessionKey.playOkID1);
        writeD(sessionKey.playOkID2);
        writeD(sessionKey.loginOkID1);
        writeD(sessionKey.loginOkID2);
    }

    @Override
    public String toString() {
        return "PlayerAuthRequestPacket{" +
                "login='" + login + '\'' +
                ", sessionKey=" + sessionKey +
                '}';
    }
}
