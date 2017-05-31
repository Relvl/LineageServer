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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.buylist.NpcBuyList;
import net.sf.l2j.gameserver.model.buylist.Product;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class BuyListTable
{
	private static final Logger _log = Logger.getLogger(BuyListTable.class.getName());
	
	private final Map<Integer, NpcBuyList> _buyLists = new HashMap<>();
	
	public static BuyListTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected BuyListTable()
	{
		try
		{
			final File f = new File("./data/xml/buylists.xml");
			final Document doc = XMLDocumentFactory.getInstance().loadDocument(f);
			final Node n = doc.getFirstChild();
			
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (!d.getNodeName().equalsIgnoreCase("buylist"))
					continue;
				
				// Setup a new BuyList.
				final int buyListId = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
				final NpcBuyList buyList = new NpcBuyList(buyListId);
				buyList.setNpcId(Integer.parseInt(d.getAttributes().getNamedItem("npcId").getNodeValue()));
				
				// Read products and feed the BuyList with it.
				for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
				{
					if (!c.getNodeName().equalsIgnoreCase("product"))
						continue;
					
					NamedNodeMap attrs = c.getAttributes();
					Node attr = attrs.getNamedItem("id");
					
					final int itemId = Integer.parseInt(attr.getNodeValue());
					
					int price = 0;
					attr = attrs.getNamedItem("price");
					if (attr != null)
						price = Integer.parseInt(attr.getNodeValue());
					
					int count = -1;
					attr = attrs.getNamedItem("count");
					if (attr != null)
						count = Integer.parseInt(attr.getNodeValue());
					
					long restockDelay = -1;
					attr = attrs.getNamedItem("restockDelay");
					if (attr != null)
						restockDelay = Long.parseLong(attr.getNodeValue());
					
					final Item item = ItemTable.getInstance().getTemplate(itemId);
					if (item != null)
						buyList.addProduct(new Product(buyList.getListId(), item, price, restockDelay, count));
					else
						_log.warning("BuyListTable: Item not found for buyList: " + buyList.getListId() + ", ItemID: " + itemId);
				}
				_buyLists.put(buyListId, buyList);
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "BuyListTable: Error loading from database: " + e.getMessage(), e);
		}
		_log.config("BuyListTable: Loaded " + _buyLists.size() + " buylists.");
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			final Statement statement = con.createStatement();
			final ResultSet rs = statement.executeQuery("SELECT * FROM `buylists`");
			while (rs.next())
			{
				int buyListId = rs.getInt("buylist_id");
				int itemId = rs.getInt("item_id");
				int count = rs.getInt("count");
				long nextRestockTime = rs.getLong("next_restock_time");
				
				final NpcBuyList buyList = _buyLists.get(buyListId);
				if (buyList == null)
				{
					_log.warning("BuyList found in database but not loaded from xml! BuyListId: " + buyListId);
					continue;
				}
				
				final Product product = buyList.getProductByItemId(itemId);
				if (product == null)
				{
					_log.warning("ItemId found in database but not loaded from xml! BuyListId: " + buyListId + " ItemId: " + itemId);
					continue;
				}
				
				if (count < product.getMaxCount())
				{
					product.setCount(count);
					product.restartRestockTask(nextRestockTime);
				}
			}
			rs.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "BuyListTable: Failed to load buyList data from database.", e);
		}
	}
	
	public NpcBuyList getBuyList(int listId)
	{
		return _buyLists.get(listId);
	}
	
	public List<NpcBuyList> getBuyListsByNpcId(int npcId)
	{
		final List<NpcBuyList> list = new ArrayList<>();
		for (NpcBuyList buyList : _buyLists.values())
		{
			if (buyList.isNpcAllowed(npcId))
				list.add(buyList);
		}
		return list;
	}
	
	private static class SingletonHolder
	{
		protected static final BuyListTable _instance = new BuyListTable();
	}
}