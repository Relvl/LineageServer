package johnson.loginserver.network.clientpackets;

import johnson.loginserver.LoginController;
import johnson.loginserver.network.ABaseLoginClientPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;

public class RequestAuthLogin extends ABaseLoginClientPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestAuthLogin.class);

    private final byte[] raw = new byte[128];

    @Override
    public boolean readImpl() {
        if (super._buf.remaining() >= 128) {
            readB(raw);
            return true;
        }
        return false;
    }

    @Override
    public void run() {
        try {
            final Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
            rsaCipher.init(Cipher.DECRYPT_MODE, getClient().getRSAPrivateKey());
            byte[] decrypted = rsaCipher.doFinal(raw, 0x00, 0x80);

            String login = new String(decrypted, 0x5E, 14).trim().toLowerCase();
            String password = new String(decrypted, 0x6C, 16).trim();
            LoginController.getInstance().onAuthLogin(login, password, getClient());
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }
}
