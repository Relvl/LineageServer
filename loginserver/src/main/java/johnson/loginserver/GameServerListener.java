package johnson.loginserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;

public class GameServerListener extends FloodProtectedListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameServerListener.class);
    private static final Collection<GameServerThread> GAME_SERVER_THREADS = new ArrayList<>();

    public GameServerListener() throws IOException {
        super(LoginServer.CONFIG.network.communicationHost, LoginServer.CONFIG.network.communicationPort);
    }

    public static void removeGameServer(GameServerThread gst) {
        GAME_SERVER_THREADS.remove(gst);
    }

    @Override
    public void addClient(Socket socket) {
        LOGGER.info("Received gameserver connection from: {}", socket.getInetAddress().getHostAddress());
        GameServerThread gst = new GameServerThread(socket);
        gst.start();
        GAME_SERVER_THREADS.add(gst);
    }
}
