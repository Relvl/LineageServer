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

import net.sf.l2j.gameserver.EChatType;
import net.sf.l2j.gameserver.network.SystemMessageId;

import java.util.ArrayList;
import java.util.List;

public class CreatureSay extends L2GameServerPacket {
    private final int objectId;
    private final EChatType chatType;
    private String charName;
    private int charId;
    private String text;
    private int npcString = -1;
    private List<String> parameters;

    public CreatureSay(int objectId, EChatType chatType, String charName, String text) {
        this.objectId = objectId;
        this.chatType = chatType;
        this.charName = charName;
        this.text = text;
    }

    public CreatureSay(int objectId, EChatType chatType, int charId, SystemMessageId sysString) {
        this.objectId = objectId;
        this.chatType = chatType;
        this.charId = charId;
        this.npcString = sysString.getId();
    }

    public void addStringParameter(String param) {
        if (parameters == null) {
            parameters = new ArrayList<>();
        }
        parameters.add(param);
    }

    @Override
    protected void writeImpl() {
        writeC(0x4a);
        writeD(objectId);
        writeD(chatType.getCode());
        if (charName != null) { writeS(charName); }
        else { writeD(charId); }
        writeD(npcString); // High Five NPCString ID
        if (text != null) { writeS(text); }
        else {
            if (parameters != null) {
                for (String s : parameters) { writeS(s); }
            }
        }
    }
}