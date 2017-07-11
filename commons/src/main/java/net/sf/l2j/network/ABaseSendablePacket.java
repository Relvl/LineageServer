package net.sf.l2j.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class ABaseSendablePacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(ABaseSendablePacket.class);

    private final ByteArrayOutputStream outputStream;

    protected ABaseSendablePacket() {
        outputStream = new ByteArrayOutputStream();
    }

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

    public byte[] getBytes() {
        writeD(0x00); // reserve for checksum
        int padding = outputStream.size() % 8;
        if (padding != 0) {
            for (int i = padding; i < 8; i++) {
                writeC(0x00);
            }
        }
        return outputStream.toByteArray();
    }

    public abstract byte[] getContent() throws IOException;
}