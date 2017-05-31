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
package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.gameserver.geoengine.GeoData;
import net.sf.l2j.gameserver.geoengine.PathFinding;
import net.sf.l2j.gameserver.geoengine.geodata.GeoStructure;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * @author -Nemesiss-
 */
public class AdminGeodata implements IAdminCommandHandler
{
	private final String Y = "x ";
	private final String N = "   ";
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_geo_bug",
		"admin_geo_pos",
		"admin_geo_can_see",
		"admin_geo_can_move_beeline"
	};
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.startsWith("admin_geo_bug"))
		{
			int geoX = GeoData.getInstance().getGeoX(activeChar.getX());
			int geoY = GeoData.getInstance().getGeoY(activeChar.getY());
			if (GeoData.getInstance().hasGeoPos(geoX, geoY))
			{
				try
				{
					String comment = command.substring(14);
					if (GeoData.getInstance().addGeoBug(activeChar.getPosition().getWorldPosition(), activeChar.getName() + ": " + comment))
						activeChar.sendMessage("GeoData bug saved.");
				}
				catch (Exception e)
				{
					activeChar.sendMessage("Usage: //admin_geo_bug comments");
				}
			}
			else
				activeChar.sendMessage("There is no geodata at this position.");
		}
		else if ("admin_geo_pos".equals(command))
		{
			int geoX = GeoData.getInstance().getGeoX(activeChar.getX());
			int geoY = GeoData.getInstance().getGeoY(activeChar.getY());
			int rx = (activeChar.getX() - L2World.WORLD_X_MIN) / L2World.TILE_SIZE + L2World.TILE_X_MIN;
			int ry = (activeChar.getY() - L2World.WORLD_Y_MIN) / L2World.TILE_SIZE + L2World.TILE_Y_MIN;
			activeChar.sendMessage("Region: " + rx + "_" + ry);
			if (GeoData.getInstance().hasGeoPos(geoX, geoY))
			{
				// Block block = GeoData.getInstance().getBlock(geoX, geoY);
				int geoZ = GeoData.getInstance().getHeightNearest(geoX, geoY, activeChar.getZ());
				byte nswe = GeoData.getInstance().getNsweNearest(geoX, geoY, geoZ);
				
				// activeChar.sendMessage("NSWE: " + block.getClass().getSimpleName());
				activeChar.sendMessage("    " + ((nswe & GeoStructure.CELL_FLAG_NW) != 0 ? Y : N) + ((nswe & GeoStructure.CELL_FLAG_N) != 0 ? Y : N) + ((nswe & GeoStructure.CELL_FLAG_NE) != 0 ? Y : N) + "         GeoX=" + geoX);
				activeChar.sendMessage("    " + ((nswe & GeoStructure.CELL_FLAG_W) != 0 ? Y : N) + "o " + ((nswe & GeoStructure.CELL_FLAG_E) != 0 ? Y : N) + "         GeoY=" + geoY);
				activeChar.sendMessage("    " + ((nswe & GeoStructure.CELL_FLAG_SW) != 0 ? Y : N) + ((nswe & GeoStructure.CELL_FLAG_S) != 0 ? Y : N) + ((nswe & GeoStructure.CELL_FLAG_SE) != 0 ? Y : N) + "         GeoZ=" + geoZ);
			}
			else
				activeChar.sendMessage("There is no geodata at this position.");
		}
		else if ("admin_geo_can_see".equals(command))
		{
			L2Object target = activeChar.getTarget();
			if (target != null)
			{
				if (PathFinding.getInstance().canSeeTarget(activeChar, target))
					activeChar.sendMessage("Can see target.");
				else
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANT_SEE_TARGET));
			}
			else
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
		}
		
		else if ("admin_geo_can_move_beeline".equals(command))
		{
			L2Object target = activeChar.getTarget();
			if (target != null)
			{
				if (PathFinding.getInstance().canMoveToTarget(activeChar.getX(), activeChar.getY(), activeChar.getZ(), target.getX(), target.getY(), target.getZ()))
					activeChar.sendMessage("Can move beeline.");
				else
					activeChar.sendMessage("Can not move beeline!");
			}
			else
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
		}
		else
			return false;
		
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}