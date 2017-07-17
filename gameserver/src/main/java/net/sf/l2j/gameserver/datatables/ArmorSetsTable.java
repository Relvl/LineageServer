package net.sf.l2j.gameserver.datatables;

import net.sf.l2j.gameserver.model.item.ArmorSet;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ArmorSetsTable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArmorSetsTable.class);

    private final Map<Integer, ArmorSet> _armorSets = new HashMap<>();

    protected ArmorSetsTable() {
        try {
            File f = new File("./data/xml/armorsets.xml");
            Document doc = XMLDocumentFactory.getInstance().loadDocument(f);

            Node n = doc.getFirstChild();
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                if (d.getNodeName().equalsIgnoreCase("armorset")) {
                    NamedNodeMap attrs = d.getAttributes();

                    final int chest = Integer.parseInt(attrs.getNamedItem("chest").getNodeValue());
                    final int[] set =
                            {
                                    chest,
                                    Integer.parseInt(attrs.getNamedItem("legs").getNodeValue()),
                                    Integer.parseInt(attrs.getNamedItem("head").getNodeValue()),
                                    Integer.parseInt(attrs.getNamedItem("gloves").getNodeValue()),
                                    Integer.parseInt(attrs.getNamedItem("feet").getNodeValue())
                            };
                    final int skillId = Integer.parseInt(attrs.getNamedItem("skillId").getNodeValue());
                    final int shield = Integer.parseInt(attrs.getNamedItem("shield").getNodeValue());
                    final int shieldSkillId = Integer.parseInt(attrs.getNamedItem("shieldSkillId").getNodeValue());
                    final int enchant6Skill = Integer.parseInt(attrs.getNamedItem("enchant6Skill").getNodeValue());

                    _armorSets.put(chest, new ArmorSet(set, skillId, shield, shieldSkillId, enchant6Skill));
                }
            }
        }
        catch (Exception e) {
            LOGGER.error("ArmorSetsTable: Error loading armorsets.xml", e);
        }
        LOGGER.info("ArmorSetsTable: Loaded {} armor sets.", _armorSets.size());
    }

    public static ArmorSetsTable getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public ArmorSet getSet(int chestId) {
        return _armorSets.get(chestId);
    }

    private static final class SingletonHolder {
        private static final ArmorSetsTable INSTANCE = new ArmorSetsTable();
    }
}