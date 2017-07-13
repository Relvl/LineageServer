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
package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2ChestInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

public class Unlock implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS =
	{
		L2SkillType.UNLOCK,
		L2SkillType.UNLOCK_SPECIAL
	};
	
	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		final L2Object object = targets[0];
		
		if (object instanceof L2DoorInstance)
		{
			final L2DoorInstance door = (L2DoorInstance) object;
			if (!door.isUnlockable() && skill.getSkillType() != L2SkillType.UNLOCK_SPECIAL)
			{
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.UNABLE_TO_UNLOCK_DOOR));
				return;
			}
			
			if (doorUnlock(skill) && (!door.isOpened()))
			{
				door.openMe();
				door.onOpen();
			}
			else
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_UNLOCK_DOOR));
		}
		else if (object instanceof L2ChestInstance)
		{
			final L2ChestInstance chest = (L2ChestInstance) object;
			if (chest.isDead() || chest.isInteracted())
				return;
			
			chest.setInteracted();
			if (chestUnlock(skill, chest))
			{
				chest.setSpecialDrop();
				chest.doDie(null);
			}
			else
			{
				chest.addDamageHate(activeChar, 0, 999);
				chest.getAI().setIntention(EIntention.ATTACK, activeChar);
			}
		}
		else
			activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.INCORRECT_TARGET));
	}
	
	private static final boolean doorUnlock(L2Skill skill)
	{
		if (skill.getSkillType() == L2SkillType.UNLOCK_SPECIAL)
			return Rnd.get(100) < skill.getPower();
		
		switch (skill.getLevel())
		{
			case 0:
				return false;
			case 1:
				return Rnd.get(120) < 30;
			case 2:
				return Rnd.get(120) < 50;
			case 3:
				return Rnd.get(120) < 75;
			default:
				return Rnd.get(120) < 100;
		}
	}
	
	private static final boolean chestUnlock(L2Skill skill, L2Character chest)
	{
		int chance = 0;
		if (chest.getLevel() > 60)
		{
			if (skill.getLevel() < 10)
				return false;
			
			chance = (skill.getLevel() - 10) * 5 + 30;
		}
		else if (chest.getLevel() > 40)
		{
			if (skill.getLevel() < 6)
				return false;
			
			chance = (skill.getLevel() - 6) * 5 + 10;
		}
		else if (chest.getLevel() > 30)
		{
			if (skill.getLevel() < 3)
				return false;
			
			if (skill.getLevel() > 12)
				return true;
			
			chance = (skill.getLevel() - 3) * 5 + 30;
		}
		else
		{
			if (skill.getLevel() > 10)
				return true;
			
			chance = skill.getLevel() * 5 + 35;
		}
		
		chance = Math.min(chance, 50);
		return Rnd.get(100) < chance;
	}
	
	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}