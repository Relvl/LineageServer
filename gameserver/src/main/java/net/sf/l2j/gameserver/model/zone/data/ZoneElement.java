package net.sf.l2j.gameserver.model.zone.data;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import net.sf.l2j.gameserver.model.location.ChaoticLocation;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.zone.EZoneShape;
import net.sf.l2j.gameserver.model.zone.EZoneType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Johnson / 17.07.2017
 */
public class ZoneElement {
    @JacksonXmlProperty(localName = "stat")
    @JacksonXmlElementWrapper(useWrapping = false)
    private final List<ZoneStatElement> stats = new ArrayList<>();

    @JacksonXmlProperty(localName = "spawn")
    @JacksonXmlElementWrapper(useWrapping = false)
    private final List<ChaoticLocation> spawns = new ArrayList<>();

    @JacksonXmlProperty(localName = "node")
    @JacksonXmlElementWrapper(useWrapping = false)
    private Location[] nodes;

    @JacksonXmlProperty(localName = "id", isAttribute = true)
    private Integer id;
    @JacksonXmlProperty(localName = "name", isAttribute = true)
    private String name;
    @JacksonXmlProperty(localName = "type", isAttribute = true)
    private EZoneType type;
    @JacksonXmlProperty(localName = "shape", isAttribute = true)
    private EZoneShape shape;
    @JacksonXmlProperty(localName = "minZ", isAttribute = true)
    private Integer minZ;
    @JacksonXmlProperty(localName = "maxZ", isAttribute = true)
    private Integer maxZ;
    @JacksonXmlProperty(localName = "rad", isAttribute = true)
    private Integer rad;
    @JacksonXmlProperty(localName = "comment", isAttribute = true)
    private String comment;

    public Integer getId() { return id; }

    public String getName() { return name; }

    public EZoneType getType() { return type; }

    public EZoneShape getShape() { return shape; }

    public Integer getMinZ() { return minZ; }

    public Integer getMaxZ() { return maxZ; }

    public Integer getRadius() { return rad; }

    public Location[] getNodes() { return nodes; }

    public List<ZoneStatElement> getStats() { return stats; }

    public List<ChaoticLocation> getSpawns() { return spawns; }

    public String getComment() { return comment; }
}
