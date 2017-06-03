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
package johnson.loginserver.network.serverpackets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Fromat: d d: response
 */
public final class GGAuth extends L2LoginServerPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(GGAuth.class);
    private final int _response;

    public GGAuth(int response) {
        _response = response;
        LOGGER.debug("Reason Hex: {}", Integer.toHexString(response));
    }

    @Override
    protected void write() {
        writeC(0x0b);
        writeD(_response);
        writeD(0x00);
        writeD(0x00);
        writeD(0x00);
        writeD(0x00);
    }
}
