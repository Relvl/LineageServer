package net.sf.l2j.commons.config;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * @author Johnson / 15.07.2017
 */
public class FloodProtectionConfig {
    /**  */
    @JacksonXmlProperty(localName = "Enabled", isAttribute = true)
    public Boolean enabled = true;
    /**  */
    @JacksonXmlProperty(localName = "FastConnectionLimit")
    public Integer fastConnectionLimit = 15;
    /**  */
    @JacksonXmlProperty(localName = "FastConnectionTime")
    public Integer fastConnectionTime = 350;
    /**  */
    @JacksonXmlProperty(localName = "NormalConnectionTime")
    public Integer normalConnectionTime = 700;
    /**  */
    @JacksonXmlProperty(localName = "MaxConnectionsPerIP")
    public Integer maxConnectionsPerIP = 10;

    @Override
    public String toString() {
        return "FloodProtectionConfig{" +
                "enabled=" + enabled +
                ", fastConnectionLimit=" + fastConnectionLimit +
                ", fastConnectionTime=" + fastConnectionTime +
                ", normalConnectionTime=" + normalConnectionTime +
                ", maxConnectionsPerIP=" + maxConnectionsPerIP +
                '}';
    }
}
