package net.sf.l2j;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import net.sf.l2j.commons.serialize.Serializer;
import net.sf.l2j.gameserver.playerpart.recipe.Recipe;
import net.sf.l2j.gameserver.playerpart.recipe.RecipeIngredient;
import net.sf.l2j.gameserver.playerpart.recipe.RecipesXmlFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Johnson / 23.07.2017
 */
public class XmlTransformator {

    public static void main(String... args) throws IOException {
        File file = new File("./data/xml/recipes.xml");
        XmlFile xmlFile = Serializer.MAPPER.readValue(file, XmlFile.class);
        RecipesXmlFile newXml = new RecipesXmlFile();

        for (RecipeOld recipeOld : xmlFile.list) {
            Recipe recipe = new Recipe();
            recipe.id = recipeOld.id;
            recipe.name = recipeOld.name;
            recipe.mana = recipeOld.mpCost;
            recipe.chance = recipeOld.successRate;
            recipe.isDwarven = recipeOld.recipe.type.equals("dwarven");
            recipe.level = recipeOld.recipe.level;

            recipe.product = recipeOld.production;
            recipe.ingredients = recipeOld.ingredient;

            recipe.recipeItemId = recipeOld.recipe.id;

            newXml.list.add(recipe);
        }

        Serializer.MAPPER.writeValue(new File("./data/xml/recipes_new.xml"), newXml);
    }

    @JsonRootName("list")
    private static class XmlFile {
        @JacksonXmlProperty(localName = "item")
        @JacksonXmlElementWrapper(useWrapping = false)
        public List<RecipeOld> list = new ArrayList<>();
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private static class RecipeOld {
        @JacksonXmlProperty(localName = "id", isAttribute = true)
        public Integer id;
        @JacksonXmlProperty(localName = "name", isAttribute = true)
        private String name;
        @JacksonXmlProperty(localName = "mpCost")
        private Integer mpCost;
        @JacksonXmlProperty(localName = "successRate")
        private Integer successRate;
        @JacksonXmlProperty(localName = "production")
        private RecipeIngredient production;
        @JacksonXmlProperty(localName = "ingredient")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<RecipeIngredient> ingredient = new ArrayList<>();
        @JacksonXmlProperty(localName = "recipe")
        private XmlRecipe recipe;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private static class XmlRecipe {
        @JacksonXmlProperty(localName = "id", isAttribute = true)
        public Integer id;
        @JacksonXmlProperty(localName = "level", isAttribute = true)
        public Integer level;
        @JacksonXmlProperty(localName = "type", isAttribute = true)
        private String type;
    }
}
