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
package net.sf.l2j.gameserver.scripting.scripts.ai.individual;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.l2j.Config;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.model.location.HeadedLocation;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2GrandBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.type.L2BossZone;
import net.sf.l2j.gameserver.network.client.game_to_client.MagicSkillUse;
import net.sf.l2j.gameserver.network.client.game_to_client.SocialAction;
import net.sf.l2j.gameserver.network.client.game_to_client.SpecialCamera;
import net.sf.l2j.gameserver.scripting.scripts.ai.AbstractNpcAI;
import net.sf.l2j.gameserver.templates.StatsSet;

public class Sailren extends AbstractNpcAI
{
	private static final L2BossZone SAILREN_LAIR = GrandBossManager.getInstance().getZoneById(110015);
	
	public static final int SAILREN = 29065;
	
	public static final byte DORMANT = 0; // No one has entered yet. Entry is unlocked.
	public static final byte FIGHTING = 1; // A group entered in the nest. Entry is locked.
	public static final byte DEAD = 2; // Sailren has been killed. Entry is locked.
	
	private static final int VELOCIRAPTOR = 22223;
	private static final int PTEROSAUR = 22199;
	private static final int TREX = 22217;
	
	private static final int DUMMY = 32110;
	private static final int CUBE = 32107;
	
	private static final long INTERVAL_CHECK = 600000L; // 10 minutes
	
	private static final HeadedLocation SAILREN_LOC = new HeadedLocation(27549, -6638, -2008, 0);
	
	private final List<L2Npc> _mobs = new CopyOnWriteArrayList<>();
	private static long _lastAttackTime = 0;
	
