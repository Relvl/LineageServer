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
package net.sf.l2j.gameserver.model.actor.instance;

import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;

/**
 * @author Drunkard Zabb0x Lets drink2code!
 */
public class L2XmassTreeInstance extends L2NpcInstance
{
	private static final L2Skill XTREE_SKILL = SkillTable.getInstance().getInfo(2139, 1);
	public static final int SPECIAL_TREE_ID = 13007;
	protected ScheduledFuture<?> _aiTask;
	
	public L2XmassTreeInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		
		if (template.isSpecialTree())
			_aiTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new XmassAI(this, XTREE_SKILL), 3000, 3000);
	}
	
	private class XmassAI implements Runnable
	{
		private final L2XmassTreeInstance _caster;
		private final L2Skill _skill;
		
		protected XmassAI(L2XmassTreeInstance caster, L2Skill skill)
		{
			_caster = caster;
			_skill = skill;
		}
		
		@Override
		public void run()
		{
			if (_skill == null || _caster.isInsideZone(ZoneId.TOWN))
			{
				_caster._aiTask.cancel(false);
				_caster._aiTask = null;
				return;
			}
			
			for (L2PcInstance player : getKnownList().getKnownTypeInRadius(L2PcInstance.class, 200))
			{
				if (player.getFirstEffect(_skill.getId()) == null)
					_skill.getEffects(player, player);
			}
		}
	}
	
	@Override
	public void deleteMe()
	{
		if (_aiTask != null)
		{
			_aiTask.cancel(true);
			_aiTask = null;
		}
		super.deleteMe();
	}
	
	@Override
	public void onAction(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}