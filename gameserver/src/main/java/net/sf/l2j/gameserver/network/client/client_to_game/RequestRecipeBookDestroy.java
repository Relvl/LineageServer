package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.RecipeBookItemList;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.playerpart.PrivateStoreType;
import net.sf.l2j.gameserver.playerpart.recipe.Recipe;
import net.sf.l2j.gameserver.playerpart.recipe.RecipeController;

public final class RequestRecipeBookDestroy extends L2GameClientPacket {
    private int recipeID;

    @Override
    protected void readImpl() {
        recipeID = readD();
    }

    @Override
    protected void runImpl() {
        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null) { return; }

        if (activeChar.getPrivateStoreType() == PrivateStoreType.MANUFACTURE) {
            activeChar.sendPacket(SystemMessageId.CANT_ALTER_RECIPEBOOK_WHILE_CRAFTING);
            return;
        }

        Recipe rp = RecipeController.getRecipe(recipeID);
        if (rp == null) { return; }

        activeChar.getRecipeController().unregisterRecipe(recipeID);
        activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_BEEN_DELETED).addItemName(recipeID));

        RecipeBookItemList response = new RecipeBookItemList(rp.isDwarvenRecipe(), activeChar.getMaxMp());
        response.addRecipes(rp.isDwarvenRecipe() ? activeChar.getRecipeController().getDwarvenRecipes() : activeChar.getRecipeController().getCommonRecipes());
        activeChar.sendPacket(response);

        activeChar.getInventory().addItem(EItemProcessPurpose.CRAFT, rp.recipeItemId, 1, activeChar, activeChar);
    }
}