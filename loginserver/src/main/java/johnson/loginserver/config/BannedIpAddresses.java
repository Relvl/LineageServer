package johnson.loginserver.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import net.sf.l2j.commons.DefaultConstructor;
import net.sf.l2j.commons.serialize.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Маппинг списка заблокированных IP адресов.
 *
 * @author Johnson / 31.05.2017
 */
@JacksonXmlRootElement(localName = "BannedIpAddresses")
public class BannedIpAddresses {
    private static final Logger LOGGER = LoggerFactory.getLogger(BannedIpAddresses.class);
    private static final File CONFIG_FILE = new File("./config/banned.ips.xml");

    @JsonIgnore
    private final Map<InetAddress, BannedAddressEntry> entries = new HashMap<>();

    @JacksonXmlProperty(localName = "address")
    @JacksonXmlElementWrapper(useWrapping = false)
    private Collection<BannedAddressEntry> getBannedList() {
        return entries.values();
    }

    @JacksonXmlProperty(localName = "address")
    @JacksonXmlElementWrapper(useWrapping = false)
    private void setBannedList(Iterable<BannedAddressEntry> list) {
        boolean needToResave = false;
        for (BannedAddressEntry entry : list) {
            try {
                if (entry.getExpired().isBefore(LocalDateTime.now())) {
                    needToResave = true;
                    LOGGER.info("Removing obsolete IP ban entry: {}", entry.getIp());
                    continue;
                }
                entries.put(InetAddress.getByName(entry.getIp()), entry);
            } catch (UnknownHostException e) {
                LOGGER.error("Cannot resolve IP address {} while loadin banlist", entry.getIp(), e);
            }
        }
        if (needToResave) { save(); }
    }

    public void addBannedIp(InetAddress ip, long duration, String initiator, String reason) {
        if (ip == null) { return; }
        entries.put(ip, new BannedAddressEntry(ip.getHostAddress(), LocalDateTime.now().plus(duration, ChronoUnit.MILLIS), initiator, reason));
        save();
    }

    public void removeBannedIp(String ip) {
        if (ip == null || ip.isEmpty()) { return; }
        try {
            entries.remove(InetAddress.getByName(ip));
            save();
        } catch (UnknownHostException e) {
            LOGGER.error("Cannot resolve IP address {} wile removing from banlist", ip, e);
        }
    }

    public boolean isIpBanned(InetAddress address) {
        if (entries.containsKey(address)) {
            BannedAddressEntry entry = entries.get(address);
            if (entry.getExpired().isAfter(LocalDateTime.now())) {
                return true;
            }
            entries.remove(address);
            save();
        }
        return false;
    }

    @JsonIgnore
    public int getSize() {
        return entries.size();
    }

    @JsonIgnore
    public static BannedIpAddresses load() {
        try {
            return Serializer.MAPPER.readValue(CONFIG_FILE, BannedIpAddresses.class);
        } catch (IOException e) {
            LOGGER.error("Failed to load {}", CONFIG_FILE, e);
            return new BannedIpAddresses();
        }
    }

    @JsonIgnore
    public void save() {
        try {
            Serializer.MAPPER.writeValue(CONFIG_FILE, this);
        } catch (IOException e) {
            LOGGER.error("Failed to save {}", CONFIG_FILE, e);
        }
    }

    @Deprecated
    public Collection<BannedAddressEntry> getAddresses() {
        return entries.values();
    }

    public static class BannedAddressEntry {
        /**  */
        @JacksonXmlProperty(localName = "ip", isAttribute = true)
        private String ip;
        /**  */
        @JacksonXmlProperty(localName = "expired", isAttribute = true)
        private LocalDateTime expired;
        /**  */
        @JacksonXmlProperty(localName = "initiator", isAttribute = true)
        private String initiator;
        /**  */
        @JacksonXmlProperty(localName = "reason", isAttribute = true)
        private String reason;

        @DefaultConstructor
        public BannedAddressEntry() { }

        public BannedAddressEntry(String ip, LocalDateTime expired, String initiator, String reason) {
            this.ip = ip;
            this.expired = expired;
            this.initiator = initiator;
            this.reason = reason;
        }

        public String getIp() {
            return ip;
        }

        public LocalDateTime getExpired() {
            return expired;
        }
    }
}
