package net.sf.l2j.gameserver.model.location;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import net.sf.l2j.commons.DefaultConstructor;
import net.sf.l2j.commons.database.annotation.OrmTypeParam;

public class HeadedLocation extends Location {

    @JacksonXmlProperty(localName = "heading", isAttribute = true)
    @JsonInclude(Include.NON_DEFAULT)
    @OrmTypeParam(3)
    protected volatile int heading;

    @DefaultConstructor
    public HeadedLocation() { }

    public HeadedLocation(int posX, int posY, int posZ, int heading) {
        super(posX, posY, posZ);
        this.heading = heading;
    }

    public int getHeading() { return heading; }
}