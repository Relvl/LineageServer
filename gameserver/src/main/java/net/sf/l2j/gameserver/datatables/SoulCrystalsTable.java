package net.sf.l2j.gameserver.datatables;

import net.sf.l2j.gameserver.model.soulcrystal.LevelingInfo;
import net.sf.l2j.gameserver.model.soulcrystal.LevelingInfo.AbsorbCrystalType;
import net.sf.l2j.gameserver.model.soulcrystal.SoulCrystalData;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SoulCrystalsTable {
    private static final Logger _log = Logger.getLogger(SoulCrystalsTable.class.getName());

    private static final Map<Integer, SoulCrystalData> _soulCrystals = new HashMap<>();
    private static final Map<Integer, LevelingInfo> _npcLevelingInfos = new HashMap<>();

    public static void load() {
        try {
            File f = new File("./data/xml/soul_crystals.xml");
            Document doc = XMLDocumentFactory.getInstance().loadDocument(f);

            Node first = doc.getFirstChild();
            for (Node n = first.getFirstChild(); n != null; n = n.getNextSibling()) {
                if ("crystals".equalsIgnoreCase(n.getNodeName())) {
                    for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                        if ("crystal".equalsIgnoreCase(d.getNodeName())) {
                            NamedNodeMap attrs = d.getAttributes();
                            Node att = attrs.getNamedItem("crystal");
                            if (att == null) {
                                _log.severe("SoulCrystalsTable: Missing \"crystal\" in \"soul_crystals.xml\", skipping.");
                                continue;
                            }
                            int crystalItemId = Integer.parseInt(att.getNodeValue());

                            att = attrs.getNamedItem("level");
                            if (att == null) {
                                _log.severe("SoulCrystalsTable: Missing \"level\" in \"soul_crystals.xml\" crystal=" + crystalItemId + ", skipping.");
                                continue;
                            }
                            int level = Integer.parseInt(att.getNodeValue());

                            att = attrs.getNamedItem("staged");
                            if (att == null) {
                                _log.severe("SoulCrystalsTable: Missing \"staged\" in \"soul_crystals.xml\" crystal=" + crystalItemId + ", skipping.");
                                continue;
                            }
                            int stagedItemId = Integer.parseInt(att.getNodeValue());

                            att = attrs.getNamedItem("broken");
                            if (att == null) {
                                _log.severe("SoulCrystalsTable: Missing \"broken\" in \"soul_crystals.xml\" crystal=" + crystalItemId + ", skipping.");
                                continue;
                            }
                            int brokenItemId = Integer.parseInt(att.getNodeValue());

                            _soulCrystals.put(crystalItemId, new SoulCrystalData(level, crystalItemId, stagedItemId, brokenItemId));
                        }
                    }
                }
                else if ("npcs".equalsIgnoreCase(n.getNodeName())) {
                    for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                        if ("npc".equalsIgnoreCase(d.getNodeName())) {
                            NamedNodeMap attrs = d.getAttributes();
                            Node att = attrs.getNamedItem("npcId");
                            if (att == null) {
                                _log.severe("SoulCrystalsTable: Missing \"npcId\" in \"soul_crystals.xml\", skipping.");
                                continue;
                            }
                            int npcId = Integer.parseInt(att.getNodeValue());

                            Node det = d.getFirstChild().getNextSibling();
                            if (det.getNodeName().equals("detail")) {
                                attrs = det.getAttributes();

                                att = attrs.getNamedItem("skill");
                                boolean skillRequired = false;
                                if (att != null) { skillRequired = Boolean.parseBoolean(att.getNodeValue()); }

                                att = attrs.getNamedItem("chanceStage");
                                int chanceStage = 10;
                                if (att != null) { chanceStage = Integer.parseInt(att.getNodeValue()); }

                                att = attrs.getNamedItem("chanceBreak");
                                int chanceBreak = 0;
                                if (att != null) { chanceBreak = Integer.parseInt(att.getNodeValue()); }

                                att = attrs.getNamedItem("absorbType");
                                AbsorbCrystalType absorbType = AbsorbCrystalType.LAST_HIT;
                                if (att != null) { absorbType = Enum.valueOf(AbsorbCrystalType.class, att.getNodeValue()); }

                                att = attrs.getNamedItem("levelList");
                                int[] levelList = null;
                                if (att != null) {
                                    String[] strings = att.getNodeValue().split(",");
                                    levelList = new int[strings.length];
                                    for (int i = 0; i < strings.length; i++) {
                                        Integer value = Integer.parseInt(strings[i].trim());
                                        if (value == null) {
                                            _log.severe("SoulCrystalsTable: Bad level value for npcId=" + npcId + ", token=" + strings[i]);
                                            continue;
                                        }
                                        levelList[i] = value;
                                    }
                                }

                                _npcLevelingInfos.put(npcId, new LevelingInfo(absorbType, skillRequired, chanceStage, chanceBreak, levelList));
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            _log.log(Level.WARNING, "SoulCrystalsTable: Could not parse soul_crystals.xml file: " + e.getMessage(), e);
        }

        _log.info("SoulCrystalsTable: Loaded " + _soulCrystals.size() + " SC data and " + _npcLevelingInfos.size() + " NPC data.");
    }

    public static Map<Integer, SoulCrystalData> getSoulCrystalInfos() {
        return _soulCrystals;
    }

    public static Map<Integer, LevelingInfo> getNpcInfos() {
        return _npcLevelingInfos;
    }
}