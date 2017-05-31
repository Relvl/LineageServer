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

import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.gameserver.util.Util;

/**
 * @authors BiTi, Sami
 */
public class SummonFriend implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS =
	{
		L2SkillType.SUMMON_FRIEND
	};
	
	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		if (!(activeChar instanceof L2PcInstance))
			return;
		
		final L2PcInstance player = (L2PcInstance) activeChar;
		
		// Check player status.
		if (!L2PcInstance.checkSummonerStatus(player))
			return;
		
		for (L2Object obj : targets)
		{
			// The target must be a player.
			if (!(obj instanceof L2PcInstance))
				continue;
			
			// Can't summon yourself.
			final L2PcInstance target = ((L2PcInstance) obj);
			if (activeChar == target)
				continue;
			
			// Check target status.
			if (!L2PcInstance.checkSummonTargetStatus(target, player))
				continue;
			
			// Check target distance.
			if (Util.checkIfInRange(50, activeChar, target, false))
				continue;
			
			// Check target teleport request status.
			if (!target.teleportRequest(player, skill))
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_ALREADY_SUMMONED).addPcName(target));
				continue;
			}
			
			// Send a request for Summon Friend skill.
			if (skill.getId() == 1403)
			{
				final ConfirmDlg confirm = new ConfirmDlg(SystemMessageId.S1_WISHES_TO_SUMMON_YOU_FROM_S2_DO_YOU_ACCEPT.getId());
				confirm.addPcName(player);
				confirm.addZoneName(activeChar.getX(), activeChar.getY(), activeChar.getZ());
				confirm.addTime(30000);
				confirm.addRequesterId(player.getObjectId());
				target.sendPacket(confirm);
			}
			else
			{
				L2PcInstance.teleToTarget(target, player, skill);
				target.teleportRequest(null, null);
			}
		}
	}
	
	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}