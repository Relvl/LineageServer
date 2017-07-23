package net.sf.l2j.gameserver.model.actor.instance.playerpart.recipe;

import com.fasterxml.jackson.annotation.JsonInclude;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Recipe {
    private final List<IntIntHolder> ingredients = new ArrayList<>();
    private final int id;
    private final int level;
    private final int recipeId;
    private final String name;
    private final int chance;
    private final int mana;
    private final int itemId;
    private final int count;
    private final boolean isDwarvenRecipe;

    public Recipe(int id, int level, int recipeId, String name, int chance, int mana, int itemId, int count, boolean isDwarvenRecipe) {
        this.id = id;
        this.level = level;
        this.recipeId = recipeId;
        this.name = name;
        this.chance = chance;
        this.mana = mana;
        this.itemId = itemId;
        this.count = count;
        this.isDwarvenRecipe = isDwarvenRecipe;
    }

    public void addNeededRecipePart(IntIntHolder recipe) {
        ingredients.add(recipe);
    }

    public int getId() { return id; }

    public int getLevel() { return level; }

    public int getRecipeId() { return recipeId; }

    public int getChance() { return chance; }

    public int getMana() { return mana; }

    public boolean isConsumable() {
        // Soulshots, Spiritshots, Bss, Arrows.
        return ((itemId >= 1463 && itemId <= 1467) || (itemId >= 2509 && itemId <= 2514) || (itemId >= 3947 && itemId <= 3952) || (itemId >= 1341 && itemId <= 1345));
    }

    public int getItemId() { return itemId; }

    public int getCount() { return count; }

    public boolean isDwarvenRecipe() { return isDwarvenRecipe; }

    public List<IntIntHolder> getNeededRecipeParts() { return ingredients; }

}