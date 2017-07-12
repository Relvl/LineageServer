package net.sf.l2j.network.ls_gs_communication;

import net.sf.l2j.NewCrypt;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.interfaces.RSAPublicKey;

/**
 * @author Johnson / 11.07.2017
 */
public abstract class AServerCommunicationThread extends Thread {
    protected static final String BLOWFISH_KEY_BASE = "_;v.]05-31!|+-%xT!^[$\00";

    private final Object streamLock = new Object();

    protected InputStream inputStream;
    protected OutputStream outputStream;
    protected NewCrypt blowfishCrypt;
    protected RSAPublicKey publicKey;

    protected AServerCommunicationThread(String name) {
        super(name);
    }

    protected void sendPacket(AServerCommunicationPacket serverPacket) {
        try {
            byte[] data = serverPacket.getSendableBuffer();
            NewCrypt.appendChecksum(data);
            data = blowfishCrypt.crypt(data);

            int len = data.length + 2;
            synchronized (streamLock) {
                outputStream.write(len & 0xff);
                outputStream.write(len >> 8 & 0xff);
                outputStream.write(data);
                outputStream.flush();
            }
        } catch (IOException e) {
            getLogger().error("IOException while sending packet {}", serverPacket.getClass().getSimpleName(), e);
        }
    }

    protected void doThreadLoop(Socket socket) throws IOException {
        while (!isInterrupted()) {
            int lengthLo = inputStream.read();
            int lengthHi = inputStream.read();
            int length = lengthHi * 256 + lengthLo;

            if (lengthHi < 0 || socket.isClosed()) {
                getLogger().info("GameServerThread: GameServer terminated the connection.");
                break;
            }

            byte[] incoming = new byte[length - 2];

            int receivedBytes = 0;
            int newBytes = 0;

            while (newBytes != -1 && receivedBytes < length - 2) {
                newBytes = inputStream.read(incoming, 0, length - 2);
                receivedBytes += newBytes;
            }

            if (receivedBytes != length - 2) {
                getLogger().warn("Incomplete packet is sent to the server, closing connection.");
                break;
            }

            // decrypt if we have a key
            incoming = blowfishCrypt.decrypt(incoming);
            if (!NewCrypt.verifyChecksum(incoming)) {
                getLogger().warn("Incorrect packet checksum, closing connection.");
                return;
            }

            int packetType = incoming[0] & 0xff;

            doProcessIncomindData(packetType, incoming);
        }
    }

    protected abstract void doProcessIncomindData(int packetType, byte[] incoming);

    protected abstract Logger getLogger();
}
