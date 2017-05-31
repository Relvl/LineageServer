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

import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.instancemanager.MovieMakerManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author KKnD
 */
public class AdminMovieMaker implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_addseq",
		"admin_playseqq",
		"admin_delsequence",
		"admin_editsequence",
		"admin_addsequence",
		"admin_playsequence",
		"admin_movie",
		"admin_updatesequence",
		"admin_broadcast",
		"admin_playmovie",
		"admin_broadmovie"
	};
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.equals("admin_movie"))
		{
			MovieMakerManager.getInstance().mainHtm(activeChar);
		}
		else if (command.startsWith("admin_playseqq"))
		{
			try
			{
				MovieMakerManager.getInstance().playSequence(Integer.parseInt(command.substring(15)), activeChar);
			}
			catch (Exception e)
			{
				activeChar.sendMessage("You entered an invalid sequence id.");
				MovieMakerManager.getInstance().mainHtm(activeChar);
				return false;
			}
		}
		else if (command.equals("admin_addseq"))
		{
			MovieMakerManager.getInstance().addSequence(activeChar);
		}
		else if (command.startsWith("admin_delsequence"))
		{
			try
			{
				MovieMakerManager.getInstance().deleteSequence(Integer.parseInt(command.substring(18)), activeChar);
			}
			catch (Exception e)
			{
				activeChar.sendMessage("You entered an invalid sequence id.");
				MovieMakerManager.getInstance().mainHtm(activeChar);
				return false;
			}
		}
		else if (command.startsWith("admin_broadcast"))
		{
			try
			{
				MovieMakerManager.getInstance().broadcastSequence(Integer.parseInt(command.substring(16)), activeChar);
			}
			catch (Exception e)
			{
				activeChar.sendMessage("You entered an invalid sequence id.");
				MovieMakerManager.getInstance().mainHtm(activeChar);
				return false;
			}
		}
		else if (command.equals("admin_playmovie"))
		{
			MovieMakerManager.getInstance().playMovie(0, activeChar);
		}
		else if (command.equals("admin_broadmovie"))
		{
			MovieMakerManager.getInstance().playMovie(1, activeChar);
		}
		else if (command.startsWith("admin_editsequence"))
		{
			try
			{
				MovieMakerManager.getInstance().editSequence(Integer.parseInt(command.substring(19)), activeChar);
			}
			catch (Exception e)
			{
				activeChar.sendMessage("You entered an invalid sequence id.");
				MovieMakerManager.getInstance().mainHtm(activeChar);
				return false;
			}
		}
		else
		{
			String[] args = command.split(" ");
			if (args.length < 10)
			{
				activeChar.sendMessage("Some arguments are missing.");
				return false;
			}
			
			final int targ = (activeChar.getTarget() != null) ? activeChar.getTarget().getObjectId() : activeChar.getObjectId();
			
			if (command.startsWith("admin_addsequence"))
			{
				MovieMakerManager.getInstance().addSequence(activeChar, Integer.parseInt(args[1]), targ, Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]), Integer.parseInt(args[7]), Integer.parseInt(args[8]), Integer.parseInt(args[9]));
			}
			else if (command.startsWith("admin_playsequence"))
			{
				MovieMakerManager.getInstance().playSequence(activeChar, targ, Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]), Integer.parseInt(args[7]), Integer.parseInt(args[8]));
			}
			else if (command.startsWith("admin_updatesequence"))
			{
				MovieMakerManager.getInstance().updateSequence(activeChar, Integer.parseInt(args[1]), targ, Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]), Integer.parseInt(args[7]), Integer.parseInt(args[8]), Integer.parseInt(args[9]));
			}
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}