package johnson.loginserver.network.gameserverpackets;

import johnson.loginserver.network.AClientBasePacket;

import java.util.ArrayList;
import java.util.List;

public class PlayerInGame extends AClientBasePacket {
    private final List<String> accounts;

    public PlayerInGame(byte[] decrypt) {
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