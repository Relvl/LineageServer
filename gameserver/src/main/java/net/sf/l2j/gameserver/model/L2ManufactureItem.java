package net.sf.l2j.gameserver.model;

import net.sf.l2j.gameserver.model.actor.instance.playerpart.recipe.RecipeController;

public class L2ManufactureItem {
    private final int recipeId;
    private final int cost;
    private final boolean isDwarven;

    public L2ManufactureItem(int recipeId, int cost) {
        this.recipeId = recipeId;
        this.cost = cost;
        isDwarven = RecipeController.getRecipe(this.recipeId).isDwarvenRecipe();
    }

    public int getRecipeId() { return recipeId; }

    public int getCost() { return cost; }

    public boolean isDwarven() { return isDwarven; }
}