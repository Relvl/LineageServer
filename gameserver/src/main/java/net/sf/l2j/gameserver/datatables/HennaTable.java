package net.sf.l2j.gameserver.datatables;

import net.sf.l2j.gameserver.model.item.Henna;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.File;
import java.util.*;

public class HennaTable {
    private static final Logger LOGGER = LoggerFactory.getLogger(HennaTable.class);

    private final Map<Integer, Henna> _henna = new HashMap<>();
    private final Map<Integer, List<Henna>> _hennaTrees = new HashMap<>();

    protected HennaTable() {
        try {
            File f = new File("./data/xml/henna.xml");
            Document doc = XMLDocumentFactory.getInstance().loadDocument(f);
            Node n = doc.getFirstChild();

            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                if (!d.getNodeName().equalsIgnoreCase("henna")) { continue; }

                StatsSet hennaDat = new StatsSet();
                Integer id = Integer.valueOf(d.getAttributes().getNamedItem("symbol_id").getNodeValue());

                hennaDat.set("symbol_id", id);

                hennaDat.set("dye", Integer.valueOf(d.getAttributes().getNamedItem("dye_id").getNodeValue()));
                hennaDat.set("price", Integer.valueOf(d.getAttributes().getNamedItem("price").getNodeValue()));

                hennaDat.set("INT", Integer.valueOf(d.getAttributes().getNamedItem("INT").getNodeValue()));
                hennaDat.set("STR", Integer.valueOf(d.getAttributes().getNamedItem("STR").getNodeValue()));
                hennaDat.set("CON", Integer.valueOf(d.getAttributes().getNamedItem("CON").getNodeValue()));
                hennaDat.set("MEN", Integer.valueOf(d.getAttributes().getNamedItem("MEN").getNodeValue()));
                hennaDat.set("DEX", Integer.valueOf(d.getAttributes().getNamedItem("DEX").getNodeValue()));
                hennaDat.set("WIT", Integer.valueOf(d.getAttributes().getNamedItem("WIT").getNodeValue()));
                String[] classes = d.getAttributes().getNamedItem("classes").getNodeValue().split(",");

                Henna template = new Henna(hennaDat);
                _henna.put(id, template);

                for (String clas : classes) {
                    Integer classId = Integer.valueOf(clas);
                    if (_hennaTrees.containsKey(classId)) {
                        _hennaTrees.get(classId).add(template);
                    }
                    else {
                        List<Henna> list = new ArrayList<>();
                        list.add(template);
                        _hennaTrees.put(classId, list);
                    }
                }
            }
        }
        catch (Exception e) {
            LOGGER.error("HennaTable: Error loading from database:{}", e.getMessage(), e);
        }
        LOGGER.info("HennaTable: Loaded {} templates.", _henna.size());
    }

    public Henna getTemplate(int id) {
        return _henna.get(id);
    }

    public List<Henna> getAvailableHenna(int classId) {
        List<Henna> henna = _hennaTrees.get(classId);
        if (henna == null) { return Collections.emptyList(); }

        return henna;
    }

    public static HennaTable getInstance() {
        return SingletonHolder._instance;
    }

    private static class SingletonHolder {
        protected static final HennaTable _instance = new HennaTable();
    }
}