	public Sailren()
	{
		super("ai/individual");
		
		addAttackId(VELOCIRAPTOR, PTEROSAUR, TREX, SAILREN);
		addKillId(VELOCIRAPTOR, PTEROSAUR, TREX, SAILREN);
		
		final StatsSet info = GrandBossManager.getInstance().getStatsSet(SAILREN);
		
		switch (GrandBossManager.getInstance().getBossStatus(SAILREN))
		{
			case DEAD: // Launch the timer to set DORMANT, or set DORMANT directly if timer expired while offline.
				final long temp = (info.getLong("respawn_time") - System.currentTimeMillis());
				if (temp > 0)
					startQuestTimer("unlock", temp, null, null, false);
				else
					GrandBossManager.getInstance().setBossStatus(SAILREN, DORMANT);
				break;
			
			case FIGHTING:
				final int loc_x = info.getInteger("loc_x");
				final int loc_y = info.getInteger("loc_y");
				final int loc_z = info.getInteger("loc_z");
				final int heading = info.getInteger("heading");
				final int hp = info.getInteger("currentHP");
				final int mp = info.getInteger("currentMP");
				
				final L2Npc sailren = addSpawn(SAILREN, loc_x, loc_y, loc_z, heading, false, 0, false);
				GrandBossManager.getInstance().addBoss((L2GrandBossInstance) sailren);
				_mobs.add(sailren);
				
				sailren.setCurrentHpMp(hp, mp);
				sailren.setRunning();
				
				// Don't need to edit _timeTracker, as it's initialized to 0.
				startQuestTimer("inactivity", INTERVAL_CHECK, null, null, true);
				break;
		}
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		if (event.equalsIgnoreCase("beginning"))
		{
			_lastAttackTime = 0;
			
			for (int i = 0; i < 3; i++)
			{
				final L2Npc temp = addSpawn(VELOCIRAPTOR, SAILREN_LOC, true, 0, false);
				temp.getAI().setIntention(EIntention.ACTIVE);
				temp.setRunning();
				_mobs.add(temp);
			}
			startQuestTimer("inactivity", INTERVAL_CHECK, null, null, true);
		}
		else if (event.equalsIgnoreCase("spawn"))
		{
			// Dummy spawn used to cast the skill. Despawned after 26sec.
			final L2Npc temp = addSpawn(DUMMY, SAILREN_LOC, false, 26000, false);
			
			// Cast skill every 2,5sec.
			SAILREN_LAIR.broadcastPacket(new MagicSkillUse(npc, npc, 5090, 1, 2500, 0));
			startQuestTimer("skill", 2500, temp, null, true);
			
			// Cinematic, meanwhile.
			SAILREN_LAIR.broadcastPacket(new SpecialCamera(temp.getObjectId(), 60, 110, 30, 4000, 4000, 0, 65, 1, 0)); // 4sec
			
			startQuestTimer("camera_0", 3900, temp, null, false); // 3sec
			startQuestTimer("camera_1", 6800, temp, null, false); // 3sec
			startQuestTimer("camera_2", 9700, temp, null, false); // 3sec
			startQuestTimer("camera_3", 12600, temp, null, false); // 3sec
			startQuestTimer("camera_4", 15500, temp, null, false); // 3sec
			startQuestTimer("camera_5", 18400, temp, null, false); // 7sec
		}
		else if (event.equalsIgnoreCase("skill"))
			SAILREN_LAIR.broadcastPacket(new MagicSkillUse(npc, npc, 5090, 1, 2500, 0));
		else if (event.equalsIgnoreCase("camera_0"))
			SAILREN_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 100, 180, 30, 3000, 3000, 0, 50, 1, 0));
		else if (event.equalsIgnoreCase("camera_1"))
			SAILREN_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 150, 270, 25, 3000, 3000, 0, 30, 1, 0));
		else if (event.equalsIgnoreCase("camera_2"))
			SAILREN_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 160, 360, 20, 3000, 3000, 10, 15, 1, 0));
		else if (event.equalsIgnoreCase("camera_3"))
			SAILREN_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 160, 450, 10, 3000, 3000, 0, 10, 1, 0));
		else if (event.equalsIgnoreCase("camera_4"))
		{
			SAILREN_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 160, 560, 0, 3000, 3000, 0, 10, 1, 0));
			
			final L2Npc temp = addSpawn(SAILREN, SAILREN_LOC, false, 0, false);
			GrandBossManager.getInstance().addBoss((L2GrandBossInstance) temp);
			_mobs.add(temp);
			
			// Stop skill task.
			cancelQuestTimers("skill");
			SAILREN_LAIR.broadcastPacket(new MagicSkillUse(npc, npc, 5091, 1, 2500, 0));
			
			temp.broadcastPacket(new SocialAction(temp, 2));
		}
		else if (event.equalsIgnoreCase("camera_5"))
			SAILREN_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 70, 560, 0, 500, 7000, -15, 10, 1, 0));
		else if (event.equalsIgnoreCase("unlock"))
			GrandBossManager.getInstance().setBossStatus(SAILREN, DORMANT);
		else if (event.equalsIgnoreCase("inactivity"))
		{
			// 10 minutes without any attack activity leads to a reset.
			if ((System.currentTimeMillis() - _lastAttackTime) >= INTERVAL_CHECK)
			{
				// Set it dormant.
				GrandBossManager.getInstance().setBossStatus(SAILREN, DORMANT);
				
				// Delete all monsters and clean the list.
				if (!_mobs.isEmpty())
				{
					for (L2Npc mob : _mobs)
						mob.deleteMe();
					
					_mobs.clear();
				}
				
				// Oust all players from area.
				SAILREN_LAIR.oustAllPlayers();
				
				// Cancel inactivity task.
				cancelQuestTimers("inactivity");
			}
		}
		else if (event.equalsIgnoreCase("oust"))
		{
			// Oust all players from area.
			SAILREN_LAIR.oustAllPlayers();
		}
		
		return null;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		if (!_mobs.contains(npc) || !SAILREN_LAIR.getAllowedPlayers().contains(killer.getObjectId()))
			return null;
		
		switch (npc.getNpcId())
		{
			case VELOCIRAPTOR:
				// Once the 3 Velociraptors are dead, spawn a Pterosaur.
				if (_mobs.remove(npc) && _mobs.isEmpty())
				{
					final L2Npc temp = addSpawn(PTEROSAUR, SAILREN_LOC, false, 0, false);
					temp.setRunning();
					temp.getAI().setIntention(EIntention.ATTACK, killer);
					_mobs.add(temp);
				}
				break;
			
			case PTEROSAUR:
				// Pterosaur is dead, spawn a Trex.
				if (_mobs.remove(npc))
				{
					final L2Npc temp = addSpawn(TREX, SAILREN_LOC, false, 0, false);
					temp.setRunning();
					temp.getAI().setIntention(EIntention.ATTACK, killer);
					temp.broadcastNpcSay("?");
					_mobs.add(temp);
				}
				break;
			
			case TREX:
				// Trex is dead, wait 5min and spawn Sailren.
				if (_mobs.remove(npc))
					startQuestTimer("spawn", Config.WAIT_TIME_SAILREN, npc, killer, false);
				break;
			
			case SAILREN:
				if (_mobs.remove(npc))
				{
					// Set Sailren as dead.
					GrandBossManager.getInstance().setBossStatus(SAILREN, DEAD);
					
					// Spawn the Teleport Cube for 10min.
					addSpawn(CUBE, npc, false, INTERVAL_CHECK, false);
					
					// Cancel inactivity task.
					cancelQuestTimers("inactivity");
					
					long respawnTime = (long) Config.SPAWN_INTERVAL_SAILREN + Rnd.get(-Config.RANDOM_SPAWN_TIME_SAILREN, Config.RANDOM_SPAWN_TIME_SAILREN);
					respawnTime *= 3600000;
					
					startQuestTimer("oust", INTERVAL_CHECK, null, null, false);
					startQuestTimer("unlock", respawnTime, null, null, false);
					
					// Save the respawn time so that the info is maintained past reboots.
					final StatsSet info = GrandBossManager.getInstance().getStatsSet(SAILREN);
					info.set("respawn_time", System.currentTimeMillis() + respawnTime);
					GrandBossManager.getInstance().setStatsSet(SAILREN, info);
				}
				break;
		}
		
		return null;
	}
	
	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		if (!_mobs.contains(npc) || !SAILREN_LAIR.getAllowedPlayers().contains(attacker.getObjectId()))
			return null;
		
		// Actualize _timeTracker.
		_lastAttackTime = System.currentTimeMillis();
		
		return null;
	}
}