package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.network.client.game_to_client.KeyPacket;

public final class ProtocolVersion extends L2GameClientPacket {
    private int _version;

    @Override
    protected void readImpl() {
        _version = readD();
    }

    @Override
    protected void runImpl() {
        if (_version == -2) {
            getClient().close(null);
        }
        else if (_version < Config.MIN_PROTOCOL_REVISION || _version > Config.MAX_PROTOCOL_REVISION) {
            _log.warn("Client: {} -> Protocol Revision: {} is invalid. Minimum and maximum allowed are: {} and {}. Closing connection.", getClient(), _version, Config.MIN_PROTOCOL_REVISION, Config.MAX_PROTOCOL_REVISION);
            getClient().close(null);
        }
        else { getClient().sendPacket(new KeyPacket(getClient().enableCrypt())); }
    }
}