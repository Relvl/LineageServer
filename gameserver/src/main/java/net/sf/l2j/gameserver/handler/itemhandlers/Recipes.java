package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.playerpart.PrivateStoreType;
import net.sf.l2j.gameserver.playerpart.recipe.Recipe;
import net.sf.l2j.gameserver.playerpart.recipe.RecipeController;

public class Recipes implements IItemHandler {
    @Override
    public void useItem(L2Playable playable, L2ItemInstance item, boolean forceUse) {
        if (!(playable instanceof L2PcInstance)) { return; }

        L2PcInstance player = (L2PcInstance) playable;

        if (player.isInCraftMode()) {
            player.sendPacket(SystemMessageId.CANT_ALTER_RECIPEBOOK_WHILE_CRAFTING);
            return;
        }

        Recipe rp = RecipeController.getRecipeByItem(item.getItemId());
        if (rp == null) { return; }

        if (player.getRecipeController().hasRecipe(rp.getRecipeId())) {
            player.sendPacket(SystemMessageId.RECIPE_ALREADY_REGISTERED);
            return;
        }

        if (rp.isDwarvenRecipe()) {
            if (player.getRecipeController().hasDwarvenCraft()) {
                if (player.getPrivateStoreType() == PrivateStoreType.MANUFACTURE) {
                    player.sendPacket(SystemMessageId.CANT_ALTER_RECIPEBOOK_WHILE_CRAFTING);
                }
                else if (rp.getLevel() > player.getRecipeController().getDwarvenCraft()) {
                    player.sendPacket(SystemMessageId.CREATE_LVL_TOO_LOW_TO_REGISTER);
                }
                else if (player.getRecipeController().getDwarvenRecipes().size() >= player.getRecipeController().getDwarfRecipeLimit()) {
                    player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.UP_TO_S1_RECIPES_CAN_REGISTER).addNumber(player.getRecipeController().getDwarfRecipeLimit()));
                }
                else {
                    player.getRecipeController().registerRecipe(rp, true);
                    player.getInventory().destroyItem(EItemProcessPurpose.CONSUME, item, 1, null, false);
                    player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_ADDED).addItemName(item));
                    player.getRecipeController().requestBookOpen(true);
                }
            }
            else {
                player.sendPacket(SystemMessageId.CANT_REGISTER_NO_ABILITY_TO_CRAFT);
            }
        }
        else {
            if (player.getRecipeController().hasCommonCraft()) {
                if (player.getPrivateStoreType() == PrivateStoreType.MANUFACTURE) {
                    player.sendPacket(SystemMessageId.CANT_ALTER_RECIPEBOOK_WHILE_CRAFTING);
                }
                else if (rp.getLevel() > player.getRecipeController().getCommonCraft()) {
                    player.sendPacket(SystemMessageId.CREATE_LVL_TOO_LOW_TO_REGISTER);
                }
                else if (player.getRecipeController().getCommonRecipes().size() >= player.getRecipeController().getCommonRecipeLimit()) {
                    player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.UP_TO_S1_RECIPES_CAN_REGISTER).addNumber(player.getRecipeController().getCommonRecipeLimit()));
                }
                else {
                    player.getRecipeController().registerRecipe(rp, false);
                    player.getInventory().destroyItem(EItemProcessPurpose.CONSUME, item, 1, null, false);
                    player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_ADDED).addItemName(item));
                    player.getRecipeController().requestBookOpen(false);
                }
            }
            else {
                player.sendPacket(SystemMessageId.CANT_REGISTER_NO_ABILITY_TO_CRAFT);
            }
        }
    }
}