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

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.EtcItem;

/**
 * Template for item skills handler.
 * @author Hasha
 */
public class SoulCrystals implements IItemHandler
{
	@Override
	public void useItem(L2Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof L2PcInstance))
			return;
		
		final EtcItem etcItem = item.getEtcItem();
		
		final IntIntHolder[] skills = etcItem.getSkills();
		if (skills == null)
			return;
		
		final L2Skill itemSkill = skills[0].getSkill();
		if (itemSkill == null || itemSkill.getId() != 2096)
			return;
		
		final L2PcInstance player = (L2PcInstance) playable;
		
		if (player.isCastingNow())
			return;
		
		if (!itemSkill.checkCondition(player, player.getTarget(), false))
			return;
		
		// No message on retail, the use is just forgotten.
		if (player.isSkillDisabled(itemSkill))
			return;
		
		player.getAI().setIntention(CtrlIntention.IDLE);
		if (!player.useMagic(itemSkill, forceUse, false))
			return;
		
		int reuseDelay = itemSkill.getReuseDelay();
		if (etcItem.getReuseDelay() > reuseDelay)
			reuseDelay = etcItem.getReuseDelay();
		
		player.addTimeStamp(itemSkill, reuseDelay);
		if (reuseDelay != 0)
			player.disableSkill(itemSkill, reuseDelay);
	}
}