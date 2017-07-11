package net.sf.l2j.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

public abstract class ABaseReceivablePacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(ABaseReceivablePacket.class);

    private final byte[] readBuffer;
    private int off;

    protected ABaseReceivablePacket(byte[] readBuffer) {
        this.readBuffer = readBuffer;
        off = 1; // skip packet type id
    }

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
}
