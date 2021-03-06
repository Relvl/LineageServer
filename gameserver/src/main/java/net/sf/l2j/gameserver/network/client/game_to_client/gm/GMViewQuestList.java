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
package net.sf.l2j.gameserver.network.client.game_to_client.gm;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.client.game_to_client.L2GameServerPacket;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

import java.util.List;

/**
 * Sh (dd) h (dddd)
 *
 * @author Tempy
 */
public class GMViewQuestList extends L2GameServerPacket {
    private final L2PcInstance _activeChar;

    public GMViewQuestList(L2PcInstance cha) {
        _activeChar = cha;
    }

    @Override
    protected final void writeImpl() {
        writeC(0x93);
        writeS(_activeChar.getName());

        List<Quest> quests = _activeChar.getAllQuests(true);

        writeH(quests.size());
        for (Quest q : quests) {
            writeD(q.getQuestId());
            QuestState qs = _activeChar.getQuestState(q.getName());
            if (qs == null) {
                writeD(0);
                continue;
            }

            int states = qs.getInt("__compltdStateFlags");
            if (states != 0) { writeD(states); }
            else { writeD(qs.getInt("cond")); }
        }
    }
}