package net.sf.l2j.gameserver.datatables;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.skill.SkillConst;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class SpellbookTable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpellbookTable.class);
    private static final Map<Integer, Integer> SKILL_SPELLBOOKS = new HashMap<>();

    public static SpellbookTable getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private SpellbookTable() {
        try {
            File file = new File("./data/xml/spellbooks.xml");
            Document doc = XMLDocumentFactory.getInstance().loadDocument(file);
            for (Node list = doc.getFirstChild().getFirstChild(); list != null; list = list.getNextSibling()) {
                if (list.getNodeName().equalsIgnoreCase("book")) {
                    NamedNodeMap bookAttrs = list.getAttributes();
                    SKILL_SPELLBOOKS.put(Integer.valueOf(bookAttrs.getNamedItem("skill_id").getNodeValue()), Integer.valueOf(bookAttrs.getNamedItem("item_id").getNodeValue()));
                }
            }
        }
        catch (Exception e) {
            LOGGER.warn("Error while loading spellbook data: ", e);
        }
    }

    public static int getBookForSkill(int skillId, int level) {
        if (skillId == SkillConst.SKILL_DIVINE_INSPIRATION) {
            if (!Config.DIVINE_SP_BOOK_NEEDED) { return 0; }
            switch (level) {
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
        if (level != 1) { return 0; }
        if (!Config.SP_BOOK_NEEDED) { return 0; }
        if (!SKILL_SPELLBOOKS.containsKey(skillId)) { return 0; }
        return SKILL_SPELLBOOKS.get(skillId);
    }

    private static final class SingletonHolder {
        private static final SpellbookTable INSTANCE = new SpellbookTable();
    }
}