package net.sf.l2j.gameserver.model.actor.instance.playerpart.recipe;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import net.sf.l2j.commons.serialize.converter.IntegerNot100OnlyConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Johnson / 23.07.2017
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RecipeNew {
    @JacksonXmlProperty(localName = "id", isAttribute = true)
    public Integer id;
    @JacksonXmlProperty(localName = "isDwarven", isAttribute = true)
    public Boolean isDwarven;
    @JacksonXmlProperty(localName = "level", isAttribute = true)
    public Integer level;
    @JacksonXmlProperty(localName = "mana", isAttribute = true)
    public Integer mana;
    @JacksonXmlProperty(localName = "chance", isAttribute = true)
    @JsonSerialize(converter = IntegerNot100OnlyConverter.class)
    public Integer chance = 100;
    @JacksonXmlProperty(localName = "name", isAttribute = true)
    public String name;

    @JacksonXmlProperty(localName = "ingredient")
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<RecipeIngredient> ingredients = new ArrayList<>();

    @JacksonXmlProperty(localName = "production")
    @JacksonXmlElementWrapper(useWrapping = false)
    public RecipeIngredient product;
}
