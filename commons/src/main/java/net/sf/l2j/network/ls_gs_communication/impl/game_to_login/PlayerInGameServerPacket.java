package net.sf.l2j.network.ls_gs_communication.impl.game_to_login;

import net.sf.l2j.network.ls_gs_communication.AServerCommunicationPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Johnson / 12.07.2017
 */
public class PlayerInGameServerPacket extends AServerCommunicationPacket {
    private List<String> logins;

    /** Конструктор для принимаемого пакета. */
    public PlayerInGameServerPacket(byte[] readBuffer) {
        super(readBuffer);
    }

    /** Конструктор для отправляемого пакета. */
    public PlayerInGameServerPacket(List<String> logins) {
        this.logins = logins;
    }

    /** Конструктор для отправляемого пакета. */
    public PlayerInGameServerPacket(String login) {
        this.logins = Collections.singletonList(login);
    }

    /** Чтение тела пакета. */
    @Override
    protected void doRead() {
        logins = new ArrayList<>();
        int size = readH();
        for (int i = 0; i < size; i++) {
            logins.add(readS());
        }
    }

    /** Запись тела пакета. */
    @Override
    protected void doWrite() {
        writeC(0x02);
        writeH(logins.size());
        for (String pc : logins) {
            writeS(pc);
        }
    }

    public List<String> getLogins() {
        return logins;
    }

    @Override
    public String toString() {
        return "PlayerInGameServerPacket{" +
                "logins=" + logins +
                '}';
    }
}
