package net.sf.l2j.gameserver.model.location;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * @author Johnson / 17.07.2017
 */
public class ChaoticLocation extends Location {

    @JacksonXmlProperty(localName = "isChaotic", isAttribute = true)
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private final boolean isChaotic;

    public ChaoticLocation() {
        super(0, 0, 0);
        isChaotic = false;
    }

    public ChaoticLocation(int x, int y, int z, boolean isChaotic) {
        super(x, y, z);
        this.isChaotic = isChaotic;
    }

    public boolean isChaotic() {
        return isChaotic;
    }
}
