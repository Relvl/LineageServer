package net.sf.l2j.gameserver.model.zone.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Johnson / 17.07.2017
 */
@JsonRootName("list")
public class ZoneXmlFile {

    @JacksonXmlProperty(localName = "zone")
    @JacksonXmlElementWrapper(useWrapping = false)
    private final List<ZoneElement> zoneElements = new ArrayList<>();

    @Override
    public String toString() {
        return "ZoneXmlFile{" + zoneElements + '}';
    }

    public List<ZoneElement> getZones() {
        return zoneElements;
    }

}
