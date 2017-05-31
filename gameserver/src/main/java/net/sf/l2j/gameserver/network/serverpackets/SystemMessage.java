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
package net.sf.l2j.gameserver.network.serverpackets;

import java.util.Arrays;
import java.util.logging.Level;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.network.SystemMessageId;

public final class SystemMessage extends L2GameServerPacket
{
	private static final SMParam[] EMPTY_PARAM_ARRAY = new SMParam[0];
	
	private static final class SMParam
	{
		private final byte _type;
		private final Object _value;
		
		public SMParam(final byte type, final Object value)
		{
			_type = type;
			_value = value;
		}
		
		public final byte getType()
		{
			return _type;
		}
		
		public final String getStringValue()
		{
			return (String) _value;
		}
		
		public final int getIntValue()
		{
			return ((Integer) _value).intValue();
		}
		
		public final int[] getIntArrayValue()
		{
			return (int[]) _value;
		}
	}
	
	private static final byte TYPE_ZONE_NAME = 7;
	private static final byte TYPE_ITEM_NUMBER = 6;
	private static final byte TYPE_CASTLE_NAME = 5;
	private static final byte TYPE_SKILL_NAME = 4;
	private static final byte TYPE_ITEM_NAME = 3;
	private static final byte TYPE_NPC_NAME = 2;
	private static final byte TYPE_NUMBER = 1;
	private static final byte TYPE_TEXT = 0;
	
	public static final SystemMessage sendString(final String text)
	{
		if (text == null)
			throw new NullPointerException();
		
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1);
		sm.addString(text);
		return sm;
	}
	
	public static final SystemMessage getSystemMessage(final SystemMessageId smId)
	{
		SystemMessage sm = smId.getStaticSystemMessage();
		if (sm != null)
			return sm;
		
		sm = new SystemMessage(smId);
		if (smId.getParamCount() == 0)
			smId.setStaticSystemMessage(sm);
		
		return sm;
	}
	
	/**
	 * Use {@link #getSystemMessage(SystemMessageId)} where possible instead
	 * @param id
	 * @return the system message associated to the given Id.
	 */
	public static SystemMessage getSystemMessage(int id)
	{
		return getSystemMessage(SystemMessageId.getSystemMessageId(id));
	}
	
	private final SystemMessageId _smId;
	private SMParam[] _params;
	private int _paramIndex;
	
	private SystemMessage(final SystemMessageId smId)
	{
		final int paramCount = smId.getParamCount();
		_smId = smId;
		_params = paramCount != 0 ? new SMParam[paramCount] : EMPTY_PARAM_ARRAY;
	}
	
	private final void append(final SMParam param)
	{
		if (_paramIndex >= _params.length)
		{
			_params = Arrays.copyOf(_params, _paramIndex + 1);
			_smId.setParamCount(_paramIndex + 1);
			_log.log(Level.INFO, "Wrong parameter count '" + (_paramIndex + 1) + "' for SystemMessageId: " + _smId);
		}
		
		_params[_paramIndex++] = param;
	}
	
	public final SystemMessage addString(final String text)
	{
		append(new SMParam(TYPE_TEXT, text));
		return this;
	}
	
	/**
	 * Castlename-e.dat<br>
	 * 0-9 Castle names<br>
	 * 21-64 CH names<br>
	 * 81-89 Territory names<br>
	 * 101-121 Fortress names<br>
	 * @param number
	 * @return
	 */
	public final SystemMessage addFortId(final int number)
	{
		append(new SMParam(TYPE_CASTLE_NAME, number));
		return this;
	}
	
	public final SystemMessage addNumber(final int number)
	{
		append(new SMParam(TYPE_NUMBER, number));
		return this;
	}
	
	public final SystemMessage addItemNumber(final int number)
	{
		append(new SMParam(TYPE_ITEM_NUMBER, number));
		return this;
	}
	
	public final SystemMessage addCharName(final L2Character cha)
	{
		if (cha instanceof L2Npc)
		{
			if (((L2Npc) cha).getTemplate().isCustomNpc())
				return addString(((L2Npc) cha).getTemplate().getName());
			
			return addNpcName((L2Npc) cha);
		}
		else if (cha instanceof L2PcInstance)
		{
			return addPcName((L2PcInstance) cha);
		}
		else if (cha instanceof L2Summon)
		{
			if (((L2Summon) cha).getTemplate().isCustomNpc())
				return addString(((L2Summon) cha).getTemplate().getName());
			
			return addNpcName((L2Summon) cha);
		}
		return addString(cha.getName());
	}
	
	public final SystemMessage addPcName(final L2PcInstance pc)
	{
		append(new SMParam(TYPE_TEXT, pc.getName()));
		return this;
	}
	
	public final SystemMessage addNpcName(final L2Npc npc)
	{
		return addNpcName(npc.getTemplate());
	}
	
	public final SystemMessage addNpcName(final L2Summon npc)
	{
		return addNpcName(npc.getNpcId());
	}
	
	public final SystemMessage addNpcName(final NpcTemplate template)
	{
		return addNpcName(template.getNpcId());
	}
	
	public final SystemMessage addNpcName(final int id)
	{
		append(new SMParam(TYPE_NPC_NAME, 1000000 + id));
		return this;
	}
	
	public final SystemMessage addItemName(final ItemInstance item)
	{
		return addItemName(item.getItem().getItemId());
	}
	
	public final SystemMessage addItemName(final Item item)
	{
		return addItemName(item.getItemId());
	}
	
	public final SystemMessage addItemName(final int id)
	{
		append(new SMParam(TYPE_ITEM_NAME, id));
		return this;
	}
	
	public final SystemMessage addZoneName(final int x, final int y, final int z)
	{
		append(new SMParam(TYPE_ZONE_NAME, new int[]
		{
			x,
			y,
			z
		}));
		return this;
	}
	
	public final SystemMessage addSkillName(final L2Effect effect)
	{
		return addSkillName(effect.getSkill());
	}
	
	public final SystemMessage addSkillName(final L2Skill skill)
	{
		return addSkillName(skill.getId(), skill.getLevel());
	}
	
	public final SystemMessage addSkillName(final int id)
	{
		return addSkillName(id, 1);
	}
	
	public final SystemMessage addSkillName(final int id, final int lvl)
	{
		append(new SMParam(TYPE_SKILL_NAME, new int[]
		{
			id,
			lvl
		}));
		return this;
	}
	
	public final SystemMessageId getSystemMessageId()
	{
		return _smId;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x64);
		
		writeD(_smId.getId());
		writeD(_paramIndex);
		
		SMParam param;
		for (int i = 0; i < _paramIndex; i++)
		{
			param = _params[i];
			writeD(param.getType());
			
			switch (param.getType())
			{
				case TYPE_TEXT:
				{
					writeS(param.getStringValue());
					break;
				}
				
				case TYPE_ITEM_NUMBER:
				case TYPE_ITEM_NAME:
				case TYPE_CASTLE_NAME:
				case TYPE_NUMBER:
				case TYPE_NPC_NAME:
				{
					writeD(param.getIntValue());
					break;
				}
				
				case TYPE_SKILL_NAME:
				{
					final int[] array = param.getIntArrayValue();
					writeD(array[0]); // SkillId
					writeD(array[1]); // SkillLevel
					break;
				}
				
				case TYPE_ZONE_NAME:
				{
					final int[] array = param.getIntArrayValue();
					writeD(array[0]); // x
					writeD(array[1]); // y
					writeD(array[2]); // z
					break;
				}
			}
		}
	}
}