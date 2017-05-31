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
package net.sf.l2j.gameserver.datatables;

import java.io.File;
import java.io.FileWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.Announcement;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Announcements system.<br>
 * See header String below for all details.
 * @author Sikken, xblx && Tryskell
 */
public class AnnouncementTable
{
	private static final Logger _log = Logger.getLogger(AnnouncementTable.class.getName());
	
	private static final String HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n<!-- \n@param String message - the message to be announced \n@param Boolean critical - type of announcement (true = critical,false = normal) \n@param Boolean auto - when the announcement will be displayed (true = auto,false = on player login) \n@param Integer initial_delay - time delay for the first announce (used only if auto=true;value in seconds) \n@param Integer delay - time delay for the announces following the first announce (used only if auto=true;value in seconds) \n@param Integer limit - limit of announces (used only if auto=true, 0 = unlimited) \n--> \n";
	
	private final Map<Integer, Announcement> _announcements = new ConcurrentHashMap<>();
	
	public static AnnouncementTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected AnnouncementTable()
	{
		load();
	}
	
	public void reload()
	{
		// Clean first tasks from automatic announcements.
		for (Announcement announce : _announcements.values())
			announce.stopTask();
		
		load();
	}
	
	public void load()
	{
		try
		{
			File f = new File("./data/xml/announcements.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);
			
			int id = 0;
			
			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equalsIgnoreCase("announcement"))
				{
					final String message = d.getAttributes().getNamedItem("message").getNodeValue();
					if (message == null || message.isEmpty())
					{
						_log.warning("AnnouncementTable: The message is empty. Ignoring it!");
						continue;
					}
					
					boolean critical = Boolean.valueOf(d.getAttributes().getNamedItem("critical").getNodeValue());
					boolean auto = Boolean.valueOf(d.getAttributes().getNamedItem("auto").getNodeValue());
					
					if (auto)
					{
						int initialDelay = Integer.valueOf(d.getAttributes().getNamedItem("initial_delay").getNodeValue());
						int delay = Integer.valueOf(d.getAttributes().getNamedItem("delay").getNodeValue());
						
						int limit = Integer.valueOf(d.getAttributes().getNamedItem("limit").getNodeValue());
						if (limit < 0)
							limit = 0;
						
						_announcements.put(id, new Announcement(message, critical, auto, initialDelay, delay, limit));
					}
					else
						_announcements.put(id, new Announcement(message, critical));
					
					id++;
				}
			}
		}
		catch (Exception e)
		{
			_log.warning("AnnouncementTable: Error loading from file:" + e.getMessage());
		}
		_log.info("AnnouncementTable: Loaded " + _announcements.size() + " announcements.");
	}
	
	/**
	 * Send stored announcements from _announcements Map to a specific player.
	 * @param activeChar : The player to send infos.
	 * @param autoOrNot : If True, sends only automatic announcements, otherwise send classic ones.
	 */
	public void showAnnouncements(L2PcInstance activeChar, boolean autoOrNot)
	{
		for (Announcement announce : _announcements.values())
		{
			if (autoOrNot)
				announce.reloadTask();
			else
			{
				if (announce.isAuto())
					continue;
				
				activeChar.sendPacket(new CreatureSay(0, announce.isCritical() ? Say2.CRITICAL_ANNOUNCE : Say2.ANNOUNCEMENT, activeChar.getName(), announce.getMessage()));
			}
		}
	}
	
	/**
	 * Use Broadcast class method in order to send announcement, wrapped into a ioobe try/catch.
	 * @param command : The command to affect.
	 * @param lengthToTrim : The length to trim, in order to send only the message without the command.
	 * @param critical : Is the message critical or not.
	 */
	public void handleAnnounce(String command, int lengthToTrim, boolean critical)
	{
		try
		{
			Broadcast.announceToOnlinePlayers(command.substring(lengthToTrim), critical);
		}
		catch (StringIndexOutOfBoundsException e)
		{
		}
	}
	
	/**
	 * Send a static HTM with dynamic announcements content took from _announcements Map.
	 * @param activeChar : The player to send the HTM packet.
	 */
	public void listAnnouncements(L2PcInstance activeChar)
	{
		final StringBuilder sb = new StringBuilder("<br>");
		if (_announcements.isEmpty())
			sb.append("<tr><td>The XML file doesn't contain any content.</td></tr>");
		else
		{
			for (Map.Entry<Integer, Announcement> entry : _announcements.entrySet())
			{
				final int index = entry.getKey();
				final Announcement announce = entry.getValue();
				
				StringUtil.append(sb, "<tr><td width=240>#", index, " - ", announce.getMessage(), "</td><td></td></tr><tr><td>Critical: ", announce.isCritical(), " | Auto: ", announce.isAuto(), "</td><td><button value=\"Delete\" action=\"bypass -h admin_announce del ", index, "\" width=65 height=19 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td></tr>");
			}
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setHtml(HtmCache.getInstance().getHtmForce("data/html/admin/announce_list.htm"));
		html.replace("%announces%", sb.toString());
		activeChar.sendPacket(html);
	}
	
	/**
	 * Add an announcement, only if the message isn't empty or null.
	 * @param message : The String to announce.
	 * @param critical : Is it a critical announcement or not.
	 * @param auto : Is it using a specific task or not.
	 * @param initialDelay : Initial delay of the task, used only if auto is setted to True.
	 * @param delay : Delay of the task, used only if auto is setted to True.
	 * @param limit : Maximum amount of loops the task will do before ending.
	 * @return True if the announcement has been successfully added, False otherwise.
	 */
	public boolean addAnnouncement(String message, boolean critical, boolean auto, int initialDelay, int delay, int limit)
	{
		// Empty or null message.
		if (message == null || message.isEmpty())
			return false;
		
		// Register announcement.
		if (auto)
			_announcements.put(_announcements.size(), new Announcement(message, critical, auto, initialDelay, delay, limit));
		else
			_announcements.put(_announcements.size(), new Announcement(message, critical));
		
		// Regenerate the XML.
		regenerateXML();
		return true;
	}
	
	/**
	 * End the task linked to an announcement and delete it.
	 * @param index : the Map index to remove.
	 */
	public void delAnnouncement(int index)
	{
		// Stop the current task, if any.
		_announcements.remove(index).stopTask();
		
		// Regenerate the XML.
		regenerateXML();
	}
	
	/**
	 * This method allows to refresh the XML with infos took from _announcements Map.
	 */
	private void regenerateXML()
	{
		final StringBuilder sb = new StringBuilder(HEADER);
		
		sb.append("<list> \n");
		
		for (Announcement announce : _announcements.values())
			StringUtil.append(sb, "<announcement message=\"", announce.getMessage(), "\" critical=\"", announce.isCritical(), "\" auto=\"", announce.isAuto(), "\" initial_delay=\"", announce.getInitialDelay(), "\" delay=\"", announce.getDelay(), "\" limit=\"", announce.getLimit(), "\" /> \n");
		
		sb.append("</list>");
		
		try (FileWriter fw = new FileWriter(new File("./data/xml/announcements.xml")))
		{
			fw.write(sb.toString());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private static class SingletonHolder
	{
		protected static final AnnouncementTable _instance = new AnnouncementTable();
	}
}