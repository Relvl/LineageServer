package net.sf.l2j.gameserver.datatables;

import net.sf.l2j.gameserver.model.item.SummonItem;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SummonItemsData {
    private static final Logger LOGGER = LoggerFactory.getLogger(SummonItemsData.class);

    private static final Map<Integer, SummonItem> SUMMONITEMS = new HashMap<>();

    public static SummonItemsData getInstance() {
        return SingletonHolder.INSTANCE;
    }

    protected SummonItemsData() {
        try {
            File f = new File("./data/xml/summon_items.xml");
            Document doc = XMLDocumentFactory.getInstance().loadDocument(f);

            Node n = doc.getFirstChild();
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                if (d.getNodeName().equalsIgnoreCase("summon_item")) {
                    NamedNodeMap node = d.getAttributes();

                    int itemID = Integer.valueOf(node.getNamedItem("itemID").getNodeValue());
                    int npcID = Integer.valueOf(node.getNamedItem("npcID").getNodeValue());
                    byte summonType = Byte.valueOf(node.getNamedItem("summonType").getNodeValue());

                    SUMMONITEMS.put(itemID, new SummonItem(itemID, npcID, summonType));
                }
            }
        }
        catch (Exception e) {
            LOGGER.warn("SummonItemsData: Error while creating SummonItemsData table: ", e);
        }
    }

    public static SummonItem getSummonItem(int itemId) {
        return SUMMONITEMS.get(itemId);
    }

    private static final class SingletonHolder {
        private static final SummonItemsData INSTANCE = new SummonItemsData();
    }
}