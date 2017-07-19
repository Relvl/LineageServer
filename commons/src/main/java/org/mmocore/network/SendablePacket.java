package org.mmocore.network;

import net.sf.l2j.commons.ICodeProvider;

public abstract class SendablePacket<T extends MMOClient<?>> extends AbstractPacket<T> {
    protected final void writeC(int data) {
        buffer.put((byte) data);
    }

    protected final void writeF(double value) {
        buffer.putDouble(value);
    }

    protected final void writeH(int value) {
        buffer.putShort((short) value);
    }

    protected final void writeH(boolean value) {
        buffer.putShort((short) (value ? 0x01 : 0x00));
    }

    protected final void writeH(ICodeProvider value) {
        buffer.putShort((short) value.getCode());
    }

    protected final void writeD(int value) {
        buffer.putInt(value);
    }

    protected final void writeD(boolean value) {
        buffer.putInt(value ? 0x01 : 0x00);
    }

    protected final void writeD(ICodeProvider value) {
        buffer.putInt(value.getCode());
    }

    protected final void writeQ(long value) {
        buffer.putLong(value);
    }

    protected final void writeB(byte[] data) {
        buffer.put(data);
    }

    protected final void writeS(String text) {
        if (text != null) {
            int len = text.length();
            for (int i = 0; i < len; i++) {
                buffer.putChar(text.charAt(i));
            }
        }

        buffer.putChar('\000');
    }

    protected abstract void write();
}
