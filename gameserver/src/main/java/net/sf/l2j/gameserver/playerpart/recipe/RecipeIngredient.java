package net.sf.l2j.gameserver.playerpart.recipe;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * @author Johnson / 23.07.2017
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RecipeIngredient {
    @JacksonXmlProperty(localName = "id", isAttribute = true)
    public Integer id;
    @JacksonXmlProperty(localName = "count", isAttribute = true)
    public Integer count;
}
