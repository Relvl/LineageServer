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
package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;

/**
 * @author l3x
 */
public class Harvester implements IItemHandler {
    @Override
    public void useItem(L2Playable playable, L2ItemInstance item, boolean forceUse) {
        if (!(playable instanceof L2PcInstance)) { return; }

        if (!(playable.getTarget() instanceof L2MonsterInstance)) {
            playable.sendPacket(SystemMessageId.INCORRECT_TARGET);
            return;
        }

        final L2MonsterInstance _target = (L2MonsterInstance) playable.getTarget();
        if (_target == null || !_target.isDead()) {
            playable.sendPacket(SystemMessageId.INCORRECT_TARGET);
            return;
        }

        final L2Skill skill = SkillTable.getInfo(2098, 1);
        if (skill != null) { playable.useMagic(skill, false, false); }
    }
}