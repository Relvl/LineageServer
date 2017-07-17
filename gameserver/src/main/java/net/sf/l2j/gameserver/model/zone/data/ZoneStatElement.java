package net.sf.l2j.gameserver.model.zone.data;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * @author Johnson / 17.07.2017
 */
public class ZoneStatElement {
    @JacksonXmlProperty(localName = "name", isAttribute = true)
    private String name;
    @JacksonXmlProperty(localName = "val", isAttribute = true)
    private String val;

    public String getName() { return name; }

    public String getVal() { return val; }
}
