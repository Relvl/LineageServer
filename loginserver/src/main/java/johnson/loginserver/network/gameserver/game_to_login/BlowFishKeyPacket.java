package johnson.loginserver.network.gameserver.game_to_login;

import net.sf.l2j.network.ABaseReceivablePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;

public class BlowFishKeyPacket extends ABaseReceivablePacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlowFishKeyPacket.class);
    byte[] key;

    public BlowFishKeyPacket(byte[] decrypt, RSAPrivateKey privateKey) {
        super(decrypt);

        int size = readD();
        byte[] encryptedKey = readB(size);


        try {
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
            rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] tempDecryptKey = rsaCipher.doFinal(encryptedKey);
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