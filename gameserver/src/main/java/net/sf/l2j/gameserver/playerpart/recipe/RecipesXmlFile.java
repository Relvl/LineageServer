package net.sf.l2j.gameserver.playerpart.recipe;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Johnson / 23.07.2017
 */
@JsonRootName("list")
public class RecipesXmlFile {
    @JacksonXmlProperty(localName = "item")
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<Recipe> list = new ArrayList<>();
}
