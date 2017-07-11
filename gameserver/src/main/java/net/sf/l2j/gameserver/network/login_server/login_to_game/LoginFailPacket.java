package net.sf.l2j.gameserver.network.login_server.login_to_game;

import net.sf.l2j.commons.EGameServerLoginFailReason;
import net.sf.l2j.network.ABaseReceivablePacket;

public class LoginFailPacket extends ABaseReceivablePacket {
    private final EGameServerLoginFailReason reason;

    public LoginFailPacket(byte[] decrypt) {
        super(decrypt);
        reason = EGameServerLoginFailReason.getByCode(readC());
    }

    public EGameServerLoginFailReason getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "LoginFailPacket{" +
                "reason=" + reason +
                '}';
    }
}