package johnson.loginserver;

import johnson.loginserver.database.GetGameserversCall;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.commons.database.CallException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameServerTable {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameServerTable.class);

    // RSA Config
    private static final int KEYS_SIZE = 10;
    // Game Server Table
    private final Map<Integer, GameServerInfo> gameServerTable = new ConcurrentHashMap<>();
    private KeyPair[] _keyPairs;

    protected GameServerTable() {
        loadRegisteredGameServers();
        initRSAKeys();
        LOGGER.info("Cached {} RSA keys for gameserver communication.", _keyPairs.length);
    }

    private static byte[] stringToHex(String string) {
        return new BigInteger(string, 16).toByteArray();
    }

    public static GameServerTable getInstance() {
        return SingletonHolder.instance;
    }

    private void initRSAKeys() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(new RSAKeyGenParameterSpec(512, RSAKeyGenParameterSpec.F4));

            _keyPairs = new KeyPair[KEYS_SIZE];
            for (int i = 0; i < KEYS_SIZE; i++)
                _keyPairs[i] = keyGen.genKeyPair();
        } catch (Exception e) {
            LOGGER.error("GameServerTable: Error loading RSA keys for Game Server communication!", e);
        }
    }

    private void loadRegisteredGameServers() {
        try (GetGameserversCall call = new GetGameserversCall()) {
            call.execute();
            for (GameServerInfo serverInfo : call.getServers()) {
                gameServerTable.put(serverInfo.getId(), serverInfo);
            }
            LOGGER.info("Loaded {} gameservers: {}", call.getServers().size(), call.getServers());
        } catch (CallException e) {
            LOGGER.error("GameServerTable: Error loading registered game servers!", e);
        }
    }

    public Map<Integer, GameServerInfo> getRegisteredGameServers() {
        return gameServerTable;
    }

    public GameServerInfo getRegisteredGameServerById(int id) {
        return gameServerTable.get(id);
    }

    public KeyPair getKeyPair() {
        return _keyPairs[Rnd.get(10)];
    }

    private static class SingletonHolder {
        protected static final GameServerTable instance = new GameServerTable();
    }
}