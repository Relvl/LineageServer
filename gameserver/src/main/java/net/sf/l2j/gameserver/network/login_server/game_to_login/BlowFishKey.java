package net.sf.l2j.gameserver.network.login_server.game_to_login;

import net.sf.l2j.network.ABaseSendablePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;

public class BlowFishKey extends ABaseSendablePacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlowFishKey.class);

    public BlowFishKey(byte[] key, RSAPublicKey publicKey) {
        writeC(0x00);
        try {
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
            rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedKey = rsaCipher.doFinal(key);

            writeD(encryptedKey.length);
            writeB(encryptedKey);
        } catch (GeneralSecurityException e) {
            LOGGER.error("Error While encrypting blowfish key for transmision (Crypt error)", e);
        }
    }

    @Override
    public byte[] getContent() {
        return getBytes();
    }
}