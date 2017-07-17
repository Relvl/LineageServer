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
package net.sf.l2j.gameserver.model;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.geoengine.GeoData;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;

/**
 * This class manages the spawn and respawn of a group of L2Npc that are in the same are and have the same type. <B><U> Concept</U> :</B><BR>
 * <BR>
 * L2Npc can be spawned either in a random position into a location area (if Lox=0 and Locy=0), either at an exact position. The heading of the L2Npc can be a random heading if not defined (value= -1) or an exact heading (ex : merchant...).<BR>
 * <BR>
 * @author Nightmare
 */
public class L2Spawn
{
	protected static final Logger _log = Logger.getLogger(L2Spawn.class.getName());
	
	/** The link on the L2NpcTemplate object containing generic and static properties of this spawn (ex : RewardExp, RewardSP, AggroRange...) */
	private NpcTemplate _template;
	
	/** The X position of the spawn point */
	private int _locX;
	
	/** The Y position of the spawn point */
	private int _locY;
	
	/** The Z position of the spawn point */
	private int _locZ;
	
	/** The heading of L2Npc when they are spawned */
	private int _heading;
	
	/** The fixed delay used for a L2Npc respawn */
	private int _respawnDelay;
	
	/** The random delay used for a L2Npc respawn */
	private int _randomRespawnDelay;
	
	/** Minimum delay RaidBoss */
	private int _respawnMinDelay;
	
	/** Maximum delay RaidBoss */
	private int _respawnMaxDelay;
	
	/** The generic constructor of L2Npc managed by this L2Spawn */
	private Constructor<?> _constructor;
	
	/** If True a L2Npc is respawned each time that another is killed */
	private boolean _doRespawn;
	
	private L2Npc _lastSpawn;
	private static List<SpawnListener> _spawnListeners = new ArrayList<>();
	
	/** The task launching the function doSpawn() */
	class SpawnTask implements Runnable
	{
		private final L2Npc _oldNpc;
		
		public SpawnTask(L2Npc pOldNpc)
		{
			_oldNpc = pOldNpc;
		}
		
		@Override
		public void run()
		{
			respawnNpc(_oldNpc);
		}
	}
	
	/**
	 * Constructor of L2Spawn.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * Each L2Spawn owns generic and static properties (ex : RewardExp, RewardSP, AggroRange...). All of those properties are stored in a different L2NpcTemplate for each type of L2Spawn. Each template is loaded once in the server cache memory (reduce memory use). When a new instance of L2Spawn is
	 * created, server just create a link between the instance and the template. This link is stored in <B>_template</B><BR>
	 * <BR>
	 * Each L2Npc is linked to a L2Spawn that manages its spawn and respawn (delay, location...). This link is stored in <B>_spawn</B> of the L2Npc<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Set the _template of the L2Spawn</li> <li>Calculate the implementationName used to generate the generic constructor of L2Npc managed by this L2Spawn</li> <li>Create the generic constructor of L2Npc managed by this L2Spawn</li><BR>
	 * <BR>
	 * @param mobTemplate The L2NpcTemplate to link to this L2Spawn
	 * @throws SecurityException
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 */
	public L2Spawn(NpcTemplate mobTemplate) throws SecurityException, ClassNotFoundException, NoSuchMethodException
	{
		// Set the _template of the L2Spawn
		_template = mobTemplate;
		if (_template == null)
			return;
		
		// Create the generic constructor of L2Npc managed by this L2Spawn
		Class<?>[] parameters =
		{
			int.class,
			Class.forName("net.sf.l2j.gameserver.model.actor.template.NpcTemplate")
		};
		_constructor = Class.forName("net.sf.l2j.gameserver.model.actor.instance." + _template.getType() + "Instance").getConstructor(parameters);
	}
	
	/**
	 * @return the maximum number of L2Npc that this L2Spawn can manage.
	 */
	public int getAmount()
	{
		return 1;
	}
	
	/**
	 * @return the X position of the spawn point.
	 */
	public int getLocx()
	{
		return _locX;
	}
	
	/**
	 * @return the Y position of the spawn point.
	 */
	public int getLocy()
	{
		return _locY;
	}
	
	/**
	 * @return the Z position of the spawn point.
	 */
	public int getLocz()
	{
		return _locZ;
	}
	
	/**
	 * @return the Itdentifier of the L2Npc manage by this L2spawn contained in the L2NpcTemplate.
	 */
	public int getNpcId()
	{
		return _template.getNpcId();
	}
	
	/**
	 * @return the heading of L2Npc when they are spawned.
	 */
	public int getHeading()
	{
		return _heading;
	}
	
	/**
	 * @return the fixed delay used for a L2Npc respawn.
	 */
	public int getRespawnDelay()
	{
		return _respawnDelay;
	}
	
	/**
	 * @return the random delay used for a L2Npc respawn.
	 */
	public int getRandomRespawnDelay()
	{
		return _randomRespawnDelay;
	}
	
