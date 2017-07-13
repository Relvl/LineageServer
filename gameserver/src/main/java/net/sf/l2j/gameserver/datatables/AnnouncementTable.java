package net.sf.l2j.gameserver.datatables;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.EChatType;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.Announcement;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.client.game_to_client.CreatureSay;
import net.sf.l2j.gameserver.network.client.game_to_client.NpcHtmlMessage;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.File;
import java.io.FileWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class AnnouncementTable {
    private static final Logger _log = Logger.getLogger(AnnouncementTable.class.getName());

    private static final String HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n<!-- \n@param String message - the message to be announced \n@param Boolean critical - type of announcement (true = critical,false = normal) \n@param Boolean auto - when the announcement will be displayed (true = auto,false = on player login) \n@param Integer initial_delay - time delay for the first announce (used only if auto=true;value in seconds) \n@param Integer delay - time delay for the announces following the first announce (used only if auto=true;value in seconds) \n@param Integer limit - limit of announces (used only if auto=true, 0 = unlimited) \n--> \n";
    private static final String DATA_XML_ANNOUNCEMENTS_XML = "./data/xml/announcements.xml";

    private final Map<Integer, Announcement> announcements = new ConcurrentHashMap<>();

    protected AnnouncementTable() {
        load();
    }

    public static AnnouncementTable getInstance() {
        return SingletonHolder._instance;
    }

    public static void handleAnnounce(String command, int lengthToTrim, boolean critical) {
        try {
            Broadcast.announceToOnlinePlayers(command.substring(lengthToTrim), critical);
        }
        catch (StringIndexOutOfBoundsException ignored) {}
    }

    public void reload() {
        for (Announcement announce : announcements.values()) {
            announce.stopTask();
        }
        load();
    }

    public void load() {
        try {
            File f = new File(DATA_XML_ANNOUNCEMENTS_XML);
            Document doc = XMLDocumentFactory.getInstance().loadDocument(f);

            int id = 0;

            Node n = doc.getFirstChild();
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                if (d.getNodeName().equalsIgnoreCase("announcement")) {
                    String message = d.getAttributes().getNamedItem("message").getNodeValue();
                    if (message == null || message.isEmpty()) {
                        _log.warning("AnnouncementTable: The message is empty. Ignoring it!");
                        continue;
                    }

                    boolean critical = Boolean.valueOf(d.getAttributes().getNamedItem("critical").getNodeValue());
                    boolean auto = Boolean.valueOf(d.getAttributes().getNamedItem("auto").getNodeValue());

                    if (auto) {
                        int initialDelay = Integer.valueOf(d.getAttributes().getNamedItem("initial_delay").getNodeValue());
                        int delay = Integer.valueOf(d.getAttributes().getNamedItem("delay").getNodeValue());

                        int limit = Integer.valueOf(d.getAttributes().getNamedItem("limit").getNodeValue());
                        if (limit < 0) { limit = 0; }

                        announcements.put(id, new Announcement(message, critical, auto, initialDelay, delay, limit));
                    }
                    else { announcements.put(id, new Announcement(message, critical)); }

                    id++;
                }
            }
        }
        catch (Exception e) {
            _log.warning("AnnouncementTable: Error loading from file:" + e.getMessage());
        }
        _log.info("AnnouncementTable: Loaded " + announcements.size() + " announcements.");
    }

    public void showAnnouncements(L2PcInstance activeChar, boolean autoOrNot) {
        for (Announcement announce : announcements.values()) {
            if (autoOrNot) { announce.reloadTask(); }
            else {
                if (announce.isAuto()) { continue; }
                activeChar.sendPacket(new CreatureSay(0, announce.isCritical() ? EChatType.CRITICAL_ANNOUNCE : EChatType.ANNOUNCEMENT, activeChar.getName(), announce.getMessage()));
            }
        }
    }

    public void listAnnouncements(L2PcInstance activeChar) {
        StringBuilder sb = new StringBuilder("<br>");
        if (announcements.isEmpty()) { sb.append("<tr><td>The XML file doesn't contain any content.</td></tr>"); }
        else {
            for (Entry<Integer, Announcement> entry : announcements.entrySet()) {
                int index = entry.getKey();
                Announcement announce = entry.getValue();

                StringUtil.append(sb, "<tr><td width=240>#", index, " - ", announce.getMessage(), "</td><td></td></tr><tr><td>Critical: ", announce.isCritical(), " | Auto: ", announce.isAuto(), "</td><td><button value=\"Delete\" action=\"bypass -h admin_announce del ", index, "\" width=65 height=19 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td></tr>");
            }
        }

        NpcHtmlMessage html = new NpcHtmlMessage(0);
        html.setHtml(HtmCache.getInstance().getHtmForce("data/html/admin/announce_list.htm"));
        html.replace("%announces%", sb.toString());
        activeChar.sendPacket(html);
    }

    public boolean addAnnouncement(String message, boolean critical, boolean auto, int initialDelay, int delay, int limit) {
        // Empty or null message.
        if (message == null || message.isEmpty()) { return false; }

        // Register announcement.
        if (auto) {
            announcements.put(announcements.size(), new Announcement(message, critical, auto, initialDelay, delay, limit));
        }
        else { announcements.put(announcements.size(), new Announcement(message, critical)); }

        // Regenerate the XML.
        regenerateXML();
        return true;
    }

    public void delAnnouncement(int index) {
        announcements.remove(index).stopTask();
        regenerateXML();
    }

    private void regenerateXML() {
        StringBuilder sb = new StringBuilder(HEADER);
        sb.append("<list> \n");
        for (Announcement announce : announcements.values()) {
            StringUtil.append(sb, "<announcement message=\"", announce.getMessage(), "\" critical=\"", announce.isCritical(), "\" auto=\"", announce.isAuto(), "\" initial_delay=\"", announce.getInitialDelay(), "\" delay=\"", announce.getDelay(), "\" limit=\"", announce.getLimit(), "\" /> \n");
        }
        sb.append("</list>");
        try (FileWriter fw = new FileWriter(new File(DATA_XML_ANNOUNCEMENTS_XML))) {
            fw.write(sb.toString());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class SingletonHolder {
        protected static final AnnouncementTable _instance = new AnnouncementTable();
    }
}