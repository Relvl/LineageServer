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
    private final String _listenIp;
    private final int _port;
    private ServerSocket _serverSocket;

    public FloodProtectedListener(String listenIp, int port) throws IOException {
        _port = port;
        _listenIp = listenIp;

        if (_listenIp.equals("*")) {
            _serverSocket = new ServerSocket(_port);
        }
        else {
            _serverSocket = new ServerSocket(_port, 50, InetAddress.getByName(_listenIp));
        }
    }

    @Override
    public void run() {
        Socket connection = null;

        while (true) {
            try {
                connection = _serverSocket.accept();
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
            } catch (Exception e) {
                try {
                    if (connection != null) {
                        connection.close();
                    }
                } catch (Exception ignored) {}

                if (isInterrupted()) {
                    try {
                        _serverSocket.close();
                    } catch (IOException io) {
                        LOGGER.error("", io);
                    }
                    break;
                }
            }
        }
    }

    public abstract void addClient(Socket s);

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

    public void close() {
        try {
            _serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static class ForeignConnection {
        public int connectionNumber;
        public long lastConnection;
        public boolean isFlooding = false;

        public ForeignConnection(long time) {
            lastConnection = time;
            connectionNumber = 1;
        }
    }
}