	/**
	 * @return the minimum RaidBoss spawn delay.
	 */
	public int getRespawnMinDelay()
	{
		return _respawnMinDelay;
	}
	
	/**
	 * @return the maximum RaidBoss spawn delay.
	 */
	public int getRespawnMaxDelay()
	{
		return _respawnMaxDelay;
	}
	
	/**
	 * Set the minimum respawn delay.
	 * @param date
	 */
	public void setRespawnMinDelay(int date)
	{
		_respawnMinDelay = date;
	}
	
	/**
	 * Set Maximum respawn delay.
	 * @param date
	 */
	public void setRespawnMaxDelay(int date)
	{
		_respawnMaxDelay = date;
	}
	
	/**
	 * Set the X position of the spawn point.
	 * @param locx
	 */
	public void setLocx(int locx)
	{
		_locX = locx;
	}
	
	/**
	 * Set the Y position of the spawn point.
	 * @param locy
	 */
	public void setLocy(int locy)
	{
		_locY = locy;
	}
	
	/**
	 * Set the Z position of the spawn point.
	 * @param locz
	 */
	public void setLocz(int locz)
	{
		_locZ = locz;
	}
	
	/**
	 * Set the heading of L2Npc when they are spawned.
	 * @param heading
	 */
	public void setHeading(int heading)
	{
		_heading = heading;
	}
	
	/**
	 * Create a SpawnTask to launch after the fixed + random delay. A respawn is ONLY possible if _doRespawn=True.<br>
	 * <br>
	 * The random timer is calculated here, in order each spawn got his own random timer. The * 1000 is made after Rnd.get because we don't need random ms, but seconds.
	 * @param oldNpc
	 */
	public void respawn(L2Npc oldNpc)
	{
		// Check if respawn is possible to prevent multiple respawning caused by lag
		if (_doRespawn)
		{
			// Calculate the random time, if any.
			final int randomTime = (_randomRespawnDelay > 0) ? Rnd.get(-_randomRespawnDelay, _randomRespawnDelay) * 1000 : 0;
			
			// Create a new SpawnTask to launch after the respawn Delay
			ThreadPoolManager.getInstance().scheduleGeneral(new SpawnTask(oldNpc), _respawnDelay + randomTime);
		}
	}
	
	/**
	 * Create the initial spawning and set _doRespawn to True.
	 */
	public void init()
	{
		doSpawn();
		_doRespawn = true;
	}
	
	/**
	 * @return true if respawn is enabled.
	 */
	public boolean isRespawnEnabled()
	{
		return _doRespawn;
	}
	
	/**
	 * Set _doRespawn to False to stop respawn for this L2Spawn.
	 */
	public void stopRespawn()
	{
		_doRespawn = false;
	}
	
	/**
	 * Set _doRespawn to True to start or restart respawn for this L2Spawn.
	 */
	public void startRespawn()
	{
		_doRespawn = true;
	}
	
	/**
	 * Create the L2Npc, add it to the world and lauch its OnSpawn action.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * L2Npc can be spawned either in a random position into a location area (if Lox=0 and Locy=0), either at an exact position. The heading of the L2Npc can be a random heading if not defined (value= -1) or an exact heading (ex : merchant...).<BR>
	 * <BR>
	 * <B><U> Actions for an random spawn into location area</U> : <I>(if Locx=0 and Locy=0)</I></B><BR>
	 * <BR>
	 * <li>Get L2Npc Init parameters and its generate an Identifier</li> <li>Call the constructor of the L2Npc</li> <li>Calculate the random position in the location area (if Locx=0 and Locy=0) or get its exact position from the L2Spawn</li> <li>Set the position of the L2Npc</li> <li>Set the HP and
	 * MP of the L2Npc to the max</li> <li>Set the heading of the L2Npc (random heading if not defined : value=-1)</li> <li>Link the L2Npc to this L2Spawn</li> <li>Init other values of the L2Npc (ex : from its L2CharTemplate for INT, STR, DEX...) and add it in the world</li> <li>Lauch the action
	 * OnSpawn fo the L2Npc</li><BR>
	 * <BR>
	 * <li>Increase the current number of L2Npc managed by this L2Spawn</li><BR>
	 * <BR>
	 * @return the newly created instance.
	 */
	public L2Npc doSpawn()
	{
		return doSpawn(false);
	}
	
