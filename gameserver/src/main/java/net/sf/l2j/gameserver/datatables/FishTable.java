package net.sf.l2j.gameserver.datatables;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.FishData;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FishTable {
    private static final Logger LOGGER = LoggerFactory.getLogger(FishTable.class);

    private static final List<FishData> _fishes = new ArrayList<>();

    public static FishTable getInstance() {
        return SingletonHolder._instance;
    }

    protected FishTable() {
        try {
            File f = new File("./data/xml/fishes.xml");
            Document doc = XMLDocumentFactory.getInstance().loadDocument(f);

            Node n = doc.getFirstChild();
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                if (d.getNodeName().equalsIgnoreCase("fish")) {
                    NamedNodeMap attrs = d.getAttributes();

                    int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
                    int lvl = Integer.parseInt(attrs.getNamedItem("level").getNodeValue());
                    String name = attrs.getNamedItem("name").getNodeValue();
                    int hp = Integer.parseInt(attrs.getNamedItem("hp").getNodeValue());
                    int hpreg = Integer.parseInt(attrs.getNamedItem("hpregen").getNodeValue());
                    int type = Integer.parseInt(attrs.getNamedItem("fish_type").getNodeValue());
                    int group = Integer.parseInt(attrs.getNamedItem("fish_group").getNodeValue());
                    int fish_guts = Integer.parseInt(attrs.getNamedItem("fish_guts").getNodeValue());
                    int guts_check_time = Integer.parseInt(attrs.getNamedItem("guts_check_time").getNodeValue());
                    int wait_time = Integer.parseInt(attrs.getNamedItem("wait_time").getNodeValue());
                    int combat_time = Integer.parseInt(attrs.getNamedItem("combat_time").getNodeValue());

                    _fishes.add(new FishData(id, lvl, name, hp, hpreg, type, group, fish_guts, guts_check_time, wait_time, combat_time));
                }
            }
        }
        catch (Exception e) {
            LOGGER.error("FishTable: Error while creating table", e);
        }

        LOGGER.info("FishTable: Loaded {} fishes.", _fishes.size());
    }

    public static FishData getFish(int lvl, int type, int group) {
        List<FishData> result = new ArrayList<>();

        for (FishData fish : _fishes) {
            if (fish.getLevel() != lvl || fish.getType() != type || fish.getGroup() != group) { continue; }

            result.add(fish);
        }

        if (result.isEmpty()) {
            LOGGER.warn("Couldn't find any fish with lvl: {} and type: {}", lvl, type);
            return null;
        }

        return Rnd.get(result);
    }

    private static class SingletonHolder {
        protected static final FishTable _instance = new FishTable();
    }
}