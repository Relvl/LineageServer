package johnson.loginserver.network.client.client_to_login;

import johnson.loginserver.L2LoginClient;
import johnson.loginserver.LoginController;
import org.mmocore.network.ReceivablePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public class RequestAuth extends ReceivablePacket<L2LoginClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestAuth.class);

    private final byte[] raw = new byte[128];

    @Override
    public boolean read() {
        if (buffer.remaining() >= 128) {
            readB(raw);
            return true;
        }
        return false;
    }

    @Override
    @SuppressWarnings("MagicNumber")
    public void run() {
        try {
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
            rsaCipher.init(Cipher.DECRYPT_MODE, getClient().getRSAPrivateKey());
            byte[] decrypted = rsaCipher.doFinal(raw, 0x00, 0x80);

            String login = new String(decrypted, 0x5E, 14).trim().toLowerCase();
            String password = new String(decrypted, 0x6C, 16).trim();

            LoginController.getInstance().onAuthLogin(login, password, getClient());

        } catch (GeneralSecurityException e) {
            LOGGER.error("", e);
        }
    }

    @Override
    public String toString() {
        return "RequestAuth{" +
                "raw=" + Arrays.toString(raw) +
                '}';
    }
}
