package org.mmocore.network;

public abstract class SendablePacket<T extends MMOClient<?>> extends AbstractPacket<T> {
    protected final void writeC(final int data) {
        buffer.put((byte) data);
    }

    protected final void writeF(final double value) {
        buffer.putDouble(value);
    }

    protected final void writeH(final int value) {
        buffer.putShort((short) value);
    }

    protected final void writeD(final int value) {
        buffer.putInt(value);
    }

    protected final void writeQ(final long value) {
        buffer.putLong(value);
    }

    protected final void writeB(final byte[] data) {
        buffer.put(data);
    }

    protected final void writeS(final String text) {
        if (text != null) {
            final int len = text.length();
            for (int i = 0; i < len; i++) {
                buffer.putChar(text.charAt(i));
            }
        }

        buffer.putChar('\000');
    }

    protected abstract void write();
}
