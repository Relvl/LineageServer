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

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ai.CtrlEvent;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;

public class L2PenaltyMonsterInstance extends L2MonsterInstance
{
	private L2PcInstance _ptk;
	
	public L2PenaltyMonsterInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public L2Character getMostHated()
	{
		if (_ptk != null)
			return _ptk; // always attack only one person
			
		return super.getMostHated();
	}
	
	public void setPlayerToKill(L2PcInstance ptk)
	{
		if (Rnd.get(100) <= 80)
			broadcastNpcSay("Your bait was too delicious! Now, I will kill you!");
		
		_ptk = ptk;
		
		getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, _ptk, Rnd.get(1, 100));
	}
	
	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
			return false;
		
		if (Rnd.get(100) <= 75)
			broadcastNpcSay("I will tell fish not to take your bait!");
		
		return true;
	}
}