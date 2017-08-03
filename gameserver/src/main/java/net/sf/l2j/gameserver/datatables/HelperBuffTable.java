package net.sf.l2j.gameserver.datatables;

import net.sf.l2j.gameserver.templates.L2HelperBuff;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents the Newbie Helper Buff list Author: Ayor
 */
public class HelperBuffTable {
    private static final Logger LOGGER = LoggerFactory.getLogger(HelperBuffTable.class);

    /** The table containing all Buff of the Newbie Helper */
    private final List<L2HelperBuff> _helperBuff;

    /**
     * The player level since Newbie Helper can give the first buff <BR>
     * Used to generate message : "Come back here when you have reached level ...")
     */
    private int _magicClassLowestLevel = 100;
    private int _physicClassLowestLevel = 100;

    /**
     * The player level above which Newbie Helper won't give any buff <BR>
     * Used to generate message : "Only novice character of level ... or less can receive my support magic.")
     */
    private int _magicClassHighestLevel = 1;
    private int _physicClassHighestLevel = 1;

    public static HelperBuffTable getInstance() {
        return SingletonHolder._instance;
    }

    /**
     * Create and Load the Newbie Helper Buff list from helper_buff_list.xml
     */
    protected HelperBuffTable() {
        _helperBuff = new ArrayList<>();

        try {
            File f = new File("./data/xml/helper_buff_list.xml");
            Document doc = XMLDocumentFactory.getInstance().loadDocument(f);

            Node n = doc.getFirstChild();
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                if (d.getNodeName().equalsIgnoreCase("buff")) {
                    NamedNodeMap attrs = d.getAttributes();

                    int id = Integer.valueOf(attrs.getNamedItem("id").getNodeValue());
                    int skill_id = Integer.valueOf(attrs.getNamedItem("skill_id").getNodeValue());
                    int skill_level = Integer.valueOf(attrs.getNamedItem("skill_level").getNodeValue());
                    int lower_level = Integer.valueOf(attrs.getNamedItem("lower_level").getNodeValue());
                    int upper_level = Integer.valueOf(attrs.getNamedItem("upper_level").getNodeValue());
                    boolean is_magic_class = Boolean.valueOf(attrs.getNamedItem("is_magic_class").getNodeValue());

                    StatsSet helperBuffDat = new StatsSet();

                    helperBuffDat.set("id", id);
                    helperBuffDat.set("skillID", skill_id);
                    helperBuffDat.set("skillLevel", skill_level);
                    helperBuffDat.set("lowerLevel", lower_level);
                    helperBuffDat.set("upperLevel", upper_level);
                    helperBuffDat.set("isMagicClass", is_magic_class);

                    if (!is_magic_class) {
                        if (lower_level < _physicClassLowestLevel) { _physicClassLowestLevel = lower_level; }

                        if (upper_level > _physicClassHighestLevel) { _physicClassHighestLevel = upper_level; }
                    }
                    else {
                        if (lower_level < _magicClassLowestLevel) { _magicClassLowestLevel = lower_level; }

                        if (upper_level > _magicClassHighestLevel) { _magicClassHighestLevel = upper_level; }
                    }

                    // Add this Helper Buff to the Helper Buff List
                    _helperBuff.add(new L2HelperBuff(helperBuffDat));
                }
            }
        }
        catch (Exception e) {
            LOGGER.error("HelperBuffTable: Error while creating table", e);
        }

        LOGGER.info("HelperBuffTable: Loaded {} buffs.", _helperBuff.size());
    }

    /**
     * @return the Helper Buff List
     */
    public List<L2HelperBuff> getHelperBuffTable() {
        return _helperBuff;
    }

    /**
     * @return Returns the magicClassHighestLevel.
     */
    public int getMagicClassHighestLevel() {
        return _magicClassHighestLevel;
    }

    /**
     * @return Returns the magicClassLowestLevel.
     */
    public int getMagicClassLowestLevel() {
        return _magicClassLowestLevel;
    }

    /**
     * @return Returns the physicClassHighestLevel.
     */
    public int getPhysicClassHighestLevel() {
        return _physicClassHighestLevel;
    }

    /**
     * @return Returns the physicClassLowestLevel.
     */
    public int getPhysicClassLowestLevel() {
        return _physicClassLowestLevel;
    }

    private static class SingletonHolder {
        protected static final HelperBuffTable _instance = new HelperBuffTable();
    }
}