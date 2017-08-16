package net.sf.l2j.gameserver.datatables;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2AccessLevel;
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
public class AccessLevels {
    public static final int MASTER_ACCESS_LEVEL_NUMBER = Config.MASTERACCESS_LEVEL;
    public static final int USER_ACCESS_LEVEL_NUMBER = 0;
    private static final Logger LOGGER = LoggerFactory.getLogger(AccessLevels.class);
    public static L2AccessLevel MASTER_ACCESS_LEVEL = new L2AccessLevel(MASTER_ACCESS_LEVEL_NUMBER, "Master Access", Config.MASTERACCESS_NAME_COLOR, Config.MASTERACCESS_TITLE_COLOR, null, true, true, true, true, true, true, true, true);
    public static L2AccessLevel USER_ACCESS_LEVEL = new L2AccessLevel(USER_ACCESS_LEVEL_NUMBER, "User", 0xFFFFFF, 0xFFFF77, null, false, false, false, true, false, true, true, true);
    private final Map<Integer, L2AccessLevel> _accessLevels = new HashMap<>();

    protected AccessLevels() {
        try {
            File f = new File("./data/xml/access_levels.xml");
            Document doc = XMLDocumentFactory.getInstance().loadDocument(f);

            Node n = doc.getFirstChild();
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                if (d.getNodeName().equalsIgnoreCase("access")) {
                    NamedNodeMap attrs = d.getAttributes();

                    int accessLevel = Integer.valueOf(attrs.getNamedItem("level").getNodeValue());
                    String name = attrs.getNamedItem("name").getNodeValue();

                    if (accessLevel == USER_ACCESS_LEVEL_NUMBER) {
                        LOGGER.warn("AccessLevels: Access level {} is using reserved user access level " + USER_ACCESS_LEVEL_NUMBER + ". Ignoring it!", name);
                        continue;
                    }
                    else if (accessLevel == MASTER_ACCESS_LEVEL_NUMBER) {
                        LOGGER.warn("AccessLevels: Access level {} is using reserved master access level {}. Ignoring it!", name, MASTER_ACCESS_LEVEL_NUMBER);
                        continue;
                    }
                    else if (accessLevel < 0) {
                        LOGGER.warn("AccessLevels: Access level {} is using banned access level (below 0). Ignoring it!", name);
                        continue;
                    }

                    int nameColor;
                    try {
                        nameColor = Integer.decode("0x" + attrs.getNamedItem("nameColor").getNodeValue());
                    }
                    catch (NumberFormatException nfe) {
                        nameColor = Integer.decode("0xFFFFFF");
                    }

                    int titleColor;
                    try {
                        titleColor = Integer.decode("0x" + attrs.getNamedItem("titleColor").getNodeValue());
                    }
                    catch (NumberFormatException nfe) {
                        titleColor = Integer.decode("0x77FFFF");
                    }

                    String childs = attrs.getNamedItem("childAccess").getNodeValue();
                    boolean isGm = Boolean.valueOf(attrs.getNamedItem("isGm").getNodeValue());
                    boolean allowPeaceAttack = Boolean.valueOf(attrs.getNamedItem("allowPeaceAttack").getNodeValue());
                    boolean allowFixedRes = Boolean.valueOf(attrs.getNamedItem("allowFixedRes").getNodeValue());
                    boolean allowTransaction = Boolean.valueOf(attrs.getNamedItem("allowTransaction").getNodeValue());
                    boolean allowAltG = Boolean.valueOf(attrs.getNamedItem("allowAltg").getNodeValue());
                    boolean giveDamage = Boolean.valueOf(attrs.getNamedItem("giveDamage").getNodeValue());
                    boolean takeAggro = Boolean.valueOf(attrs.getNamedItem("takeAggro").getNodeValue());
                    boolean gainExp = Boolean.valueOf(attrs.getNamedItem("gainExp").getNodeValue());

                    _accessLevels.put(accessLevel, new L2AccessLevel(accessLevel, name, nameColor, titleColor, childs.isEmpty()
                                                                                                               ? null
                                                                                                               : childs, isGm, allowPeaceAttack, allowFixedRes, allowTransaction, allowAltG, giveDamage, takeAggro, gainExp));
                }
            }
        }
        catch (Exception e) {
            LOGGER.error("AccessLevels: Error loading from database: {}", e.getMessage(), e);
        }

        LOGGER.info("AccessLevels: Loaded {} accesses.", _accessLevels.size());

        // Add finally the normal user access level.
        _accessLevels.put(USER_ACCESS_LEVEL_NUMBER, USER_ACCESS_LEVEL);
    }

    public static AccessLevels getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Returns the access level by characterAccessLevel
     *
     * @param accessLevelNum as int
     * @return AccessLevel: AccessLevel instance by char access level<br>
     */
    public L2AccessLevel getAccessLevel(int accessLevelNum) {
        L2AccessLevel accessLevel = null;

        synchronized (_accessLevels) {
            accessLevel = _accessLevels.get(accessLevelNum);
        }
        return accessLevel;
    }

    public void addBanAccessLevel(int accessLevel) {
        synchronized (_accessLevels) {
            if (accessLevel > -1) { return; }

            _accessLevels.put(accessLevel, new L2AccessLevel(accessLevel, "Banned", -1, -1, null, false, false, false, false, false, false, false, false));
        }
    }

    private static final class SingletonHolder {
        private static final AccessLevels INSTANCE = new AccessLevels();
    }
}