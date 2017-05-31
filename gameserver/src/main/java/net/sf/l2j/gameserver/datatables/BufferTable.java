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
package net.sf.l2j.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.model.holder.BuffSkillHolder;

/**
 * This class stores players' buff schemes into _schemesTable.
 * @author House, Tryskell
 */
public class BufferTable
{
	private static final Logger _log = Logger.getLogger(BufferTable.class.getName());
	
	private static final Map<Integer, HashMap<String, ArrayList<Integer>>> _schemesTable = new ConcurrentHashMap<>();
	
	private static final String LOAD_SCHEMES = "SELECT * FROM buffer_schemes";
	private static final String DELETE_SCHEMES = "TRUNCATE TABLE buffer_schemes";
	private static final String INSERT_SCHEME = "INSERT INTO buffer_schemes (object_id, scheme_name, skills) VALUES (?,?,?)";
	
	public static BufferTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public BufferTable()
	{
		int count = 0;
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement st = con.prepareStatement(LOAD_SCHEMES);
			ResultSet rs = st.executeQuery();
			
			while (rs.next())
			{
				final int objectId = rs.getInt("object_id");
				
				final String schemeName = rs.getString("scheme_name");
				final String[] skills = rs.getString("skills").split(",");
				
				ArrayList<Integer> schemeList = new ArrayList<>();
				for (String skill : skills)
				{
					// Don't feed the skills list if the config is reached, or if the list is empty.
					if (skill.isEmpty() || schemeList.size() >= Config.BUFFER_MAX_SKILLS)
						break;
					
					schemeList.add(Integer.valueOf(skill));
				}
				setScheme(objectId, schemeName, schemeList);
				count++;
			}
			
			rs.close();
			st.close();
		}
		catch (Exception e)
		{
			_log.warning("BufferTable: Failed to load buff schemes : " + e);
		}
		_log.info("BufferTable: Loaded " + count + " players schemes.");
	}
	
	public void saveSchemes()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			// Delete all entries from database.
			PreparedStatement st = con.prepareStatement(DELETE_SCHEMES);
			st.execute();
			st.close();
			
			st = con.prepareStatement(INSERT_SCHEME);
			
			// Save _schemesTable content.
			for (Map.Entry<Integer, HashMap<String, ArrayList<Integer>>> player : _schemesTable.entrySet())
			{
				for (Map.Entry<String, ArrayList<Integer>> scheme : player.getValue().entrySet())
				{
					// Build a String composed of skill ids seperated by a ",".
					final StringBuilder sb = new StringBuilder();
					for (int skillId : scheme.getValue())
						StringUtil.append(sb, skillId, ",");
					
					// Delete the last "," : must be called only if there is something to delete !
					if (sb.length() > 0)
						sb.setLength(sb.length() - 1);
					
					st.setInt(1, player.getKey());
					st.setString(2, scheme.getKey());
					st.setString(3, sb.toString());
					st.executeUpdate();
					st.clearParameters();
				}
			}
			st.close();
		}
		catch (Exception e)
		{
			_log.warning("BufferTable: Error while saving schemes : " + e);
		}
	}
	
	public void setScheme(int playerId, String schemeName, ArrayList<Integer> list)
	{
		if (!_schemesTable.containsKey(playerId))
			_schemesTable.put(playerId, new HashMap<String, ArrayList<Integer>>());
		else if (_schemesTable.get(playerId).size() >= Config.BUFFER_MAX_SCHEMES)
			return;
		
		_schemesTable.get(playerId).put(schemeName, list);
	}
	
	/**
	 * @param playerId : The player objectId to check.
	 * @return the list of schemes for a given player.
	 */
	public Map<String, ArrayList<Integer>> getPlayerSchemes(int playerId)
	{
		return _schemesTable.get(playerId);
	}
	
	/**
	 * @param playerId : The player objectId to check.
	 * @param schemeName : The scheme name to check.
	 * @return the List holding skills for the given scheme name and player, or null (if scheme or player isn't registered).
	 */
	public List<Integer> getScheme(int playerId, String schemeName)
	{
		if (_schemesTable.get(playerId) == null || _schemesTable.get(playerId).get(schemeName) == null)
			return Collections.emptyList();
		
		return _schemesTable.get(playerId).get(schemeName);
	}
	
	/**
	 * @param playerId : The player objectId to check.
	 * @param schemeName : The scheme name to check.
	 * @param skillId : The skill id to check.
	 * @return true if the skill is already registered on the scheme, or false otherwise.
	 */
	public boolean getSchemeContainsSkill(int playerId, String schemeName, int skillId)
	{
		final List<Integer> skills = getScheme(playerId, schemeName);
		if (skills.isEmpty())
			return false;
		
		for (int id : skills)
		{
			if (id == skillId)
				return true;
		}
		return false;
	}
	
	/**
	 * @param groupType : The type of skills to return.
	 * @return a list of skills ids based on the given groupType.
	 */
	public static List<Integer> getSkillsIdsByType(String groupType)
	{
		List<Integer> skills = new ArrayList<>();
		for (BuffSkillHolder skill : Config.BUFFER_BUFFLIST.values())
		{
			if (skill.getType().equalsIgnoreCase(groupType))
				skills.add(skill.getId());
		}
		return skills;
	}
	
	/**
	 * @return a list of all buff types available.
	 */
	public static List<String> getSkillTypes()
	{
		List<String> skillTypes = new ArrayList<>();
		for (BuffSkillHolder skill : Config.BUFFER_BUFFLIST.values())
		{
			if (!skillTypes.contains(skill.getType()))
				skillTypes.add(skill.getType());
		}
		return skillTypes;
	}
	
	private static class SingletonHolder
	{
		protected static final BufferTable _instance = new BufferTable();
	}
}