package net.sf.l2j.network.ls_gs_communication.impl.login_to_game;

import net.sf.l2j.network.ls_gs_communication.AServerCommunicationPacket;

import java.util.Arrays;

/**
 * Первый пакет, который ЛогинСервер отправляет ГемСерверу после установления подключения.
 * Сравнивает ревизии серверов, и инициализирует ключи BlowFish ширования.
 *
 * @author Johnson / 12.07.2017
 */
public class InitGameServerPacket extends AServerCommunicationPacket {
    private int revision;
    private byte[] publicKey;

    /** Конструктор для принимаемого пакета. */
    public InitGameServerPacket(byte[] readBuffer) {
        super(readBuffer);
    }

    /** Конструктор для отправляемого пакета. */
    public InitGameServerPacket(int revision, byte[] publicKey) {
        this.revision = revision;
        this.publicKey = publicKey;
    }

    @Override
    protected void doRead() {
        revision = readD();
        int size = readD();
        publicKey = readB(size);
    }

    @Override
    protected void doWrite() {
        writeC(0x00);
        writeD(revision);
        writeD(publicKey.length);
        writeB(publicKey);
    }

    public int getRevision() {
        return revision;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    @Override
    public String toString() {
        return "InitGameServerPacket{" +
                "revision=" + revision +
                ", publicKey=" + Arrays.toString(publicKey) +
                '}';
    }
}
