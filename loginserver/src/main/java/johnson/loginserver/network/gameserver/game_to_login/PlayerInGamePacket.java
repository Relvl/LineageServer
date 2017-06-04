package johnson.loginserver.network.gameserver.game_to_login;

import johnson.loginserver.network.gameserver.ABaseClientPacket;

import java.util.ArrayList;
import java.util.List;

public class PlayerInGamePacket extends ABaseClientPacket {
    private final List<String> accounts;

    public PlayerInGamePacket(byte[] decrypt) {
        super(decrypt);
        accounts = new ArrayList<>();
        int size = readH();
        for (int i = 0; i < size; i++) {
            accounts.add(readS());
        }
    }

    public List<String> getAccounts() {
        return accounts;
    }
}