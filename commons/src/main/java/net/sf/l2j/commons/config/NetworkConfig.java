package net.sf.l2j.commons.config;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * @author Johnson / 15.07.2017
 */
public class NetworkConfig {
    /**  */
    @JacksonXmlProperty(localName = "host", isAttribute = true)
    public String host = "*";
    /**  */
    @JacksonXmlProperty(localName = "port", isAttribute = true)
    public Integer port = 0;

    /**  */
    @JacksonXmlProperty(localName = "communicationHost")
    public String communicationHost = "127.0.0.1";
    /**  */
    @JacksonXmlProperty(localName = "communicationPort")
    public Integer communicationPort = 9013;
    /**  */
    @JacksonXmlProperty(localName = "communicationProtocol")
    public int communicationProtocol = 0x0102;

    /**  */
    @JacksonXmlProperty(localName = "SelectorSleepTime")
    public Integer selectorSleepTime = 20;
    /**  */
    @JacksonXmlProperty(localName = "MaxSendPerPass")
    public Integer maxSendPerPass = 12;
    /**  */
    @JacksonXmlProperty(localName = "MaxReadPerPass")
    public Integer maxReadPerPass = 12;
    /**  */
    @JacksonXmlProperty(localName = "HelperBufferCount")
    public Integer helperBufferCount = 20;

    public NetworkConfig() {
    }

    public NetworkConfig(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public String toString() {
        return "NetworkConfig{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", communicationHost='" + communicationHost + '\'' +
                ", communicationPort=" + communicationPort +
                ", communicationProtocol=" + communicationProtocol +
                ", selectorSleepTime=" + selectorSleepTime +
                ", maxSendPerPass=" + maxSendPerPass +
                ", maxReadPerPass=" + maxReadPerPass +
                ", helperBufferCount=" + helperBufferCount +
                '}';
    }
}
