package johnson.loginserver.network;

import johnson.loginserver.L2LoginClient;
import org.mmocore.network.ReceivablePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ABaseLoginClientPacket extends ReceivablePacket<L2LoginClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ABaseLoginClientPacket.class);

    @Override
    protected final boolean read() {
        try {
            return readImpl();
        } catch (Exception e) {
            LOGGER.error("ERROR READING: {}", this.getClass().getSimpleName(), e);
            return false;
        }
    }

    protected abstract boolean readImpl();
}
