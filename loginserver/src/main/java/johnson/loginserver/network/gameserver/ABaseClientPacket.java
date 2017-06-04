package johnson.loginserver.network.gameserver;

public abstract class ABaseClientPacket {
    private final byte[] decrypt;
    private int off;

    // FIXME Чё за говно? Где БайтБуффер?

    public ABaseClientPacket(byte[] decrypt) {
        this.decrypt = decrypt;
        off = 1; // skip packet type id
    }

    public int readD() {
        int result = decrypt[off++] & 0xff;
        result |= decrypt[off++] << 8 & 0xff00;
        result |= decrypt[off++] << 0x10 & 0xff0000;
        result |= decrypt[off++] << 0x18 & 0xff000000;
        return result;
    }

    public int readC() {
        return decrypt[off++] & 0xff;
    }

    public int readH() {
        int result = decrypt[off++] & 0xff;
        result |= decrypt[off++] << 8 & 0xff00;
        return result;
    }

    public String readS() {
        String result = null;
        try {
            result = new String(decrypt, off, decrypt.length - off, "UTF-16LE");
            result = result.substring(0, result.indexOf(0x00));
            off += result.length() * 2 + 2;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public final byte[] readB(int length) {
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = decrypt[off + i];
        }
        off += length;
        return result;
    }
}
