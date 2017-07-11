package johnson.loginserver.network.gameserver.game_to_login;

import net.sf.l2j.network.ABaseReceivablePacket;

import java.util.ArrayList;
import java.util.List;

public class PlayerInGamePacket extends ABaseReceivablePacket {
    private final List<String> logins;

    public PlayerInGamePacket(byte[] decrypt) {
        super(decrypt);
        logins = new ArrayList<>();
        int size = readH();
        for (int i = 0; i < size; i++) {
            logins.add(readS());
        }
    }

    public List<String> getLogins() {
        return logins;
    }
}