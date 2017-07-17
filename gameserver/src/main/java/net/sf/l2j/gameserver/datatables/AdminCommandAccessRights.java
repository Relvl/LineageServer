package net.sf.l2j.gameserver.datatables;

import net.sf.l2j.gameserver.model.L2AccessLevel;
import net.sf.l2j.gameserver.model.L2AdminCommandAccessRight;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author FBIagent
 */
public class AdminCommandAccessRights {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminCommandAccessRights.class);
    private final Map<String, L2AdminCommandAccessRight> _adminCommandAccessRights;

    protected AdminCommandAccessRights() {
        _adminCommandAccessRights = new HashMap<>();
        load();
    }

    public static AdminCommandAccessRights getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public void reload() {
        _adminCommandAccessRights.clear();
        load();
    }

    private void load() {
        try {
            File f = new File("./data/xml/admin_commands_rights.xml");
            Document doc = XMLDocumentFactory.getInstance().loadDocument(f);

            Node n = doc.getFirstChild();
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                if (d.getNodeName().equalsIgnoreCase("aCar")) {
                    NamedNodeMap attrs = d.getAttributes();

                    String adminCommand = attrs.getNamedItem("name").getNodeValue();
                    String accessLevels = attrs.getNamedItem("accessLevel").getNodeValue();
                    _adminCommandAccessRights.put(adminCommand, new L2AdminCommandAccessRight(adminCommand, accessLevels));
                }
            }
        }
        catch (Exception e) {
            LOGGER.error("AdminCommandAccessRights: Error loading from database:{}", e.getMessage(), e);
        }

        LOGGER.info("AdminCommandAccessRights: Loaded {} rights.", _adminCommandAccessRights.size());
    }

    public boolean hasAccess(String adminCommand, L2AccessLevel accessLevel) {
        if (accessLevel.getLevel() == AccessLevels.MASTER_ACCESS_LEVEL_NUMBER) { return true; }

        L2AdminCommandAccessRight acar = _adminCommandAccessRights.get(adminCommand);
        if (acar == null) {
            LOGGER.info("AdminCommandAccessRights: No rights defined for admin command {}.", adminCommand);
            return false;
        }

        return acar.hasAccess(accessLevel);
    }

    private static final class SingletonHolder {
        private static final AdminCommandAccessRights INSTANCE = new AdminCommandAccessRights();
    }
}