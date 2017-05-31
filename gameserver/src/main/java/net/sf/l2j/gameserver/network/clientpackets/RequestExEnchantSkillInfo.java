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
package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.model.L2EnchantSkillData;
import net.sf.l2j.gameserver.model.L2EnchantSkillLearn;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExEnchantSkillInfo;

/**
 * Format chdd c: (id) 0xD0 h: (subid) 0x06 d: skill id d: skill lvl
 * @author -Wooden-
 */
public final class RequestExEnchantSkillInfo extends L2GameClientPacket
{
	private int _skillId;
	private int _skillLevel;
	
	@Override
	protected void readImpl()
	{
		_skillId = readD();
		_skillLevel = readD();
	}
	
	@Override
	protected void runImpl()
	{
		if (_skillId <= 0 || _skillLevel <= 0)
			return;
		
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		
		if (activeChar.getClassId().level() < 3 || activeChar.getLevel() < 76)
			return;
		
		final L2Npc trainer = activeChar.getCurrentFolkNPC();
		if (trainer == null)
			return;
		
		if (!activeChar.isInsideRadius(trainer, L2Npc.INTERACTION_DISTANCE, false, false) && !activeChar.isGM())
			return;
		
		if (activeChar.getSkillLevel(_skillId) >= _skillLevel)
			return;
		
		final L2Skill skill = SkillTable.getInstance().getInfo(_skillId, _skillLevel);
		if (skill == null)
			return;
		
		if (!trainer.getTemplate().canTeach(activeChar.getClassId()))
			return;
		
		// Try to find enchant skill.
		for (L2EnchantSkillLearn esl : SkillTreeTable.getInstance().getAvailableEnchantSkills(activeChar))
		{
			if (esl == null)
				continue;
			
			if (esl.getId() == _skillId && esl.getLevel() == _skillLevel)
			{
				L2EnchantSkillData data = SkillTreeTable.getInstance().getEnchantSkillData(esl.getEnchant());
				// Enchant skill or enchant data not found.
				if (data == null)
					return;
				
				// Send ExEnchantSkillInfo packet.
				ExEnchantSkillInfo esi = new ExEnchantSkillInfo(_skillId, _skillLevel, data.getCostSp(), data.getCostExp(), data.getRate(activeChar.getLevel()));
				if (Config.ES_SP_BOOK_NEEDED)
					if (data.getItemId() != 0 && data.getItemCount() != 0)
						esi.addRequirement(4, data.getItemId(), data.getItemCount(), 0);
				sendPacket(esi);
				
				break;
			}
		}
	}
}