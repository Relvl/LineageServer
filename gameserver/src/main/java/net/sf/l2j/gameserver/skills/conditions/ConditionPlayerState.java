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
package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.skills.Env;

/**
 * @author mkizub
 */
public class ConditionPlayerState extends Condition
{
	public enum PlayerState
	{
		RESTING,
		MOVING,
		RUNNING,
		RIDING,
		FLYING,
		BEHIND,
		FRONT,
		OLYMPIAD
	}
	
	private final PlayerState _check;
	private final boolean _required;
	
	public ConditionPlayerState(PlayerState check, boolean required)
	{
		_check = check;
		_required = required;
	}
	
	@Override
	public boolean testImpl(Env env)
	{
		final L2Character character = env.getCharacter();
		final L2PcInstance player = env.getPlayer();
		
		switch (_check)
		{
			case RESTING:
				return (player == null) ? !_required : player.isSitting() == _required;
				
			case MOVING:
				return character.isMoving() == _required;
				
			case RUNNING:
				return character.isMoving() == _required && character.isRunning() == _required;
				
			case RIDING:
				return character.isRiding() == _required;
				
			case FLYING:
				return character.isFlying() == _required;
				
			case BEHIND:
				return character.isBehindTarget() == _required;
				
			case FRONT:
				return character.isInFrontOfTarget() == _required;
				
			case OLYMPIAD:
				return (player == null) ? !_required : player.isInOlympiadMode() == _required;
		}
		return !_required;
	}
}