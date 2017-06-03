package johnson.loginserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class GameServerListener extends FloodProtectedListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameServerListener.class);
    private static List<GameServerThread> _gameServers = new ArrayList<>();

    public GameServerListener() throws IOException {
        super(LoginServer.config.gameServerListener.host, LoginServer.config.gameServerListener.port);
    }

    @Override
    public void addClient(Socket s) {
        LOGGER.trace("Received gameserver connection from: {}", s.getInetAddress().getHostAddress());
        GameServerThread gst = new GameServerThread(s);
        _gameServers.add(gst);
    }

    public void removeGameServer(GameServerThread gst) {
        _gameServers.remove(gst);
    }
}
