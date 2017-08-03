package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.gameserver.network.L2GameClient;
import org.mmocore.network.SendablePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class L2GameServerPacket extends SendablePacket<L2GameClient> {
    protected static final Logger LOGGER = LoggerFactory.getLogger(L2GameServerPacket.class);

    @Override
    protected void write() {
        try {
            writeImpl();
        }
        catch (Throwable t) {
            LOGGER.error("Client: {} - Failed writing: {}", getClient(), getType(), t);
        }
    }

    public void runImpl() {
    }

    protected abstract void writeImpl();

    public String getType() {
        return "[S] " + getClass().getSimpleName();
    }
}