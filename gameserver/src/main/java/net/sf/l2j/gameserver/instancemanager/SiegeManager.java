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
package net.sf.l2j.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.config.ExProperties;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.TowerSpawn;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class SiegeManager
{
	private static final Logger _log = Logger.getLogger(SiegeManager.class.getName());
	
	public static final SiegeManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private final Map<Integer, List<TowerSpawn>> _controlTowers = new HashMap<>();
	private final Map<Integer, List<TowerSpawn>> _flameTowers = new HashMap<>();
	
	public static int MAX_ATTACKERS_NUMBER;
	public static int ATTACKERS_RESPAWN_DELAY;
	public static int MAX_DEFENDERS_NUMBER;
	public static int FLAGS_MAX_COUNT;
	public static int MINIMUM_CLAN_LEVEL;
	public static int SIEGE_LENGTH;
	
	protected SiegeManager()
	{
		ExProperties sieges = Config.load(Config.SIEGE_FILE);
		MAX_ATTACKERS_NUMBER = sieges.getProperty("AttackerMaxClans", 10);
		ATTACKERS_RESPAWN_DELAY = sieges.getProperty("AttackerRespawn", 10000);
		MAX_DEFENDERS_NUMBER = sieges.getProperty("DefenderMaxClans", 10);
		FLAGS_MAX_COUNT = sieges.getProperty("MaxFlags", 1);
		MINIMUM_CLAN_LEVEL = sieges.getProperty("SiegeClanMinLevel", 4);
		SIEGE_LENGTH = sieges.getProperty("SiegeLength", 120);
		
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			final List<TowerSpawn> controlTowers = new ArrayList<>();
			for (int i = 1; i < 0xFF; i++)
			{
				final String parameters = sieges.getProperty(castle.getName() + "ControlTower" + Integer.toString(i), "");
				if (parameters.isEmpty())
					break;
				
				final StringTokenizer st = new StringTokenizer(parameters.trim(), ",");
				
				try
				{
					final int x = Integer.parseInt(st.nextToken());
					final int y = Integer.parseInt(st.nextToken());
					final int z = Integer.parseInt(st.nextToken());
					final int npcId = Integer.parseInt(st.nextToken());
					
					controlTowers.add(new TowerSpawn(npcId, new Location(x, y, z)));
				}
				catch (Exception e)
				{
					_log.warning("Error while loading control tower(s) for " + castle.getName() + " castle.");
				}
			}
			
			final List<TowerSpawn> flameTowers = new ArrayList<>();
			for (int i = 1; i < 0xFF; i++)
			{
				final String parameters = sieges.getProperty(castle.getName() + "FlameTower" + Integer.toString(i), "");
				
				if (parameters.isEmpty())
					break;
				
				final StringTokenizer st = new StringTokenizer(parameters.trim(), ",");
				
				try
				{
					final int x = Integer.parseInt(st.nextToken());
					final int y = Integer.parseInt(st.nextToken());
					final int z = Integer.parseInt(st.nextToken());
					final int npcId = Integer.parseInt(st.nextToken());
					
					final List<Integer> zoneList = new ArrayList<>();
					
					while (st.hasMoreTokens())
						zoneList.add(Integer.parseInt(st.nextToken()));
					
					flameTowers.add(new TowerSpawn(npcId, new Location(x, y, z), zoneList));
				}
				catch (Exception e)
				{
					_log.warning("Error while loading flame tower(s) for " + castle.getName() + " castle.");
				}
			}
			
			_controlTowers.put(castle.getCastleId(), controlTowers);
			_flameTowers.put(castle.getCastleId(), flameTowers);
			
			loadTrapUpgrade(castle.getCastleId());
		}
		_log.info("SiegeManager: Loaded " + _controlTowers.size() + " Control Towers & " + _flameTowers.size() + " Flame Towers.");
	}
	
	/**
	 * That method verify if the player can summon a siege summon. Following checks are made :
	 * <UL>
	 * <LI>must be on a castle ground;</LI>
	 * <LI>during a siege period;</LI>
	 * <LI>must be an attacker;</LI>
	 * <LI>mustn't be inside a castle (siege zone, but not castle zone)</LI>
	 * </UL>
	 * @param activeChar The player who attempt to summon a siege summon.
	 * @return true if the player can summon, false otherwise (send an error message aswell).
	 */
	public static boolean checkIfOkToSummon(L2PcInstance activeChar)
	{
		if (activeChar == null)
			return false;
		
		Castle castle = CastleManager.getInstance().getCastle(activeChar);
		if ((castle == null || castle.getCastleId() <= 0) || (!castle.getSiege().isInProgress()) || (activeChar.getClanId() != 0 && castle.getSiege().getAttackerClan(activeChar.getClanId()) == null) || (activeChar.isInSiege() && activeChar.isInsideZone(ZoneId.CASTLE)))
		{
			activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_CALL_PET_FROM_THIS_LOCATION));
			return false;
		}
		
		return true;
	}
	
	/**
	 * Verify if the clan is registered to any siege.
	 * @param clan The L2Clan of the player
	 * @return true if the clan is registered or owner of a castle
	 */
	public static boolean checkIsRegistered(L2Clan clan)
	{
		if (clan == null || clan.hasCastle())
			return true;
		
		boolean register = false;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT clan_id FROM siege_clans WHERE clan_id=?");
			statement.setInt(1, clan.getClanId());
			ResultSet rs = statement.executeQuery();
			
			while (rs.next())
			{
				register = true;
				break;
			}
			
			rs.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Exception: checkIsRegistered(): " + e);
		}
		return register;
	}
	
	public static void addSiegeSkills(L2PcInstance character)
	{
		for (L2Skill sk : SkillTable.getInstance().getSiegeSkills(character.isNoble()))
			character.addSkill(sk, false);
	}
	
	public static void removeSiegeSkills(L2PcInstance character)
	{
		for (L2Skill sk : SkillTable.getInstance().getSiegeSkills(character.isNoble()))
			character.removeSkill(sk);
	}
	
	public List<TowerSpawn> getControlTowers(int castleId)
	{
		return _controlTowers.get(castleId);
	}
	
	public List<TowerSpawn> getFlameTowers(int castleId)
	{
		return _flameTowers.get(castleId);
	}
	
	public static Siege getSiege(L2Object activeObject)
	{
		return getSiege(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}
	
	public static Siege getSiege(int x, int y, int z)
	{
		for (Castle castle : CastleManager.getInstance().getCastles())
			if (castle.getSiege().checkIfInZone(x, y, z))
				return castle.getSiege();
		
		return null;
	}
	
	public static List<Siege> getSieges()
	{
		List<Siege> sieges = new ArrayList<>();
		for (Castle castle : CastleManager.getInstance().getCastles())
			sieges.add(castle.getSiege());
		
		return sieges;
	}
	
	private void loadTrapUpgrade(int castleId)
	{
		if (castleId <= 0)
			return;
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT * FROM castle_trapupgrade WHERE castleId=?");
			statement.setInt(1, castleId);
			ResultSet rs = statement.executeQuery();
			
			while (rs.next())
				_flameTowers.get(castleId).get(rs.getInt("towerIndex")).setUpgradeLevel(rs.getInt("level"));
			
			rs.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Exception: loadTrapUpgrade(): " + e);
		}
	}
	
	private static class SingletonHolder
	{
		protected static final SiegeManager _instance = new SiegeManager();
	}
}