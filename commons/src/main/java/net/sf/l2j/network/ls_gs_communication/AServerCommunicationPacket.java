package net.sf.l2j.network.ls_gs_communication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Абстрактный класс, который призван решить проблему с неудобностью правок в парных пакетах.
 *
 * @author Johnson / 12.07.2017
 */
public abstract class AServerCommunicationPacket {
    protected static final Logger LOGGER = LoggerFactory.getLogger(AServerCommunicationPacket.class);

    private final boolean sendableMode;
    private final ByteArrayOutputStream outputStream;
    private final byte[] readBuffer;
    private int off;

    /** Конструктор для отправляемого пакета. */
    protected AServerCommunicationPacket() {
        this.sendableMode = true;
        this.outputStream = new ByteArrayOutputStream();
        this.readBuffer = null;
    }

    /** Конструктор для принимаемого пакета. */
    protected AServerCommunicationPacket(byte[] readBuffer) {
        this.sendableMode = false;
        this.outputStream = null;
        this.readBuffer = readBuffer;
        this.off = 1; // skip packet type id
    }

    /** Чтение тела пакета. */
    protected abstract void doRead();

    /** Запись тела пакета. */
    protected abstract void doWrite();

    public byte[] getSenableBuffer() {
        writeD(0x00); // reserve for checksum
        int padding = outputStream.size() % 8;
        if (padding != 0) {
            for (int i = padding; i < 8; i++) {
                writeC(0x00);
            }
        }
        return outputStream.toByteArray();
    }

    // region READ METHODS
    public int readD() {
        int result = readBuffer[off++] & 0xff;
        result |= readBuffer[off++] << 8 & 0xff00;
        result |= readBuffer[off++] << 0x10 & 0xff0000;
        result |= readBuffer[off++] << 0x18 & 0xff000000;
        return result;
    }

    public int readC() {
        return readBuffer[off++] & 0xff;
    }

    public int readH() {
        int result = readBuffer[off++] & 0xff;
        result |= readBuffer[off++] << 8 & 0xff00;
        return result;
    }

    public double readF() {
        long result = readBuffer[off++] & 0xff;
        result |= readBuffer[off++] << 8 & 0xff00;
        result |= readBuffer[off++] << 0x10 & 0xff0000;
        result |= readBuffer[off++] << 0x18 & 0xff000000;
        result |= readBuffer[off++] << 0x20 & 0xff00000000l;
        result |= readBuffer[off++] << 0x28 & 0xff0000000000l;
        result |= readBuffer[off++] << 0x30 & 0xff000000000000l;
        result |= readBuffer[off++] << 0x38 & 0xff00000000000000l;
        return Double.longBitsToDouble(result);
    }

    public String readS() {
        String result = null;
        try {
            result = new String(readBuffer, off, readBuffer.length - off, "UTF-16LE");
            result = result.substring(0, result.indexOf(0x00));
            off += result.length() * 2 + 2;
        } catch (RuntimeException | UnsupportedEncodingException e) {
            LOGGER.error("", e);
        }
        return result;
    }

    public byte[] readB(int length) {
        byte[] result = new byte[length];
        System.arraycopy(readBuffer, off, result, 0, length);
        off += length;
        return result;
    }
    // endregion READ METHODS

    // region WRITE METHODS
    protected void writeD(int value) {
        outputStream.write(value & 0xff);
        outputStream.write(value >> 8 & 0xff);
        outputStream.write(value >> 16 & 0xff);
        outputStream.write(value >> 24 & 0xff);
    }

    protected void writeC(int value) {
        outputStream.write(value & 0xff);
    }

    protected void writeH(int value) {
        outputStream.write(value & 0xff);
        outputStream.write(value >> 8 & 0xff);
    }

    protected void writeF(double org) {
        long value = Double.doubleToRawLongBits(org);
        outputStream.write((int) (value & 0xff));
        outputStream.write((int) (value >> 8 & 0xff));
        outputStream.write((int) (value >> 16 & 0xff));
        outputStream.write((int) (value >> 24 & 0xff));
        outputStream.write((int) (value >> 32 & 0xff));
        outputStream.write((int) (value >> 40 & 0xff));
        outputStream.write((int) (value >> 48 & 0xff));
        outputStream.write((int) (value >> 56 & 0xff));
    }

    protected void writeS(String text) {
        try {
            if (text != null) {
                outputStream.write(text.getBytes("UTF-16LE"));
            }
        } catch (RuntimeException | IOException e) {
            LOGGER.error("", e);
        }

        outputStream.write(0);
        outputStream.write(0);
    }

    protected void writeB(byte[] array) {
        try {
            outputStream.write(array);
        } catch (IOException e) {
            LOGGER.error("", e);
        }
    }
    // endregion WRITE METHODS
}
