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
package net.sf.l2j.gameserver.scripting;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public final class ScriptManager implements Runnable
{
	private static final Logger _log = Logger.getLogger(ScriptManager.class.getName());
	
	public static ScriptManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public static final int PERIOD = 5 * 60 * 1000; // 5 minutes
	
	private final List<Quest> _quests = new ArrayList<>();
	private final List<ScheduledQuest> _scheduled = new LinkedList<>();
	
	public ScriptManager()
	{
		load();
	}
	
	private final void load()
	{
		try
		{
			File f = new File("./data/xml/scripts.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);
			
			Node n = doc.getFirstChild();
			for (Node script = n.getFirstChild(); script != null; script = script.getNextSibling())
			{
				if (!script.getNodeName().equalsIgnoreCase("script"))
					continue;
				
				final NamedNodeMap params = script.getAttributes();
				
				// Get path to the script.
				Node param = params.getNamedItem("path");
				if (param == null)
				{
					_log.warning("ScriptManager: The \"path\" is not defined.");
					continue;
				}
				
				// Create the script.
				final String path = param.getNodeValue();
				try
				{
					Quest instance = (Quest) Class.forName("net.sf.l2j.gameserver.scripting." + path).newInstance();
					
					// The script has been identified as a scheduled script ; make proper checks and schedule the launch.
					if (instance instanceof ScheduledQuest)
					{
						param = params.getNamedItem("schedule");
						if (param != null)
						{
							final String type = param.getNodeValue();
							
							param = params.getNamedItem("start");
							if (param == null)
							{
								_log.warning("ScriptManager: Missing \"start\" parametr for \"" + path + "\".");
								continue;
							}
							final String start = param.getNodeValue();
							
							param = params.getNamedItem("end");
							if (param == null)
							{
								_log.warning("ScriptManager: Missing \"end\" parametr for \"" + path + "\".");
								continue;
							}
							final String end = param.getNodeValue();
							
							if (((ScheduledQuest) instance).setSchedule(type, start, end))
								_scheduled.add(((ScheduledQuest) instance));
						}
					}
					// The script is an AI or any other custom type of script.
					else
						_quests.add(instance);
				}
				catch (ClassNotFoundException e)
				{
					_log.warning("ScriptManager: Script \"" + path + "\" not found.");
					continue;
				}
			}
			
			_log.info("ScriptManager: Loaded " + _quests.size() + " scripts, " + _scheduled.size() + " schedules.");
		}
		catch (Exception e)
		{
			_log.warning("ScriptManager: Error loading \"scripts.xml\" file, " + e);
		}
		
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(this, PERIOD, PERIOD);
	}
	
	@Override
	public void run()
	{
		// each PERIOD
		final long next = System.currentTimeMillis() + PERIOD;
		for (ScheduledQuest script : _scheduled)
		{
			// check all scheduled scripts
			final long eta = script.getTimeLeft();
			if (eta < next)
				// and schedule particular ones
				ThreadPoolManager.getInstance().scheduleGeneral(new Scheduler(script), eta);
		}
	}
	
	private final class Scheduler implements Runnable
	{
		private final ScheduledQuest _script;
		
		protected Scheduler(ScheduledQuest script)
		{
			_script = script;
		}
		
		@Override
		public void run()
		{
			// notify script
			_script.notifyAndSchedule();
			
			// in case the next action should terminate before the resolution of ScriptManager, schedule the the action again
			final long eta = _script.getTimeLeft();
			if (eta < System.currentTimeMillis() + PERIOD)
				ThreadPoolManager.getInstance().scheduleGeneral(new Scheduler(_script), eta);
		}
	}
	
	/**
	 * Returns the quest by given quest name.
	 * @param questName : The name of the quest.
	 * @return Quest : Quest to be returned, null if quest does not exist.
	 */
	public final Quest getQuest(String questName)
	{
		// Check all quests.
		for (Quest q : _quests)
		{
			// If quest found, return him.
			if (q.getName().equalsIgnoreCase(questName))
				return q;
		}
		
		// Otherwise return null.
		return null;
	}
	
	/**
	 * Returns the quest by given quest id.
	 * @param questId : The id of the quest.
	 * @return Quest : Quest to be returned, null if quest does not exist.
	 */
	public final Quest getQuest(int questId)
	{
		// Check all quests.
		for (Quest q : _quests)
		{
			// If quest found, return him.
			if (q.getQuestId() == questId)
				return q;
		}
		
		// Otherwise return null.
		return null;
	}
	
	/**
	 * Returns the list of quests.
	 * @return List<Quest> : List of quest.
	 */
	public final List<Quest> getQuests()
	{
		return _quests;
	}
	
	private static class SingletonHolder
	{
		protected static final ScriptManager _instance = new ScriptManager();
	}
}