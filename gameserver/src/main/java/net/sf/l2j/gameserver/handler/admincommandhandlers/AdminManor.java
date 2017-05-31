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

import java.util.ArrayList;
import java.util.StringTokenizer;

import net.sf.l2j.Config;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.CastleManorManager;
import net.sf.l2j.gameserver.instancemanager.CastleManorManager.CropProcure;
import net.sf.l2j.gameserver.instancemanager.CastleManorManager.SeedProduction;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * This class handles following admin commands:
 * <ul>
 * <li>manor_info = shows info about current manor state</li>
 * <li>manor_approve = approves settings for the next manor period</li>
 * <li>manor_setnext = changes manor settings to the next day's</li>
 * <li>manor_reset castle = resets all manor data for specified castle (or all)</li>
 * <li>manor_setmaintenance = sets manor system under maintenance mode</li>
 * <li>manor_save = saves all manor data into database</li>
 * <li>manor_disable = disables manor system</li>
 * </ul>
 * @author l3x
 */
public class AdminManor implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_manor",
		"admin_manor_approve",
		"admin_manor_setnext",
		"admin_manor_reset",
		"admin_manor_setmaintenance",
		"admin_manor_save",
		"admin_manor_disable"
	};
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		StringTokenizer st = new StringTokenizer(command);
		command = st.nextToken();
		
		if (command.equals("admin_manor"))
			showMainPage(activeChar);
		else if (command.equals("admin_manor_setnext"))
		{
			CastleManorManager.getInstance().setNextPeriod();
			CastleManorManager.getInstance().setNewManorRefresh();
			CastleManorManager.getInstance().updateManorRefresh();
			activeChar.sendMessage("Manor System: set to next period");
			showMainPage(activeChar);
		}
		else if (command.equals("admin_manor_approve"))
		{
			CastleManorManager.getInstance().approveNextPeriod();
			CastleManorManager.getInstance().setNewPeriodApprove();
			CastleManorManager.getInstance().updatePeriodApprove();
			activeChar.sendMessage("Manor System: next period approved");
			showMainPage(activeChar);
		}
		else if (command.equals("admin_manor_reset"))
		{
			int castleId = 0;
			try
			{
				castleId = Integer.parseInt(st.nextToken());
			}
			catch (Exception e)
			{
			}
			
			if (castleId > 0)
			{
				final Castle castle = CastleManager.getInstance().getCastleById(castleId);
				
				castle.setCropProcure(new ArrayList<CropProcure>(), CastleManorManager.PERIOD_CURRENT);
				castle.setCropProcure(new ArrayList<CropProcure>(), CastleManorManager.PERIOD_NEXT);
				castle.setSeedProduction(new ArrayList<SeedProduction>(), CastleManorManager.PERIOD_CURRENT);
				castle.setSeedProduction(new ArrayList<SeedProduction>(), CastleManorManager.PERIOD_NEXT);
				
				if (Config.ALT_MANOR_SAVE_ALL_ACTIONS)
				{
					castle.saveCropData();
					castle.saveSeedData();
				}
				activeChar.sendMessage("Manor data for " + castle.getName() + " was nulled");
			}
			else
			{
				for (Castle castle : CastleManager.getInstance().getCastles())
				{
					castle.setCropProcure(new ArrayList<CropProcure>(), CastleManorManager.PERIOD_CURRENT);
					castle.setCropProcure(new ArrayList<CropProcure>(), CastleManorManager.PERIOD_NEXT);
					castle.setSeedProduction(new ArrayList<SeedProduction>(), CastleManorManager.PERIOD_CURRENT);
					castle.setSeedProduction(new ArrayList<SeedProduction>(), CastleManorManager.PERIOD_NEXT);
					
					if (Config.ALT_MANOR_SAVE_ALL_ACTIONS)
					{
						castle.saveCropData();
						castle.saveSeedData();
					}
				}
				activeChar.sendMessage("Manor data was nulled");
			}
			showMainPage(activeChar);
		}
		else if (command.equals("admin_manor_setmaintenance"))
		{
			final boolean mode = CastleManorManager.getInstance().isUnderMaintenance();
			
			CastleManorManager.getInstance().setUnderMaintenance(!mode);
			activeChar.sendMessage((mode) ? "Manor System: not under maintenance" : "Manor System: under maintenance");
			showMainPage(activeChar);
		}
		else if (command.equals("admin_manor_save"))
		{
			CastleManorManager.getInstance().save();
			activeChar.sendMessage("Manor System: all data saved");
			showMainPage(activeChar);
		}
		else if (command.equals("admin_manor_disable"))
		{
			final boolean mode = CastleManorManager.getInstance().isDisabled();
			
			CastleManorManager.getInstance().setDisabled(!mode);
			activeChar.sendMessage((mode) ? "Manor System: enabled" : "Manor System: disabled");
			showMainPage(activeChar);
		}
		
		return true;
	}
	
	private static String formatTime(long millis)
	{
		String s = "";
		int secs = (int) millis / 1000;
		int mins = secs / 60;
		secs -= mins * 60;
		int hours = mins / 60;
		mins -= hours * 60;
		
		if (hours > 0)
			s += hours + ":";
		s += mins + ":";
		s += secs;
		return s;
	}
	
	private static void showMainPage(L2PcInstance activeChar)
	{
		final StringBuilder sb = new StringBuilder(500);
		for (Castle c : CastleManager.getInstance().getCastles())
			StringUtil.append(sb, "<tr><td>", c.getName(), "</td><td>", c.getManorCost(CastleManorManager.PERIOD_CURRENT), "a</td><td>", c.getManorCost(CastleManorManager.PERIOD_NEXT), "a</td></tr>");
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/admin/manor.htm");
		html.replace("%disabled%", String.valueOf(CastleManorManager.getInstance().isDisabled()));
		html.replace("%maintenance%", String.valueOf(CastleManorManager.getInstance().isUnderMaintenance()));
		html.replace("%refresh%", formatTime(CastleManorManager.getInstance().getMillisToManorRefresh()));
		html.replace("%approve%", formatTime(CastleManorManager.getInstance().getMillisToNextPeriodApprove()));
		html.replace("%value1%", (CastleManorManager.getInstance().isUnderMaintenance()) ? "Set normal" : "Set maintenance");
		html.replace("%value2%", (CastleManorManager.getInstance().isDisabled()) ? "Enable" : "Disable");
		html.replace("%castles%", sb.toString());
		activeChar.sendPacket(html);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}