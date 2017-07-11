/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.client.game_to_client.L2GameServerPacket;
import org.mmocore.network.ReceivablePacket;

import java.nio.BufferUnderflowException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Packets received by the game server from clients
 *
 * @author KenM
 */
public abstract class L2GameClientPacket extends ReceivablePacket<L2GameClient> {
    protected static final Logger _log = Logger.getLogger(L2GameClientPacket.class.getName());

    @Override
    protected boolean read() {
        if (Config.PACKET_HANDLER_DEBUG)
            _log.info(getType());

        try {
            readImpl();
            return true;
        } catch (Exception e) {
            _log.log(Level.SEVERE, "Client: " + getClient().toString() + " - Failed reading: " + getType() + " ; " + e, e);

            if (e instanceof BufferUnderflowException) // only one allowed per client per minute
                getClient().onBufferUnderflow();
        }
        return false;
    }

    protected abstract void readImpl();

    @Override
    public void run() {
        try {
            runImpl();

            // Depending of the packet send, removes spawn protection
            if (triggersOnActionRequest()) {
                final L2PcInstance actor = getClient().getActiveChar();
                if (actor != null && actor.isSpawnProtected()) {
                    actor.onActionRequest();
                    if (Config.DEBUG)
                        _log.info("Spawn protection for player " + actor.getName() + " removed by packet: " + getType());
                }
            }
        } catch (Throwable t) {
            _log.log(Level.SEVERE, "Client: " + getClient().toString() + " - Failed reading: " + getType() + " ; " + t, t);

            if (this instanceof EnterWorld)
                getClient().closeNow();
        }
    }

    protected abstract void runImpl();

    protected final void sendPacket(L2GameServerPacket gsp) {
        getClient().sendPacket(gsp);
    }

    /**
     * @return A String with this packet name for debuging purposes
     */
    public String getType() {
        return "[C] " + getClass().getSimpleName();
    }

    /**
     * Overriden with true value on some packets that should disable spawn protection
     *
     * @return
     */
    protected boolean triggersOnActionRequest() {
        return true;
    }
}