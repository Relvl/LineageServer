package johnson.loginserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public abstract class FloodProtectedListener extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(FloodProtectedListener.class);

    private final Map<String, ForeignConnection> _floodProtection = new HashMap<>();
    private final String listenIp;
    private final int port;
    private final ServerSocket serverSocket;

    public FloodProtectedListener(String listenIp, int port) throws IOException {
        this.port = port;
        this.listenIp = listenIp;

        if ("*".equals(this.listenIp)) {
            serverSocket = new ServerSocket(this.port);
        }
        else {
            serverSocket = new ServerSocket(this.port, 50, InetAddress.getByName(this.listenIp));
        }
    }

    @Override
    public void run() {
        Socket connection = null;

        while (true) {
            try {
                connection = serverSocket.accept();
                if (LoginServer.config.floodProtection.enabled) {
                    ForeignConnection fConnection = _floodProtection.get(connection.getInetAddress().getHostAddress());
                    if (fConnection != null) {
                        fConnection.connectionNumber += 1;
                        if ((fConnection.connectionNumber > LoginServer.config.floodProtection.fastConnectionLimit && (System.currentTimeMillis() - fConnection.lastConnection) < LoginServer.config.floodProtection.normalConnectionTime) ||
                                (System.currentTimeMillis() - fConnection.lastConnection) < LoginServer.config.floodProtection.fastConnectionTime ||
                                fConnection.connectionNumber > LoginServer.config.floodProtection.maxConnectionsPerIP
                                ) {

                            fConnection.lastConnection = System.currentTimeMillis();
                            connection.close();
                            fConnection.connectionNumber -= 1;
                            if (!fConnection.isFlooding) {
                                LOGGER.warn("Potential Flood from {}", connection.getInetAddress().getHostAddress());
                            }
                            fConnection.isFlooding = true;
                            continue;
                        }

                        // if connection was flooding server but now passed the check
                        if (fConnection.isFlooding) {
                            fConnection.isFlooding = false;
                            LOGGER.info("{} is not considered as flooding anymore.", connection.getInetAddress().getHostAddress());
                        }
                        fConnection.lastConnection = System.currentTimeMillis();
                    }
                    else {
                        fConnection = new ForeignConnection(System.currentTimeMillis());
                        _floodProtection.put(connection.getInetAddress().getHostAddress(), fConnection);
                    }
                }
                addClient(connection);
            } catch (RuntimeException | IOException e) {
                try {
                    if (connection != null) {
                        connection.close();
                    }
                } catch (IOException ignored) {}

                if (isInterrupted()) {
                    try {
                        serverSocket.close();
                    } catch (IOException io) {
                        LOGGER.error("", io);
                    }
                    break;
                }
            }
        }
    }

    public abstract void addClient(Socket socket);

    public void removeFloodProtection(String ip) {
        if (!LoginServer.config.floodProtection.enabled) {
            return;
        }
        ForeignConnection fConnection = _floodProtection.get(ip);
        if (fConnection != null) {
            fConnection.connectionNumber -= 1;
            if (fConnection.connectionNumber == 0) {
                _floodProtection.remove(ip);
            }
        }
        else {
            LOGGER.warn("Removing a flood protection for a GameServer that was not in the connection map??? :{}", ip);
        }
    }

    private static final class ForeignConnection {
        private int connectionNumber;
        private long lastConnection;
        private boolean isFlooding;

        private ForeignConnection(long time) {
            lastConnection = time;
            connectionNumber = 1;
        }
    }
}