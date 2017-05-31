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
package net.sf.l2j.loginserver.network.serverpackets;

public final class PlayFail extends L2LoginServerPacket {
    private final PlayFailReason _reason;

    public PlayFail(PlayFailReason reason) {
        _reason = reason;
    }

    @Override
    protected void write() {
        writeC(0x06);
        writeC(_reason.getCode());
    }

    public static enum PlayFailReason {
        REASON_SYSTEM_ERROR(0x01),
        REASON_USER_OR_PASS_WRONG(0x02),
        REASON3(0x03),
        REASON4(0x04),
        REASON_TOO_MANY_PLAYERS(0x0f);

        private final int _code;

        PlayFailReason(int code) {
            _code = code;
        }

        public final int getCode() {
            return _code;
        }
    }
}