package net.sf.l2j.commons.config;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * @author Johnson / 15.07.2017
 */
public class MMOCoreConfigImpl {
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

    @Override
    public String toString() {
        return "MMOCoreConfigImpl{" +
                "selectorSleepTime=" + selectorSleepTime +
                ", maxSendPerPass=" + maxSendPerPass +
                ", maxReadPerPass=" + maxReadPerPass +
                ", helperBufferCount=" + helperBufferCount +
                '}';
    }
}
