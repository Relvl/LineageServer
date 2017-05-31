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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class SpellbookTable
{
	private static final Logger _log = Logger.getLogger(SkillTreeTable.class.getName());
	
	private static final Map<Integer, Integer> _skillSpellbooks = new HashMap<>();
	
	public static SpellbookTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected SpellbookTable()
	{
		try
		{
			File f = new File("./data/xml/spellbooks.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);
			
			for (Node list = doc.getFirstChild().getFirstChild(); list != null; list = list.getNextSibling())
			{
				if (list.getNodeName().equalsIgnoreCase("book"))
				{
					NamedNodeMap bookAttrs = list.getAttributes();
					_skillSpellbooks.put(Integer.valueOf(bookAttrs.getNamedItem("skill_id").getNodeValue()), Integer.valueOf(bookAttrs.getNamedItem("item_id").getNodeValue()));
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Error while loading spellbook data: " + e.getMessage(), e);
		}
		
		_log.info("SpellbookTable: Loaded " + _skillSpellbooks.size() + " spellbooks.");
	}
	
	public int getBookForSkill(int skillId, int level)
	{
		if (skillId == L2Skill.SKILL_DIVINE_INSPIRATION)
		{
			if (!Config.DIVINE_SP_BOOK_NEEDED)
				return 0;
			
			switch (level)
			{
				case 1:
					return 8618; // Ancient Book - Divine Inspiration (Modern Language Version)
				case 2:
					return 8619; // Ancient Book - Divine Inspiration (Original Language Version)
				case 3:
					return 8620; // Ancient Book - Divine Inspiration (Manuscript)
				case 4:
					return 8621; // Ancient Book - Divine Inspiration (Original Version)
				default:
					return 0;
			}
		}
		
		if (level != 1)
			return 0;
		
		if (!Config.SP_BOOK_NEEDED)
			return 0;
		
		if (!_skillSpellbooks.containsKey(skillId))
			return 0;
		
		return _skillSpellbooks.get(skillId);
	}
	
	private static class SingletonHolder
	{
		protected static final SpellbookTable _instance = new SpellbookTable();
	}
}