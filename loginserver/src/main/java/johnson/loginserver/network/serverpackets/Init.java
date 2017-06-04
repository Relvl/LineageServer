package johnson.loginserver.network.serverpackets;

import johnson.loginserver.L2LoginClient;
import johnson.loginserver.network.ABaseLoginServerPacket;

/**
 * Первый пакет после инициализации соединения с клиентом.
 */
public final class Init extends ABaseLoginServerPacket {
    private final int sessionId;

    private final byte[] rsaPublicKey;
    private final byte[] blowfishKey;

    public Init(L2LoginClient client) {
        this.sessionId = client.getSessionId();
        this.rsaPublicKey = client.getScrambledModulus();
        this.blowfishKey = client.getBlowfishKey();
    }

    @Override
    protected void write() {
        writeC(0x00);

        writeD(sessionId);
        writeD(0x0000c621);

        writeB(rsaPublicKey);

        // Что это? Имеет отношение к ГГ?
        writeD(0x29DD954E);
        writeD(0x77C39CFC);
        writeD(0x97ADB620);
        writeD(0x07BDE0F7);

        writeB(blowfishKey);
        writeC(0x00); // NULL-termination
    }
}
