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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Маппинг списка заблокированных IP адресов.
 *
 * @author Johnson / 31.05.2017
 */
@JacksonXmlRootElement(localName = "BannedIpAddresses")
public class BannedIpAddresses {
    private static final Logger LOGGER = LoggerFactory.getLogger(BannedIpAddresses.class);
    private static final File CONFIG_FILE = new File("./config/banned.ips.xml");

    @JacksonXmlProperty(localName = "address")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<BannedAddressEntry> addresses;

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

    public void addBanEntry(String ip, LocalDateTime expire, String initiator, String reason) {
        if (addresses == null) {
            addresses = new ArrayList<>();
        }
        addresses.add(new BannedAddressEntry(ip, expire, initiator, reason));
    }

    public List<BannedAddressEntry> getAddresses() {
        return addresses;
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
