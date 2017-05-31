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
package net.sf.l2j.loginserver;

import net.sf.l2j.loginserver.L2LoginClient.LoginClientState;
import net.sf.l2j.loginserver.network.clientpackets.AuthGameGuard;
import net.sf.l2j.loginserver.network.clientpackets.RequestAuthLogin;
import net.sf.l2j.loginserver.network.clientpackets.RequestServerList;
import net.sf.l2j.loginserver.network.clientpackets.RequestServerLogin;
import org.mmocore.network.IPacketHandler;
import org.mmocore.network.ReceivablePacket;

import java.nio.ByteBuffer;

/**
 * Handler for packets received by Login Server
 *
 * @author KenM
 */
public final class L2LoginPacketHandler implements IPacketHandler<L2LoginClient> {
    private static void debugOpcode(int opcode, LoginClientState state) {
        System.out.println("Unknown Opcode: " + opcode + " for state: " + state.name());
    }

    @Override
    public ReceivablePacket<L2LoginClient> handlePacket(ByteBuffer buf, L2LoginClient client) {
        int opcode = buf.get() & 0xFF;

        ReceivablePacket<L2LoginClient> packet = null;
        LoginClientState state = client.getState();

        switch (state) {
            case CONNECTED:
                if (opcode == 0x07)
                    packet = new AuthGameGuard();
                else
                    debugOpcode(opcode, state);
                break;

            case AUTHED_GG:
                if (opcode == 0x00)
                    packet = new RequestAuthLogin();
                else
                    debugOpcode(opcode, state);
                break;

            case AUTHED_LOGIN:
                if (opcode == 0x05)
                    packet = new RequestServerList();
                else if (opcode == 0x02)
                    packet = new RequestServerLogin();
                else
                    debugOpcode(opcode, state);
                break;
        }
        return packet;
    }
}