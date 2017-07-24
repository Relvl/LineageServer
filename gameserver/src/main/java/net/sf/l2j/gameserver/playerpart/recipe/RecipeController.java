package net.sf.l2j.gameserver.playerpart.recipe;

import net.sf.l2j.Config;
import net.sf.l2j.commons.database.CallException;
import net.sf.l2j.commons.database.EModify;
import net.sf.l2j.commons.serialize.Serializer;
import net.sf.l2j.gameserver.model.L2ShortCut;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.skill.SkillConst;
import net.sf.l2j.gameserver.playerpart.recipe.PlayerRecipesLoadCall.RecipeRow;
import net.sf.l2j.gameserver.network.client.game_to_client.RecipeBookItemList;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Johnson / 22.07.2017
 */
@SuppressWarnings("ClassHasNoToStringMethod")
public final class RecipeController {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeController.class);
    private static final File RECIPES_FILE = new File("./data/xml/recipes_new.xml");

    private static Map<Integer, Recipe> recipes = new HashMap<>();

    private final L2PcInstance player;
    private final Map<Integer, Recipe> commonRecipes = new HashMap<>();
    private final Map<Integer, Recipe> dwarvenRecipes = new HashMap<>();
    private final PlayerRecipesModifyCall modifyCall;

    public RecipeController(L2PcInstance player) {
        this.player = player;
        this.modifyCall = new PlayerRecipesModifyCall(player.getObjectId());
    }

    public int getDwarfRecipeLimit() { return Config.DWARF_RECIPE_LIMIT + (int) player.getStat().calcStat(Stats.REC_D_LIM, 0, null, null); }

    public int getCommonRecipeLimit() { return Config.COMMON_RECIPE_LIMIT + (int) player.getStat().calcStat(Stats.REC_C_LIM, 0, null, null); }

    public void reload() {
        try (PlayerRecipesLoadCall call = new PlayerRecipesLoadCall(player.getObjectId())) {
            call.execute();

            commonRecipes.clear();
            dwarvenRecipes.clear();

            for (RecipeRow row : call.getRecipes()) {
                Recipe recipe = getRecipe(row.getRecipeId());
                if (recipe != null) {
                    (recipe.isDwarvenRecipe() ? dwarvenRecipes : commonRecipes).put(recipe.getRecipeId(), recipe);
                }
            }
        }
        catch (CallException e) {
            LOGGER.error("Cannot load recipe list for player {}", player.getName(), e);
        }
    }

    public void requestBookOpen(boolean isDwarvenCraft) {
        RecipeBookItemList response = new RecipeBookItemList(isDwarvenCraft, player.getMaxMp());
        response.addRecipes(isDwarvenCraft ? dwarvenRecipes.values() : commonRecipes.values());
        player.sendPacket(response);
    }

    public Collection<Recipe> getCommonRecipes() { return commonRecipes.values(); }

    public Collection<Recipe> getDwarvenRecipes() { return dwarvenRecipes.values(); }

    public boolean hasRecipe(Integer id) {
        return commonRecipes.containsKey(id) || dwarvenRecipes.containsKey(id);
    }

    public void registerRecipe(Recipe recipe, boolean dwarven) {
        try {
            modifyCall.setRecipeId(recipe.getRecipeId());
            modifyCall.setModify(EModify.ADD);
            modifyCall.execute();

            (dwarven ? dwarvenRecipes : commonRecipes).put(recipe.getRecipeId(), recipe);
        }
        catch (CallException e) {
            LOGGER.error("Cannot store registered recipe {} for player {}", recipe.getRecipeId(), player.getObjectId(), e);
        }
    }

    public void unregisterRecipe(Integer id) {
        try {
            modifyCall.setRecipeId(id);
            modifyCall.setModify(EModify.DELETE);
            modifyCall.execute();

            commonRecipes.remove(id);
            dwarvenRecipes.remove(id);
            for (L2ShortCut sc : player.getAllShortCuts()) {
                if (sc != null && sc.getId() == id && sc.getType() == L2ShortCut.TYPE_RECIPE) {
                    player.deleteShortCut(sc.getSlot(), sc.getPage());
                }
            }
        }
        catch (CallException e) {
            LOGGER.error("Cannot store unregistered recipe {} for player {}", id, player.getObjectId(), e);
        }

    }

    public boolean hasDwarvenCraft() { return player.getSkillLevel(SkillConst.SKILL_CREATE_DWARVEN) >= 1; }

    public int getDwarvenCraft() { return player.getSkillLevel(SkillConst.SKILL_CREATE_DWARVEN); }

    public boolean hasCommonCraft() { return player.getSkillLevel(SkillConst.SKILL_CREATE_COMMON) >= 1; }

    public int getCommonCraft() { return player.getSkillLevel(SkillConst.SKILL_CREATE_COMMON); }

    public int getCraftSkillLevel(boolean dwarven) { return dwarven ? getDwarvenCraft() : getCommonCraft(); }

    public void doManufacture(Integer recipeId, boolean privateStore, L2PcInstance requester) {
        Recipe recipe = getRecipe(recipeId);
        if (recipe == null || !hasRecipe(recipeId)) {
            Util.handleIllegalPlayerAction(player, player.getName() + " of account " + player.getAccountName() + " sent a wrong recipe id.", Config.DEFAULT_PUNISH);
            return;
        }
        recipe.doManufacture(player, privateStore, requester);
    }

    // region RECIPES TABLE
    public static void loadRecipes() {
        LOGGER.info("Load recipes...");
        try {
            Map<Integer, Recipe> recipeNewMap = new HashMap<>();
            RecipesXmlFile xmlFile = Serializer.MAPPER.readValue(RECIPES_FILE, RecipesXmlFile.class);
            for (Recipe recipe : xmlFile.list) {
                if (recipe.getIngredients() == null || recipe.getIngredients().isEmpty()) {
                    LOGGER.warn("Ingridients empty for recipe {}", recipe.getRecipeId());
                    continue;
                }
                recipe.makeInfo();
                recipeNewMap.put(recipe.id, recipe);
            }
            recipes = recipeNewMap;
        }
        catch (IOException e) {
            LOGGER.error("Cannot load recipes", e);
        }
    }

    public static Recipe getRecipe(Integer id) { return recipes.get(id); }

    public static Recipe getRecipeByItem(Integer itemId) {
        for (Recipe recipe : recipes.values()) {
            if (recipe.getRecipeItemId().equals(itemId)) {
                return recipe;
            }
        }
        return null;
    }

    // endregion RECIPES TABLE

}
