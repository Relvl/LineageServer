package net.sf.l2j.gameserver.datatables;

import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.gameserver.model.buylist.NpcBuyList;
import net.sf.l2j.gameserver.model.buylist.Product;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuyListTable {
    private static final Logger LOGGER = LoggerFactory.getLogger(BuyListTable.class);

    private final Map<Integer, NpcBuyList> _buyLists = new HashMap<>();

    public static BuyListTable getInstance() {
        return SingletonHolder._instance;
    }

    protected BuyListTable() {
        try {
            File f = new File("./data/xml/buylists.xml");
            Document doc = XMLDocumentFactory.getInstance().loadDocument(f);
            Node n = doc.getFirstChild();

            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                if (!d.getNodeName().equalsIgnoreCase("buylist")) { continue; }

                // Setup a new BuyList.
                int buyListId = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
                NpcBuyList buyList = new NpcBuyList(buyListId);
                buyList.setNpcId(Integer.parseInt(d.getAttributes().getNamedItem("npcId").getNodeValue()));

                // Read products and feed the BuyList with it.
                for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling()) {
                    if (!c.getNodeName().equalsIgnoreCase("product")) { continue; }

                    NamedNodeMap attrs = c.getAttributes();
                    Node attr = attrs.getNamedItem("id");

                    int itemId = Integer.parseInt(attr.getNodeValue());

                    int price = 0;
                    attr = attrs.getNamedItem("price");
                    if (attr != null) { price = Integer.parseInt(attr.getNodeValue()); }

                    int count = -1;
                    attr = attrs.getNamedItem("count");
                    if (attr != null) { count = Integer.parseInt(attr.getNodeValue()); }

                    long restockDelay = -1;
                    attr = attrs.getNamedItem("restockDelay");
                    if (attr != null) { restockDelay = Long.parseLong(attr.getNodeValue()); }

                    Item item = ItemTable.getInstance().getTemplate(itemId);
                    if (item != null) { buyList.addProduct(new Product(buyList.getListId(), item, price, restockDelay, count)); }
                    else { LOGGER.warn("BuyListTable: Item not found for buyList: {}, ItemID: {}", buyList.getListId(), itemId); }
                }
                _buyLists.put(buyListId, buyList);
            }
        }
        catch (Exception e) {
            LOGGER.error("BuyListTable: Error loading from database: {}", e.getMessage(), e);
        }
        LOGGER.info("BuyListTable: Loaded {} buylists.", _buyLists.size());

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            Statement statement = con.createStatement();
            ResultSet rs = statement.executeQuery("SELECT * FROM `buylists`");
            while (rs.next()) {
                int buyListId = rs.getInt("buylist_id");
                int itemId = rs.getInt("item_id");
                int count = rs.getInt("count");
                long nextRestockTime = rs.getLong("next_restock_time");

                NpcBuyList buyList = _buyLists.get(buyListId);
                if (buyList == null) {
                    LOGGER.warn("BuyList found in database but not loaded from xml! BuyListId: {}", buyListId);
                    continue;
                }

                Product product = buyList.getProductByItemId(itemId);
                if (product == null) {
                    LOGGER.warn("ItemId found in database but not loaded from xml! BuyListId: {} ItemId: {}", buyListId, itemId);
                    continue;
                }

                if (count < product.getMaxCount()) {
                    product.setCount(count);
                    product.restartRestockTask(nextRestockTime);
                }
            }
            rs.close();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("BuyListTable: Failed to load buyList data from database.", e);
        }
    }

    public NpcBuyList getBuyList(int listId) {
        return _buyLists.get(listId);
    }

    public List<NpcBuyList> getBuyListsByNpcId(int npcId) {
        List<NpcBuyList> list = new ArrayList<>();
        for (NpcBuyList buyList : _buyLists.values()) {
            if (buyList.isNpcAllowed(npcId)) { list.add(buyList); }
        }
        return list;
    }

    private static class SingletonHolder {
        protected static final BuyListTable _instance = new BuyListTable();
    }
}