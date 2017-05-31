package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.StringTokenizer;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SkillCoolTime;

public class AdminBuffs implements IAdminCommandHandler
{
	private static final int PAGE_LIMIT = 20;
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_getbuffs",
		"admin_stopbuff",
		"admin_stopallbuffs",
		"admin_areacancel",
		"admin_removereuse"
	};
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.startsWith("admin_getbuffs"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			if (st.hasMoreTokens())
			{
				String playername = st.nextToken();
				L2PcInstance player = L2World.getInstance().getPlayer(playername);
				if (player == null)
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
					return false;
				}
				
				int page = 1;
				if (st.hasMoreTokens())
					page = Integer.parseInt(st.nextToken());
				
				showBuffs(activeChar, player, page);
				return true;
			}
			else if ((activeChar.getTarget() != null) && (activeChar.getTarget() instanceof L2Character))
			{
				showBuffs(activeChar, (L2Character) activeChar.getTarget(), 1);
				return true;
			}
			else
			{
				activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				return false;
			}
		}
		else if (command.startsWith("admin_stopbuff"))
		{
			try
			{
				StringTokenizer st = new StringTokenizer(command, " ");
				
				st.nextToken();
				int objectId = Integer.parseInt(st.nextToken());
				int skillId = Integer.parseInt(st.nextToken());
				
				removeBuff(activeChar, objectId, skillId);
				return true;
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Failed removing effect: " + e.getMessage());
				activeChar.sendMessage("Usage: //stopbuff <objectId> <skillId>");
				return false;
			}
		}
		else if (command.startsWith("admin_stopallbuffs"))
		{
			try
			{
				StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				int objectId = Integer.parseInt(st.nextToken());
				removeAllBuffs(activeChar, objectId);
				return true;
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Failed removing all effects: " + e.getMessage());
				activeChar.sendMessage("Usage: //stopallbuffs <objectId>");
				return false;
			}
		}
		else if (command.startsWith("admin_areacancel"))
		{
			try
			{
				StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				String val = st.nextToken();
				int radius = Integer.parseInt(val);
				
				for (L2PcInstance knownChar : activeChar.getKnownList().getKnownTypeInRadius(L2PcInstance.class, radius))
				{
					if (!knownChar.equals(activeChar))
						knownChar.stopAllEffects();
				}
				
				activeChar.sendMessage("All effects canceled within radius " + radius + ".");
				return true;
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Usage: //areacancel <radius>");
				return false;
			}
		}
		else if (command.startsWith("admin_removereuse"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			L2PcInstance player = null;
			if (st.hasMoreTokens())
			{
				final String name = st.nextToken();
				
				player = L2World.getInstance().getPlayer(name);
				if (player == null)
				{
					activeChar.sendMessage("The player " + name + " is not online.");
					return false;
				}
			}
			else if (activeChar.getTarget() instanceof L2PcInstance)
				player = (L2PcInstance) activeChar.getTarget();
			
			if (player == null)
			{
				activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				return false;
			}
			
			player.getReuseTimeStamp().clear();
			player.getDisabledSkills().clear();
			player.sendPacket(new SkillCoolTime(player));
			activeChar.sendMessage(player.getName() + "'s skills reuse time is now cleaned.");
			return true;
		}
		else
			return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	public static void showBuffs(L2PcInstance activeChar, L2Character target, int page)
	{
		final L2Effect[] effects = target.getAllEffects();
		
		if (page > effects.length / PAGE_LIMIT + 1 || page < 1)
			return;
		
		int max = effects.length / PAGE_LIMIT;
		if (effects.length > PAGE_LIMIT * max)
			max++;
		
		final StringBuilder sb = new StringBuilder("<html><table width=\"100%\"><tr><td width=45><button value=\"Main\" action=\"bypass -h admin_admin\" width=45 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td width=180><center><font color=\"LEVEL\">Effects of " + target.getName() + "</font></td><td width=45><button value=\"Back\" action=\"bypass -h admin_current_player\" width=45 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table><br><table width=\"100%\"><tr><td width=160>Skill</td><td width=60>Time Left</td><td width=60>Action</td></tr>");
		
		int start = ((page - 1) * PAGE_LIMIT);
		int end = Math.min(((page - 1) * PAGE_LIMIT) + PAGE_LIMIT, effects.length);
		
		for (int i = start; i < end; i++)
		{
			L2Effect e = effects[i];
			if (e != null)
				StringUtil.append(sb, "<tr><td>", e.getSkill().getName(), "</td><td>", (e.getSkill().isToggle()) ? "toggle" : e.getPeriod() - e.getTime() + "s", "</td><td><a action=\"bypass -h admin_stopbuff ", target.getObjectId(), " ", e.getSkill().getId(), "\">Remove</a></td></tr>");
		}
		
		sb.append("</table><br><table width=\"100%\" bgcolor=444444><tr>");
		for (int x = 0; x < max; x++)
		{
			int pagenr = x + 1;
			if (page == pagenr)
				StringUtil.append(sb, "<td>Page ", pagenr, "</td>");
			else
				StringUtil.append(sb, "<td><a action=\"bypass -h admin_getbuffs ", target.getName(), " ", x + 1, "\"> Page ", pagenr, "</a></td>");
		}
		
		StringUtil.append(sb, "</tr></table><br><center><button value=\"Remove All\" action=\"bypass -h admin_stopallbuffs ", target.getObjectId(), "\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></html>");
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setHtml(sb.toString());
		activeChar.sendPacket(html);
	}
	
	private static void removeBuff(L2PcInstance activeChar, int objId, int skillId)
	{
		if (skillId < 1)
			return;
		
		final L2Object obj = L2World.getInstance().getObject(objId);
		if (obj instanceof L2Character)
		{
			final L2Character target = (L2Character) obj;
			
			for (L2Effect e : target.getAllEffects())
			{
				if (e != null && e.getSkill().getId() == skillId)
				{
					e.exit();
					activeChar.sendMessage("Removed " + e.getSkill().getName() + " level " + e.getSkill().getLevel() + " from " + target.getName() + " (" + objId + ")");
				}
			}
			showBuffs(activeChar, target, 1);
		}
	}
	
	private static void removeAllBuffs(L2PcInstance activeChar, int objId)
	{
		final L2Object target = L2World.getInstance().getObject(objId);
		if (target instanceof L2Character)
		{
			((L2Character) target).stopAllEffects();
			activeChar.sendMessage("Removed all effects from " + target.getName() + " (" + objId + ")");
			showBuffs(activeChar, ((L2Character) target), 1);
		}
	}
}