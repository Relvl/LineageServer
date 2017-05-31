/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package net.sf.l2j.gameserver.model.actor.instance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.sf.l2j.Config;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.datatables.BufferTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class L2BufferInstance extends L2NpcInstance
{
	public L2BufferInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String currentCommand = st.nextToken();
		
		if (currentCommand.startsWith("menu"))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile(getHtmlPath(getNpcId(), 0));
			html.replace("%objectId%", getObjectId());
			player.sendPacket(html);
		}
		else if (currentCommand.startsWith("cleanup"))
		{
			player.stopAllEffectsExceptThoseThatLastThroughDeath();
			
			final L2Summon summon = player.getPet();
			if (summon != null)
				summon.stopAllEffectsExceptThoseThatLastThroughDeath();
			
			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile(getHtmlPath(getNpcId(), 0));
			html.replace("%objectId%", getObjectId());
			player.sendPacket(html);
		}
		else if (currentCommand.startsWith("heal"))
		{
			player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
			player.setCurrentCp(player.getMaxCp());
			
			final L2Summon summon = player.getPet();
			if (summon != null)
				summon.setCurrentHpMp(summon.getMaxHp(), summon.getMaxMp());
			
			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile(getHtmlPath(getNpcId(), 0));
			html.replace("%objectId%", getObjectId());
			player.sendPacket(html);
		}
		else if (currentCommand.startsWith("support"))
		{
			showGiveBuffsWindow(player, st.nextToken());
		}
		else if (currentCommand.startsWith("givebuffs"))
		{
			final String targetType = st.nextToken();
			final String schemeName = st.nextToken();
			final int cost = Integer.parseInt(st.nextToken());
			
			final L2Character target = (targetType.equalsIgnoreCase("pet")) ? player.getPet() : player;
			if (target == null)
				player.sendMessage("You don't have a pet.");
			else if (cost == 0 || player.reduceAdena("NPC Buffer", cost, this, true))
			{
				for (int skillId : BufferTable.getInstance().getScheme(player.getObjectId(), schemeName))
					SkillTable.getInstance().getInfo(skillId, SkillTable.getInstance().getMaxLevel(skillId)).getEffects(this, target);
			}
			showGiveBuffsWindow(player, targetType);
		}
		else if (currentCommand.startsWith("editschemes"))
		{
			showEditSchemeWindow(player, st.nextToken(), st.nextToken());
		}
		else if (currentCommand.startsWith("skill"))
		{
			final String groupType = st.nextToken();
			final String schemeName = st.nextToken();
			
			final int skillId = Integer.parseInt(st.nextToken());
			
			final List<Integer> skills = BufferTable.getInstance().getScheme(player.getObjectId(), schemeName);
			
			if (currentCommand.startsWith("skillselect") && !schemeName.equalsIgnoreCase("none"))
			{
				if (skills.size() < Config.BUFFER_MAX_SKILLS)
					skills.add(skillId);
				else
					player.sendMessage("This scheme has reached the maximum amount of buffs.");
			}
			else if (currentCommand.startsWith("skillunselect"))
				skills.remove(Integer.valueOf(skillId));
			
			showEditSchemeWindow(player, groupType, schemeName);
		}
		else if (currentCommand.startsWith("manageschemes"))
		{
			showManageSchemeWindow(player);
		}
		else if (currentCommand.startsWith("createscheme"))
		{
			try
			{
				final String schemeName = st.nextToken();
				if (schemeName.length() > 14)
				{
					player.sendMessage("Scheme's name must contain up to 14 chars. Spaces are trimmed.");
					showManageSchemeWindow(player);
					return;
				}
				
				final Map<String, ArrayList<Integer>> schemes = BufferTable.getInstance().getPlayerSchemes(player.getObjectId());
				if (schemes != null)
				{
					if (schemes.size() == Config.BUFFER_MAX_SCHEMES)
					{
						player.sendMessage("Maximum schemes amount is already reached.");
						showManageSchemeWindow(player);
						return;
					}
					
					if (schemes.containsKey(schemeName))
					{
						player.sendMessage("The scheme name already exists.");
						showManageSchemeWindow(player);
						return;
					}
				}
				
				BufferTable.getInstance().setScheme(player.getObjectId(), schemeName.trim(), new ArrayList<Integer>());
				showManageSchemeWindow(player);
			}
			catch (Exception e)
			{
				player.sendMessage("Scheme's name must contain up to 14 chars. Spaces are trimmed.");
				showManageSchemeWindow(player);
			}
		}
		else if (currentCommand.startsWith("deletescheme"))
		{
			try
			{
				final String schemeName = st.nextToken();
				final Map<String, ArrayList<Integer>> schemes = BufferTable.getInstance().getPlayerSchemes(player.getObjectId());
				
				if (schemes != null && schemes.containsKey(schemeName))
					schemes.remove(schemeName);
			}
			catch (Exception e)
			{
				player.sendMessage("This scheme name is invalid.");
			}
			showManageSchemeWindow(player);
		}
		else if (currentCommand.startsWith("clearscheme"))
		{
			try
			{
				final String schemeName = st.nextToken();
				final Map<String, ArrayList<Integer>> schemes = BufferTable.getInstance().getPlayerSchemes(player.getObjectId());
				
				if (schemes != null && schemes.containsKey(schemeName))
					schemes.get(schemeName).clear();
			}
			catch (Exception e)
			{
				player.sendMessage("This scheme name is invalid.");
			}
			showManageSchemeWindow(player);
		}
		
		super.onBypassFeedback(player, command);
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String filename = "";
		if (val == 0)
			filename = "" + npcId;
		else
			filename = npcId + "-" + val;
		
		return "data/html/mods/buffer/" + filename + ".htm";
	}
	
	/**
	 * Sends an html packet to player with Give Buffs menu info for player and pet, depending on targetType parameter {player, pet}
	 * @param player : The player to make checks on.
	 * @param targetType : a String used to define if the player or his pet must be used as target.
	 */
	private void showGiveBuffsWindow(L2PcInstance player, String targetType)
	{
		final StringBuilder sb = new StringBuilder(200);
		
		final Map<String, ArrayList<Integer>> schemes = BufferTable.getInstance().getPlayerSchemes(player.getObjectId());
		if (schemes == null || schemes.isEmpty())
			sb.append("<font color=\"LEVEL\">You haven't defined any scheme, please go to 'Manage my schemes' and create at least one valid scheme.</font>");
		else
		{
			for (Map.Entry<String, ArrayList<Integer>> scheme : schemes.entrySet())
			{
				final int cost = getFee(scheme.getValue());
				StringUtil.append(sb, "<font color=\"LEVEL\"><a action=\"bypass -h npc_%objectId%_givebuffs ", targetType, " ", scheme.getKey(), " ", cost, "\">", scheme.getKey(), " (", scheme.getValue().size(), " skill(s))</a>", ((cost > 0) ? " - Adena cost: " + cost : ""), "</font><br1>");
			}
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(getHtmlPath(getNpcId(), 1));
		html.replace("%schemes%", sb.toString());
		html.replace("%targettype%", (targetType.equalsIgnoreCase("pet") ? "&nbsp;<a action=\"bypass -h npc_%objectId%_support player\">yourself</a>&nbsp;|&nbsp;your pet" : "yourself&nbsp;|&nbsp;<a action=\"bypass -h npc_%objectId%_support pet\">your pet</a>"));
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}
	
	/**
	 * Sends an html packet to player with Manage scheme menu info. This allows player to create/delete/clear schemes
	 * @param player : The player to make checks on.
	 */
	private void showManageSchemeWindow(L2PcInstance player)
	{
		final StringBuilder sb = new StringBuilder(200);
		
		final Map<String, ArrayList<Integer>> schemes = BufferTable.getInstance().getPlayerSchemes(player.getObjectId());
		if (schemes == null || schemes.isEmpty())
			sb.append("<font color=\"LEVEL\">You haven't created any scheme.</font>");
		else
		{
			sb.append("<table>");
			for (Map.Entry<String, ArrayList<Integer>> scheme : schemes.entrySet())
				StringUtil.append(sb, "<tr><td width=140>", scheme.getKey(), " (", scheme.getValue().size(), " skill(s))</td><td width=60><button value=\"Clear\" action=\"bypass -h npc_%objectId%_clearscheme ", scheme.getKey(), "\" width=55 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td width=60><button value=\"Drop\" action=\"bypass -h npc_%objectId%_deletescheme ", scheme.getKey(), "\" width=55 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
			
			sb.append("</table>");
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(getHtmlPath(getNpcId(), 2));
		html.replace("%schemes%", sb.toString());
		html.replace("%max_schemes%", Config.BUFFER_MAX_SCHEMES);
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}
	
	/**
	 * This sends an html packet to player with Edit Scheme Menu info. This allows player to edit each created scheme (add/delete skills)
	 * @param player : The player to make checks on.
	 * @param groupType : The group of skills to select.
	 * @param schemeName : The scheme to make check.
	 */
	private void showEditSchemeWindow(L2PcInstance player, String groupType, String schemeName)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		
		if (schemeName.equalsIgnoreCase("none"))
			html.setFile(getHtmlPath(getNpcId(), 3));
		else
		{
			if (groupType.equalsIgnoreCase("none"))
				html.setFile(getHtmlPath(getNpcId(), 4));
			else
			{
				html.setFile(getHtmlPath(getNpcId(), 5));
				html.replace("%skilllistframe%", getGroupSkillList(player, groupType, schemeName));
			}
			html.replace("%schemename%", schemeName);
			html.replace("%myschemeframe%", getPlayerSchemeSkillList(player, groupType, schemeName));
			html.replace("%typesframe%", getTypesFrame(groupType, schemeName));
		}
		html.replace("%schemes%", getPlayerSchemes(player, schemeName));
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}
	
	/**
	 * @param player : The player to make checks on.
	 * @param schemeName : The name to don't link (previously clicked).
	 * @return a String listing player's schemes. The scheme currently on selection isn't linkable.
	 */
	private static String getPlayerSchemes(L2PcInstance player, String schemeName)
	{
		final Map<String, ArrayList<Integer>> schemes = BufferTable.getInstance().getPlayerSchemes(player.getObjectId());
		if (schemes == null || schemes.isEmpty())
			return "Please create at least one scheme.";
		
		final StringBuilder sb = new StringBuilder(200);
		sb.append("<table>");
		
		for (Map.Entry<String, ArrayList<Integer>> scheme : schemes.entrySet())
		{
			if (schemeName.equalsIgnoreCase(scheme.getKey()))
				StringUtil.append(sb, "<tr><td width=200>", scheme.getKey(), " (<font color=\"LEVEL\">", scheme.getValue().size(), "</font> / ", Config.BUFFER_MAX_SKILLS, " skill(s))</td></tr>");
			else
				StringUtil.append(sb, "<tr><td width=200><a action=\"bypass -h npc_%objectId%_editschemes none ", scheme.getKey(), "\">", scheme.getKey(), " (", scheme.getValue().size(), " / ", Config.BUFFER_MAX_SKILLS, " skill(s))</a></td></tr>");
		}
		
		sb.append("</table>");
		
		return sb.toString();
	}
	
	/**
	 * @param player : The player to make checks on.
	 * @param groupType : The group of skills to select.
	 * @param schemeName : The scheme to make check.
	 * @return a String representing skills available to selection for a given groupType.
	 */
	private static String getGroupSkillList(L2PcInstance player, String groupType, String schemeName)
	{
		final List<Integer> skills = new ArrayList<>();
		for (int skillId : BufferTable.getSkillsIdsByType(groupType))
		{
			if (BufferTable.getInstance().getSchemeContainsSkill(player.getObjectId(), schemeName, skillId))
				continue;
			
			skills.add(skillId);
		}
		
		if (skills.isEmpty())
			return "That group doesn't contain any skills.";
		
		final StringBuilder sb = new StringBuilder(500);
		
		sb.append("<table>");
		int count = 0;
		for (int skillId : skills)
		{
			if (BufferTable.getInstance().getSchemeContainsSkill(player.getObjectId(), schemeName, skillId))
				continue;
			
			if (count == 0)
				sb.append("<tr>");
			
			if (skillId < 100)
				StringUtil.append(sb, "<td><button action=\"bypass -h npc_%objectId%_skillselect ", groupType, " ", schemeName, " ", skillId, "\" width=32 height=32 back=\"icon.skill00", skillId, "\" fore=\"icon.skill00", skillId, "\"></td>");
			else if (skillId < 1000)
				StringUtil.append(sb, "<td><button action=\"bypass -h npc_%objectId%_skillselect ", groupType, " ", schemeName, " ", skillId, "\" width=32 height=32 back=\"icon.skill0", skillId, "\" fore=\"icon.skill0", skillId, "\"></td>");
			else
				StringUtil.append(sb, "<td><button action=\"bypass -h npc_%objectId%_skillselect ", groupType, " ", schemeName, " ", skillId, "\" width=32 height=32 back=\"icon.skill", skillId, "\" fore=\"icon.skill", skillId, "\"></td>");
			
			count++;
			if (count == 6)
			{
				sb.append("</tr>");
				count = 0;
			}
		}
		
		if (!sb.toString().endsWith("</tr>"))
			sb.append("</tr>");
		
		sb.append("</table>");
		
		return sb.toString();
	}
	
	/**
	 * @param player : The player to make checks on.
	 * @param groupType : The group of skills to select.
	 * @param schemeName : The scheme to make check.
	 * @return a String representing a given scheme's content.
	 */
	private static String getPlayerSchemeSkillList(L2PcInstance player, String groupType, String schemeName)
	{
		final List<Integer> skills = BufferTable.getInstance().getScheme(player.getObjectId(), schemeName);
		if (skills.isEmpty())
			return "That scheme is empty.";
		
		final StringBuilder sb = new StringBuilder(500);
		sb.append("<table>");
		int count = 0;
		
		for (int sk : skills)
		{
			if (count == 0)
				sb.append("<tr>");
			
			if (sk < 100)
				StringUtil.append(sb, "<td><button action=\"bypass -h npc_%objectId%_skillunselect ", groupType, " ", schemeName, " ", sk, "\" width=32 height=32 back=\"icon.skill00", sk, "\" fore=\"icon.skill00", sk, "\"></td>");
			else if (sk < 1000)
				StringUtil.append(sb, "<td><button action=\"bypass -h npc_%objectId%_skillunselect ", groupType, " ", schemeName, " ", sk, "\" width=32 height=32 back=\"icon.skill0", sk, "\" fore=\"icon.skill0", sk, "\"></td>");
			else
				StringUtil.append(sb, "<td><button action=\"bypass -h npc_%objectId%_skillunselect ", groupType, " ", schemeName, " ", sk, "\" width=32 height=32 back=\"icon.skill", sk, "\" fore=\"icon.skill", sk, "\"></td>");
			
			count++;
			if (count == 6)
			{
				sb.append("</tr>");
				count = 0;
			}
		}
		
		if (!sb.toString().endsWith("<tr>"))
			sb.append("<tr>");
		
		sb.append("</table>");
		
		return sb.toString();
	}
	
	/**
	 * @param groupType : The group of skills to select.
	 * @param schemeName : The scheme to make check.
	 * @return a string representing all groupTypes availables. The group currently on selection isn't linkable.
	 */
	private static String getTypesFrame(String groupType, String schemeName)
	{
		final StringBuilder sb = new StringBuilder(500);
		sb.append("<table>");
		
		int count = 0;
		for (String s : BufferTable.getSkillTypes())
		{
			if (count == 0)
				sb.append("<tr>");
			
			if (groupType.equalsIgnoreCase(s))
				StringUtil.append(sb, "<td width=65>", s, "</td>");
			else
				StringUtil.append(sb, "<td width=65><a action=\"bypass -h npc_%objectId%_editschemes ", s, " ", schemeName, "\">", s, "</a></td>");
			
			count++;
			if (count == 4)
			{
				sb.append("</tr>");
				count = 0;
			}
		}
		
		if (!sb.toString().endsWith("</tr>"))
			sb.append("</tr>");
		
		sb.append("</table>");
		
		return sb.toString();
	}
	
	/**
	 * @param list : A list of skill ids.
	 * @return a global fee for all skills contained in list.
	 */
	private static int getFee(ArrayList<Integer> list)
	{
		if (Config.BUFFER_STATIC_BUFF_COST >= 0)
			return (list.size() * Config.BUFFER_STATIC_BUFF_COST);
		
		int fee = 0;
		for (int sk : list)
			fee += Config.BUFFER_BUFFLIST.get(sk).getValue();
		
		return fee;
	}
}