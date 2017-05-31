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

import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.VehiclePathPoint;
import net.sf.l2j.gameserver.model.actor.instance.L2BoatInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.template.CharTemplate;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.templates.StatsSet;

public class BoatManager
{
	private final Map<Integer, L2BoatInstance> _boats = new HashMap<>();
	private final boolean[] _docksBusy = new boolean[3];
	
	public static final int TALKING_ISLAND = 0;
	public static final int GLUDIN_HARBOR = 1;
	public static final int RUNE_HARBOR = 2;
	
	public static final int BOAT_BROADCAST_RADIUS = 20000;
	
	public static final BoatManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected BoatManager()
	{
	}
	
	public L2BoatInstance getNewBoat(int boatId, int x, int y, int z, int heading)
	{
		StatsSet npcDat = new StatsSet();
		npcDat.set("id", boatId);
		npcDat.set("level", 0);
		
		npcDat.set("str", 0);
		npcDat.set("con", 0);
		npcDat.set("dex", 0);
		npcDat.set("int", 0);
		npcDat.set("wit", 0);
		npcDat.set("men", 0);
		
		npcDat.set("hp", 50000);
		npcDat.set("mp", 0);
		
		npcDat.set("hpRegen", 3.e-3f);
		npcDat.set("mpRegen", 3.e-3f);
		
		npcDat.set("radius", 0);
		npcDat.set("height", 0);
		npcDat.set("type", "");
		
		npcDat.set("exp", 0);
		npcDat.set("sp", 0);
		
		npcDat.set("pAtk", 0);
		npcDat.set("mAtk", 0);
		npcDat.set("pDef", 100);
		npcDat.set("mDef", 100);
		
		npcDat.set("rHand", 0);
		npcDat.set("lHand", 0);
		
		npcDat.set("walkSpd", 0);
		npcDat.set("runSpd", 0);
		
		CharTemplate template = new CharTemplate(npcDat);
		L2BoatInstance boat = new L2BoatInstance(IdFactory.getInstance().getNextId(), template);
		
		_boats.put(boat.getObjectId(), boat);
		
		boat.setHeading(heading);
		boat.setXYZInvisible(x, y, z);
		boat.spawnMe();
		
		return boat;
	}
	
	/**
	 * @param boatId
	 * @return
	 */
	public L2BoatInstance getBoat(int boatId)
	{
		return _boats.get(boatId);
	}
	
	/**
	 * Lock/unlock dock so only one ship can be docked
	 * @param h Dock Id
	 * @param value True if dock is locked
	 */
	public void dockShip(int h, boolean value)
	{
		_docksBusy[h] = value;
	}
	
	/**
	 * Check if dock is busy
	 * @param h Dock Id
	 * @return Trye if dock is locked
	 */
	public boolean dockBusy(int h)
	{
		return _docksBusy[h];
	}
	
	/**
	 * Broadcast one packet in both path points
	 * @param point1
	 * @param point2
	 * @param packet The packet to broadcast.
	 */
	public void broadcastPacket(VehiclePathPoint point1, VehiclePathPoint point2, L2GameServerPacket packet)
	{
		for (L2PcInstance player : L2World.getInstance().getPlayers())
		{
			double dx = (double) player.getX() - point1.x;
			double dy = (double) player.getY() - point1.y;
			
			if (Math.sqrt(dx * dx + dy * dy) < BOAT_BROADCAST_RADIUS)
				player.sendPacket(packet);
			else
			{
				dx = (double) player.getX() - point2.x;
				dy = (double) player.getY() - point2.y;
				
				if (Math.sqrt(dx * dx + dy * dy) < BOAT_BROADCAST_RADIUS)
					player.sendPacket(packet);
			}
		}
	}
	
	/**
	 * Broadcast several packets in both path points
	 * @param point1
	 * @param point2
	 * @param packets The packets to broadcast.
	 */
	public void broadcastPackets(VehiclePathPoint point1, VehiclePathPoint point2, L2GameServerPacket... packets)
	{
		for (L2PcInstance player : L2World.getInstance().getPlayers())
		{
			double dx = (double) player.getX() - point1.x;
			double dy = (double) player.getY() - point1.y;
			
			if (Math.sqrt(dx * dx + dy * dy) < BOAT_BROADCAST_RADIUS)
			{
				for (L2GameServerPacket p : packets)
					player.sendPacket(p);
			}
			else
			{
				dx = (double) player.getX() - point2.x;
				dy = (double) player.getY() - point2.y;
				
				if (Math.sqrt(dx * dx + dy * dy) < BOAT_BROADCAST_RADIUS)
					for (L2GameServerPacket p : packets)
						player.sendPacket(p);
			}
		}
	}
	
	private static class SingletonHolder
	{
		protected static final BoatManager _instance = new BoatManager();
	}
}