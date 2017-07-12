package org.mmocore.network;

import java.nio.ByteBuffer;

public abstract class ReceivablePacket<T extends MMOClient<?>> extends AbstractPacket<T> implements Runnable {
    NioNetStringBuffer _sbuf;

    protected ReceivablePacket() {
    }

    protected abstract boolean read();

    public abstract void run();

    protected final void readB(final byte[] dst) {
        buffer.get(dst);
    }

    protected final void readB(final byte[] dst, final int offset, final int len) {
        buffer.get(dst, offset, len);
    }

    protected final int readC() {
        return buffer.get() & 0xFF;
    }

    protected final int readH() {
        return buffer.getShort() & 0xFFFF;
    }

    protected final int readD() {
        return buffer.getInt();
    }

    protected final long readQ() {
        return buffer.getLong();
    }

    protected final double readF() {
        return buffer.getDouble();
    }

    protected final String readS() {
        _sbuf.clear();

        char ch;
        while ((ch = buffer.getChar()) != 0) {
            _sbuf.append(ch);
        }

        return _sbuf.toString();
    }

    public void setBuffers(ByteBuffer data, T client, NioNetStringBuffer sBuffer) {
        buffer = data;
        this.client = client;
        _sbuf = sBuffer;
    }
}
