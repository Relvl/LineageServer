package net.sf.l2j.gameserver.network.login_server.login_to_game;

import net.sf.l2j.network.ABaseReceivablePacket;

public class InitLSPacket extends ABaseReceivablePacket {
    private final int _rev;
    private final byte[] _key;

    public InitLSPacket(byte[] decrypt) {
        super(decrypt);
        _rev = readD();
        int size = readD();
        _key = readB(size);
    }

    public int getRevision() {
        return _rev;
    }

    public byte[] getRSAKey() {
        return _key;
    }

}