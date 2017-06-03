package johnson.loginserver.config;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

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

    @JacksonXmlProperty(localName = "address")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<BannedAddressEntry> addresses;

    public void addBanEntry(String ip, LocalDateTime expire, String initiator, String reason) {
        if (addresses == null) {
            addresses = new ArrayList<>();
        }
        addresses.add(new BannedAddressEntry(ip, expire, initiator, reason));
    }

    public List<BannedAddressEntry> getAddresses() {
        return addresses;
    }

    private static class BannedAddressEntry {
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

        public BannedAddressEntry() {
        }

        public BannedAddressEntry(String ip, LocalDateTime expired, String initiator, String reason) {
            this.ip = ip;
            this.expired = expired;
            this.initiator = initiator;
            this.reason = reason;
        }
    }
}
