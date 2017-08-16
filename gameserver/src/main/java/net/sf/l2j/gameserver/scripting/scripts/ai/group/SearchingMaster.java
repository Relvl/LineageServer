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
package net.sf.l2j.gameserver.scripting.scripts.ai.group;

import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.scripting.EventType;
import net.sf.l2j.gameserver.scripting.scripts.ai.AbstractNpcAI;

public class SearchingMaster extends AbstractNpcAI {
    private static final int[] mobs =
            {
                    20965,
                    20966,
                    20967,
                    20968,
                    20969,
                    20970,
                    20971,
                    20972,
                    20973
            };

    public SearchingMaster() {
        super("ai/group");

        registerMobs(mobs, EventType.ON_ATTACK);
    }

    @Override
    public String onAttack(L2Npc npc, L2PcInstance player, int damage, boolean isPet) {
        if (player == null) { return null; }

        attack(((L2Attackable) npc), player);
        return super.onAttack(npc, player, damage, isPet);
    }
}