package net.sf.l2j.gameserver.datatables;

import net.sf.l2j.gameserver.model.L2TeleportLocation;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class TeleportLocationTable {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeleportLocationTable.class);

    private static final Map<Integer, L2TeleportLocation> TELEPORTS = new HashMap<>();

    public static TeleportLocationTable getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private TeleportLocationTable() { load(); }

    public void reload() {
        TELEPORTS.clear();
        load();
    }

    public void load() {
        try {
            File file = new File("./data/xml/teleports.xml");
            Document doc = XMLDocumentFactory.getInstance().loadDocument(file);

            Node child = doc.getFirstChild();
            for (Node d = child.getFirstChild(); d != null; d = d.getNextSibling()) {
                if (d.getNodeName().equalsIgnoreCase("teleport")) {
                    NamedNodeMap node = d.getAttributes();

                    L2TeleportLocation teleport = new L2TeleportLocation();
                    teleport.setTeleId(Integer.valueOf(node.getNamedItem("id").getNodeValue()));
                    teleport.setLocX(Integer.valueOf(node.getNamedItem("loc_x").getNodeValue()));
                    teleport.setLocY(Integer.valueOf(node.getNamedItem("loc_y").getNodeValue()));
                    teleport.setLocZ(Integer.valueOf(node.getNamedItem("loc_z").getNodeValue()));
                    teleport.setPrice(Integer.valueOf(node.getNamedItem("price").getNodeValue()));
                    teleport.setIsForNoble(Integer.valueOf(node.getNamedItem("fornoble").getNodeValue()) == 1);

                    TELEPORTS.put(teleport.getTeleId(), teleport);
                }
            }
        }
        catch (Exception e) {
            LOGGER.error("TeleportLocationTable: Error while creating table", e);
        }
        LOGGER.info("TeleportLocationTable: Loaded {} templates.", TELEPORTS.size());
    }

    public static L2TeleportLocation getTemplate(int id) {
        return TELEPORTS.get(id);
    }

    private static final class SingletonHolder {
        private static final TeleportLocationTable INSTANCE = new TeleportLocationTable();
    }
}