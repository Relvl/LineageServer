package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.playerpart.recipe.Recipe;
import net.sf.l2j.gameserver.playerpart.recipe.RecipeController;

public class RecipeItemMakeInfo extends L2GameServerPacket {
    private final int recipeId;
    private final L2PcInstance player;
    private final int status;

    public RecipeItemMakeInfo(int recipeId, L2PcInstance player, int status) {
        this.recipeId = recipeId;
        this.player = player;
        this.status = status;
    }

    public RecipeItemMakeInfo(int id, L2PcInstance player) {
        recipeId = id;
        this.player = player;
        status = -1;
    }

    @Override
    protected final void writeImpl() {
        Recipe recipe = RecipeController.getRecipe(recipeId);
        if (recipe != null) {
            writeC(0xD7);
            writeD(recipeId);
            writeD(recipe.isDwarvenRecipe() ? 0 : 1);
            writeD((int) player.getCurrentMp());
            writeD(player.getMaxMp());
            writeD(status);
        }
    }
}