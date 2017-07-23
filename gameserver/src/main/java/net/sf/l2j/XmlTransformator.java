package net.sf.l2j;

import net.sf.l2j.commons.serialize.Serializer;
import net.sf.l2j.gameserver.model.actor.instance.playerpart.recipe.RecipeNew;
import net.sf.l2j.gameserver.model.actor.instance.playerpart.recipe.RecipesXmlFile;

import java.io.File;
import java.io.IOException;

/**
 * @author Johnson / 23.07.2017
 */
public class XmlTransformator {

    public static void main(String... args) throws IOException {
        File file = new File("./data/xml/recipes.xml");
        RecipesXmlFile xmlFile = Serializer.MAPPER.readValue(file, RecipesXmlFile.class);

        for (RecipeNew xmlElement : xmlFile.list) {
        }
        Serializer.MAPPER.writeValue(new File("./data/xml/recipes_new.xml"), xmlFile);
    }
}
