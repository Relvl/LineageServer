package johnson.loginserver;

import johnson.loginserver.database.GetGameserversCall;
import net.sf.l2j.commons.database.CallException;
import net.sf.l2j.commons.random.Rnd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.HashMap;
import java.util.Map;

public class GameServerTable {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameServerTable.class);
    private static final int RSA_KEYS = 10;

    private final Map<Integer, GameServerInfo> gameServerTable = new HashMap<>();
    private final KeyPair[] keyPairs;

    protected GameServerTable() {
        try (GetGameserversCall call = new GetGameserversCall()) {
            call.execute();
            for (GameServerInfo serverInfo : call.getServers()) {
                gameServerTable.put(serverInfo.getId(), serverInfo);
            }
            LOGGER.info("Loaded {} gameservers: {}", call.getServers().size(), call.getServers());
        } catch (CallException e) {
            LOGGER.error("GameServerTable: Error loading registered game servers!", e);
        }

        keyPairs = new KeyPair[RSA_KEYS];
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(new RSAKeyGenParameterSpec(512, RSAKeyGenParameterSpec.F4));
            for (int i = 0; i < RSA_KEYS; i++)
                keyPairs[i] = keyGen.genKeyPair();
        } catch (Exception e) {
            LOGGER.error("GameServerTable: Error loading RSA keys for Game Server communication!", e);
        }
    }

    public Map<Integer, GameServerInfo> getRegisteredGameServers() { return gameServerTable; }

    public GameServerInfo getGameServer(int id) { return gameServerTable.get(id); }

    public KeyPair getRandomKeyPair() { return keyPairs[Rnd.get(10)]; }

    public static GameServerTable getInstance() { return SingletonHolder.instance; }

    private static class SingletonHolder {
        protected static final GameServerTable instance = new GameServerTable();
    }
}