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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Armor;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.model.multisell.Entry;
import net.sf.l2j.gameserver.model.multisell.Ingredient;
import net.sf.l2j.gameserver.model.multisell.ListContainer;
import net.sf.l2j.gameserver.network.serverpackets.MultiSellList;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class MultisellData
{
	private static final Logger _log = Logger.getLogger(MultisellData.class.getName());
	
	private static final ListContainer EMPTY_CONTAINER = new ListContainer();
	
	private final Map<Integer, ListContainer> _entries = new HashMap<>();
	
	public MultisellData()
	{
		parse();
	}
	
	public void reload()
	{
		_entries.clear();
		parse();
	}
	
	public static MultisellData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public ListContainer getList(int id)
	{
		return _entries.get(id);
	}
	
	/**
	 * This will generate the multisell list for the items. There exist various parameters in multisells that affect the way they will appear:
	 * <ul>
	 * <li>inventory only: * if true, only show items of the multisell for which the "primary" ingredients are already in the player's inventory. By "primary" ingredients we mean weapon and armor. * if false, show the entire list.</li>
	 * <li>maintain enchantment: presumably, only lists with "inventory only" set to true should sometimes have this as true. This makes no sense otherwise... * If true, then the product will match the enchantment level of the ingredient. if the player has multiple items that match the ingredient
	 * list but the enchantment levels differ, then the entries need to be duplicated to show the products and ingredients for each enchantment level. For example: If the player has a crystal staff +1 and a crystal staff +3 and goes to exchange it at the mammon, the list should have all exchange
	 * possibilities for the +1 staff, followed by all possibilities for the +3 staff. * If false, then any level ingredient will be considered equal and product will always be at +0</li>
	 * <li>apply taxes: Uses the "taxIngredient" entry in order to add a certain amount of adena to the ingredients</li>
	 * </ul>
	 * @param listId
	 * @param inventoryOnly
	 * @param player
	 * @param taxRate
	 * @return the multisell list for the items.
	 */
	private ListContainer generateMultiSell(int listId, boolean inventoryOnly, L2PcInstance player, double taxRate)
	{
		final ListContainer listTemplate = _entries.get(listId);
		if (listTemplate == null)
			return EMPTY_CONTAINER;
		
		final ListContainer list = new ListContainer();
		list.setListId(listId);
		
		if (inventoryOnly)
		{
			if (player == null)
				return list;
			
			ItemInstance[] items;
			if (listTemplate.getMaintainEnchantment())
				items = player.getInventory().getUniqueItemsByEnchantLevel(false, false, false);
			else
				items = player.getInventory().getUniqueItems(false, false, false);
			
			for (ItemInstance item : items)
			{
				// only do the matchup on equipable items that are not currently equipped
				// so for each appropriate item, produce a set of entries for the multisell list.
				if ((item.getItem() instanceof Armor) || (item.getItem() instanceof Weapon))
				{
					final int enchantLevel = (listTemplate.getMaintainEnchantment() ? item.getEnchantLevel() : 0);
					
					// loop through the entries to see which ones we wish to include
					for (Entry ent : listTemplate.getEntries())
					{
						boolean doInclude = false;
						
						// check ingredients of this entry to see if it's an entry we'd like to include.
						for (Ingredient ing : ent.getIngredients())
						{
							if (item.getItemId() == ing.getItemId())
							{
								doInclude = true;
								break;
							}
						}
						
						// manipulate the ingredients of the template entry for this particular instance shown
						// i.e: Assign enchant levels and/or apply taxes as needed.
						if (doInclude)
							list.addEntry(prepareEntry(ent, listTemplate.getApplyTaxes(), listTemplate.getMaintainEnchantment(), enchantLevel, taxRate));
					}
				}
			}
		}
		// this is a list-all type
		else
		{
			// if no taxes are applied, no modifications are needed
			for (Entry ent : listTemplate.getEntries())
				list.addEntry(prepareEntry(ent, listTemplate.getApplyTaxes(), false, 0, taxRate));
		}
		return list;
	}
	
	/**
	 * Regarding taxation, the following is the case:
	 * <ul>
	 * <li>a) The taxes come out purely from the adena TaxIngredient</li>
	 * <li>b) If the entry has no adena ingredients other than the taxIngredient, the resultingamount of adena is appended to the entry</li>
	 * <li>c) If the entry already has adena as an entry, the taxIngredient is used in order to increase the count for the existing adena ingredient</li>
	 * </ul>
	 * @param templateEntry
	 * @param applyTaxes
	 * @param maintainEnchantment
	 * @param enchantLevel
	 * @param taxRate
	 * @return
	 */
	private static Entry prepareEntry(Entry templateEntry, boolean applyTaxes, boolean maintainEnchantment, int enchantLevel, double taxRate)
	{
		final Entry newEntry = new Entry();
		newEntry.setEntryId(templateEntry.getEntryId() * 100000 + enchantLevel);
		
		int adenaAmount = 0;
		
		for (Ingredient ing : templateEntry.getIngredients())
		{
			// load the ingredient from the template
			final Ingredient newIngredient = new Ingredient(ing);
			
			// if taxes are to be applied, modify/add the adena count based on the template adena/ancient adena count
			if (ing.getItemId() == 57 && ing.isTaxIngredient())
			{
				if (applyTaxes)
					adenaAmount += (int) Math.round(ing.getItemCount() * taxRate);
				
				continue; // do not adena yet, as non-taxIngredient adena entries might occur next (order not guaranteed)
			}
			else if (ing.getItemId() == 57)
			{
				adenaAmount += ing.getItemCount();
				continue; // do not adena yet, as taxIngredient adena entries might occur next (order not guaranteed)
			}
			// if it is an armor/weapon, modify the enchantment level appropriately, if necessary
			else if (maintainEnchantment && newIngredient.getItemId() > 0)
			{
				final Item tempItem = ItemTable.getInstance().createDummyItem(ing.getItemId()).getItem();
				if ((tempItem instanceof Armor) || (tempItem instanceof Weapon))
					newIngredient.setEnchantmentLevel(enchantLevel);
			}
			
			// finally, add this ingredient to the entry
			newEntry.addIngredient(newIngredient);
		}
		
		// now add the adena, if any.
		if (adenaAmount > 0)
			newEntry.addIngredient(new Ingredient(57, adenaAmount, 0, false, false));
		
		// Now modify the enchantment level of products, if necessary
		for (Ingredient ing : templateEntry.getProducts())
		{
			// load the ingredient from the template
			final Ingredient newIngredient = new Ingredient(ing);
			
			if (maintainEnchantment)
			{
				// if it is an armor/weapon, modify the enchantment level appropriately
				// (note, if maintain enchantment is "false" this modification will result to a +0)
				final Item tempItem = ItemTable.getInstance().createDummyItem(ing.getItemId()).getItem();
				if ((tempItem instanceof Armor) || (tempItem instanceof Weapon))
					newIngredient.setEnchantmentLevel(enchantLevel);
			}
			newEntry.addProduct(newIngredient);
		}
		return newEntry;
	}
	
	public void separateAndSend(int listId, L2PcInstance player, boolean inventoryOnly, double taxRate)
	{
		final ListContainer list = generateMultiSell(listId, inventoryOnly, player, taxRate);
		
		ListContainer temp = new ListContainer();
		int page = 1;
		
		temp.setListId(list.getListId());
		
		for (Entry e : list.getEntries())
		{
			if (temp.getEntries().size() == 40)
			{
				player.sendPacket(new MultiSellList(temp, page++, 0));
				temp = new ListContainer();
				temp.setListId(list.getListId());
			}
			temp.addEntry(e);
		}
		player.sendPacket(new MultiSellList(temp, page, 1));
	}
	
	private static void hashFiles(String dirname, List<File> hash)
	{
		File dir = new File("./data/" + dirname);
		if (!dir.isDirectory())
		{
			_log.config("Dir " + dir.getAbsolutePath() + " doesn't exist.");
			return;
		}
		
		File[] files = dir.listFiles();
		for (File f : files)
		{
			if (f.getName().endsWith(".xml"))
				hash.add(f);
		}
	}
	
	private void parse()
	{
		Document doc = null;
		int id = 0;
		List<File> files = new ArrayList<>();
		hashFiles("multisell", files);
		
		for (File f : files)
		{
			try
			{
				id = Integer.parseInt(f.getName().replaceAll(".xml", ""));
				doc = XMLDocumentFactory.getInstance().loadDocument(f);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error loading file " + f, e);
			}
			
			try
			{
				ListContainer list = parseDocument(doc);
				list.setListId(id);
				_entries.put(id, list);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error in file " + f, e);
			}
		}
		_log.log(Level.INFO, "L2Multisell: Loaded " + _entries.size() + " files.");
	}
	
	private static ListContainer parseDocument(Document doc)
	{
		ListContainer list = new ListContainer();
		
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				Node attribute;
				attribute = n.getAttributes().getNamedItem("applyTaxes");
				if (attribute == null)
					list.setApplyTaxes(false);
				else
					list.setApplyTaxes(Boolean.parseBoolean(attribute.getNodeValue()));
				
				attribute = n.getAttributes().getNamedItem("maintainEnchantment");
				if (attribute == null)
					list.setMaintainEnchantment(false);
				else
					list.setMaintainEnchantment(Boolean.parseBoolean(attribute.getNodeValue()));
				
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("item".equalsIgnoreCase(d.getNodeName()))
					{
						Entry e = parseEntry(d);
						list.addEntry(e);
					}
				}
			}
			else if ("item".equalsIgnoreCase(n.getNodeName()))
			{
				Entry e = parseEntry(n);
				list.addEntry(e);
			}
		}
		return list;
	}
	
	private static Entry parseEntry(Node n)
	{
		int entryId = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
		
		Node first = n.getFirstChild();
		Entry entry = new Entry();
		
		for (n = first; n != null; n = n.getNextSibling())
		{
			if ("ingredient".equalsIgnoreCase(n.getNodeName()))
			{
				Node attribute;
				
				int id = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
				int count = Integer.parseInt(n.getAttributes().getNamedItem("count").getNodeValue());
				boolean isTaxIngredient = false, mantainIngredient = false;
				
				attribute = n.getAttributes().getNamedItem("isTaxIngredient");
				
				if (attribute != null)
					isTaxIngredient = Boolean.parseBoolean(attribute.getNodeValue());
				
				attribute = n.getAttributes().getNamedItem("mantainIngredient");
				
				if (attribute != null)
					mantainIngredient = Boolean.parseBoolean(attribute.getNodeValue());
				
				Ingredient e = new Ingredient(id, count, isTaxIngredient, mantainIngredient);
				entry.addIngredient(e);
			}
			else if ("production".equalsIgnoreCase(n.getNodeName()))
			{
				int id = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
				int count = Integer.parseInt(n.getAttributes().getNamedItem("count").getNodeValue());
				
				Node attribute;
				int enchant = 0;
				attribute = n.getAttributes().getNamedItem("enchant");
				if (attribute != null)
					enchant = Integer.parseInt(attribute.getNodeValue());
				
				Ingredient e = new Ingredient(id, count, enchant, false, false);
				entry.addProduct(e);
			}
		}
		
		entry.setEntryId(entryId);
		
		return entry;
	}
	
	private static class SingletonHolder
	{
		protected static final MultisellData _instance = new MultisellData();
	}
}