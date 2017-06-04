package johnson.loginserver;

import johnson.loginserver.network.client.client_to_login.AuthGameGuard;
import johnson.loginserver.network.client.client_to_login.RequestAuth;
import johnson.loginserver.network.client.client_to_login.RequestServerList;
import johnson.loginserver.network.client.client_to_login.RequestServer;
import org.mmocore.network.IPacketHandler;
import org.mmocore.network.ReceivablePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public final class L2LoginPacketHandler implements IPacketHandler<L2LoginClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(L2LoginPacketHandler.class);

    private static void debugOpcode(int opcode, ELoginClientState state) {
        LOGGER.error("Unknown Opcode: {} for state: {}", opcode, state.name());
    }

    @Override
    public ReceivablePacket<L2LoginClient> handlePacket(ByteBuffer buf, L2LoginClient client) {
        int opcode = buf.get() & 0xFF;

        switch (client.getState()) {
            case CONNECTED:
                if (opcode == 0x07) { return new AuthGameGuard(); }
                else { debugOpcode(opcode, client.getState()); }
                break;

            case AUTHED_GG:
                if (opcode == 0x00) { return new RequestAuth(); }
                else { debugOpcode(opcode, client.getState()); }
                break;

            case AUTHED_LOGIN:
                if (opcode == 0x05) { return new RequestServerList(); }
                else if (opcode == 0x02) { return new RequestServer(); }
                else { debugOpcode(opcode, client.getState()); }
                break;
        }
        return null;
    }
}