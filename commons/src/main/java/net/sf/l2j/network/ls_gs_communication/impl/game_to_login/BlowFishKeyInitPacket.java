package net.sf.l2j.network.ls_gs_communication.impl.game_to_login;

import net.sf.l2j.network.ls_gs_communication.AServerCommunicationPacket;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;

/**
 * @author Johnson / 12.07.2017
 */
public class BlowFishKeyInitPacket extends AServerCommunicationPacket {
    private static Cipher RSA_CIPHER;

    static {
        try {
            RSA_CIPHER = Cipher.getInstance("RSA/ECB/nopadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException ignored) {
            RSA_CIPHER = null;
        }
    }

    private RSAPrivateKey privateKey;
    private byte[] encryptedKey;
    private byte[] key;

    /** Конструктор для отправляемого пакета. */
    public BlowFishKeyInitPacket(byte[] key, @SuppressWarnings("TypeMayBeWeakened") RSAPublicKey publicKey) {
        try {
            RSA_CIPHER.init(Cipher.ENCRYPT_MODE, publicKey);
            encryptedKey = RSA_CIPHER.doFinal(key);
        } catch (GeneralSecurityException e) {
            LOGGER.error("Error while encrypting blowfish key for transmision (Crypt error)", e);
        }
    }

    /**
     * Конструктор для принимаемого пакета.
     */
    public BlowFishKeyInitPacket(byte[] readBuffer, RSAPrivateKey privateKey) {
        super(readBuffer);
        this.privateKey = privateKey;
    }

    /** Чтение тела пакета. */
    @Override
    protected void doRead() {
        int size = readD();
        encryptedKey = readB(size);
    }

    /** Запись тела пакета. */
    @Override
    protected void doWrite() {
        writeC(0x00);
        writeD(encryptedKey.length);
        writeB(encryptedKey);
    }

    public byte[] getBlowFishKey() {
        if (key == null) {
            try {
                RSA_CIPHER.init(Cipher.DECRYPT_MODE, privateKey);
                byte[] tempDecryptKey = RSA_CIPHER.doFinal(encryptedKey);
                // there are nulls before the key we must remove them
                int i = 0;
                int len = tempDecryptKey.length;
                for (; i < len; i++) {
                    if (tempDecryptKey[i] != 0) { break; }
                }
                key = new byte[len - i];
                System.arraycopy(tempDecryptKey, i, key, 0, len - i);
            } catch (GeneralSecurityException e) {
                LOGGER.error("Error while decrypting blowfish key (RSA)", e);
            }
        }
        return key;
    }

    @Override
    public String toString() {
        return "BlowFishKeyInitPacket{" +
                "privateKey=" + privateKey +
                ", encryptedKey=" + Arrays.toString(encryptedKey) +
                ", key=" + Arrays.toString(key) +
                '}';
    }
}