	public L2Npc doSpawn(boolean isSummonSpawn)
	{
		L2Npc mob = null;
		try
		{
			// Check if the L2Spawn is not a L2Pet or L2Minion
			if (_template.isType("L2Pet") || _template.isType("L2Minion"))
				return mob;
			
			// Get L2Npc Init parameters and its generate an Identifier
			Object[] parameters =
			{
				IdFactory.getInstance().getNextId(),
				_template
			};
			
			// Call the constructor of the L2Npc (can be a L2ArtefactInstance, L2FriendlyMobInstance, L2GuardInstance, L2MonsterInstance, L2SiegeGuardInstance, L2BoxInstance, L2FeedableBeastInstance, L2TamedBeastInstance, L2NpcInstance)
			Object tmp = _constructor.newInstance(parameters);
			
			if (isSummonSpawn && tmp instanceof L2Character)
				((L2Character) tmp).setShowSummonAnimation(isSummonSpawn);
			
			// Check if the Instance is a L2Npc
			if (!(tmp instanceof L2Npc))
				return mob;
			
			mob = (L2Npc) tmp;
			return initializeNpcInstance(mob);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "NPC " + _template.getNpcId() + " class not found", e);
		}
		return mob;
	}
	
	/**
	 * @param mob
	 * @return
	 */
	private L2Npc initializeNpcInstance(L2Npc mob)
	{
		int newlocx, newlocy, newlocz;
		
		// If Locx=0 and Locy=0, there's a problem.
		if (getLocx() == 0 && getLocy() == 0)
		{
			_log.warning("L2Spawn : the following npcID: " + _template.getNpcId() + " misses X/Y informations.");
			return mob;
		}
		
		// The L2Npc is spawned at the exact position (Lox, Locy, Locz)
		newlocx = getLocx();
		newlocy = getLocy();
		newlocz = GeoData.getInstance().getHeight(newlocx, newlocy, getLocz());
		
		// temporarily fix: when the spawn Z and geo Z differs more than 200, use spawn Z coord
		if (Math.abs(newlocz - getLocz()) > 200)
			newlocz = getLocz();
		
		mob.stopAllEffects();
		mob.setIsDead(false);
		
		// Reset decay info
		mob.setDecayed(false);
		
		// Set the HP and MP of the L2Npc to the max
		mob.setCurrentHpMp(mob.getMaxHp(), mob.getMaxMp());
		
		// Reset script value.
		mob.setScriptValue(0);
		
		// Set the heading of the L2Npc (random heading if not defined)
		if (getHeading() == -1)
			mob.setHeading(Rnd.get(61794));
		else
			mob.setHeading(getHeading());
		
		// Test champion state for next spawn.
		if (Config.CHAMPION_FREQUENCY > 0)
		{
			// It can't be a Raid, a Raid minion nor a minion. Quest mobs and chests are disabled too.
			if (mob instanceof L2MonsterInstance && !getTemplate().cantBeChampion() && mob.getLevel() >= Config.CHAMP_MIN_LVL && mob.getLevel() <= Config.CHAMP_MAX_LVL && !mob.isRaid() && !((L2MonsterInstance) mob).isRaidMinion() && !((L2MonsterInstance) mob).isMinion())
				((L2Attackable) mob).setChampion(Rnd.get(100) < Config.CHAMPION_FREQUENCY);
		}
		
		// Link the L2Npc to this L2Spawn
		mob.setSpawn(this);
		
		// Init other values of the L2Npc (ex : from its L2CharTemplate for INT, STR, DEX...) and add it in the world as a visible object
		mob.spawnMe(newlocx, newlocy, newlocz);
		
		L2Spawn.notifyNpcSpawned(mob);
		
		_lastSpawn = mob;
		
		return mob;
	}
	
	public static void addSpawnListener(SpawnListener listener)
	{
		synchronized (_spawnListeners)
		{
			_spawnListeners.add(listener);
		}
	}
	
	public static void removeSpawnListener(SpawnListener listener)
	{
		synchronized (_spawnListeners)
		{
			_spawnListeners.remove(listener);
		}
	}
	
	public static void notifyNpcSpawned(L2Npc npc)
	{
		synchronized (_spawnListeners)
		{
			for (SpawnListener listener : _spawnListeners)
				listener.npcSpawned(npc);
		}
	}
	
	/**
	 * Set the respawn delay. It can't be inferior to 0, and is automatically modified if inferior to 10 seconds.
	 * @param i delay in seconds
	 */
	public void setRespawnDelay(int i)
	{
		if (i < 10)
		{
			if (i < 0)
				_log.warning("Respawn delay is negative for spawnId: " + getNpcId());
			
			i = 10;
		}
		
		_respawnDelay = i * 1000;
	}
	
	public void setRandomRespawnDelay(int i)
	{
		_randomRespawnDelay = i;
	}
	
	public L2Npc getLastSpawn()
	{
		return _lastSpawn;
	}
	
	public void respawnNpc(L2Npc oldNpc)
	{
		if (_doRespawn)
		{
			oldNpc.refreshID();
			initializeNpcInstance(oldNpc);
		}
	}
	
	public NpcTemplate getTemplate()
	{
		return _template;
	}
	
	@Override
	public String toString()
	{
		return "L2Spawn [_template=" + getNpcId() + ", _locX=" + _locX + ", _locY=" + _locY + ", _locZ=" + _locZ + ", heading=" + _heading + "]";
	}
}