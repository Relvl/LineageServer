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
package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.gameserver.model.L2ShortCut;

/**
 * format dd d/dd/d d
 */
public class ShortCutRegister extends L2GameServerPacket {
    private final L2ShortCut _shortcut;

    public ShortCutRegister(L2ShortCut shortcut) {
        _shortcut = shortcut;
    }

    @Override
    protected final void writeImpl() {
        writeC(0x44);

        writeD(_shortcut.getType());
        writeD(_shortcut.getSlot() + _shortcut.getPage() * 12); // C4 Client
        switch (_shortcut.getType()) {
            case L2ShortCut.TYPE_ITEM: // 1
                writeD(_shortcut.getId());
                writeD(_shortcut.getCharacterType());
                writeD(_shortcut.getSharedReuseGroup());
                break;
            case L2ShortCut.TYPE_SKILL: // 2
                writeD(_shortcut.getId());
                writeD(_shortcut.getLevel());
                writeC(0x00); // C5
                writeD(_shortcut.getCharacterType());
                break;
            default:
                writeD(_shortcut.getId());
                writeD(_shortcut.getCharacterType());
        }
    }
}