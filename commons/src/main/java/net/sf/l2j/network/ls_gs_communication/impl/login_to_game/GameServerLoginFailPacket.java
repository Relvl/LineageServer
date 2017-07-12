package net.sf.l2j.network.ls_gs_communication.impl.login_to_game;

import net.sf.l2j.commons.EGameServerLoginFailReason;
import net.sf.l2j.network.ls_gs_communication.AServerCommunicationPacket;

/**
 * @author Johnson / 12.07.2017
 */
public class GameServerLoginFailPacket extends AServerCommunicationPacket {
    private EGameServerLoginFailReason reason;

    /** Конструктор для принимаемого пакета. */
    public GameServerLoginFailPacket(byte[] readBuffer) {
        super(readBuffer);
    }

    /** Конструктор для отправляемого пакета. */
    public GameServerLoginFailPacket(EGameServerLoginFailReason reason) {
        this.reason = reason;
    }

    /** Чтение тела пакета. */
    @Override
    protected void doRead() {
        reason = EGameServerLoginFailReason.getByCode(readC());
    }

    /** Запись тела пакета. */
    @Override
    protected void doWrite() {
        writeC(0x01);
        writeC(reason.getCode());
    }

    public EGameServerLoginFailReason getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "GameServerLoginFailPacket{" +
                "reason=" + reason +
                '}';
    }
}
