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

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.client.game_to_client.L2GameServerPacket;
import org.mmocore.network.ReceivablePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.BufferUnderflowException;

public abstract class L2GameClientPacket extends ReceivablePacket<L2GameClient> {
    protected static final Logger _log = LoggerFactory.getLogger(L2GameClientPacket.class);

    @Override
    protected boolean read() {
        try {
            readImpl();
            return true;
        }
        catch (Exception e) {
            _log.error("Client: {} - Failed reading: {} ; {}", getClient(), getType(), e, e);

            if (e instanceof BufferUnderflowException) // only one allowed per client per minute
            { getClient().onBufferUnderflow(); }
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
                L2PcInstance actor = getClient().getActiveChar();
                if (actor != null && actor.isSpawnProtected()) {
                    actor.onActionRequest();
                }
            }
        }
        catch (Throwable t) {
            _log.error("Client: {} - Failed reading: {} ; {}", getClient(), getType(), t, t);

            if (this instanceof EnterWorld) { getClient().closeNow(); }
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