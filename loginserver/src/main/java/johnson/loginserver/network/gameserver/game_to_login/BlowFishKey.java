package johnson.loginserver.network.gameserver.game_to_login;

import johnson.loginserver.network.gameserver.ABaseClientPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;

public class BlowFishKey extends ABaseClientPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlowFishKey.class);
    byte[] key;

    public BlowFishKey(byte[] decrypt, RSAPrivateKey privateKey) {
        super(decrypt);
        int size = readD();
        byte[] tempKey = readB(size);
        try {
            byte[] tempDecryptKey;
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
            rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
            tempDecryptKey = rsaCipher.doFinal(tempKey);
            // there are nulls before the key we must remove them
            int i = 0;
            int len = tempDecryptKey.length;
            for (; i < len; i++) {
                if (tempDecryptKey[i] != 0) { break; }
            }
            key = new byte[len - i];
            System.arraycopy(tempDecryptKey, i, key, 0, len - i);
        } catch (GeneralSecurityException e) {
            LOGGER.error("Error While decrypting blowfish key (RSA)", e);
        }
    }

    public byte[] getKey() {
        return key;
    }
}