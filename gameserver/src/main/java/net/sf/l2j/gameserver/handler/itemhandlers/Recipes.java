package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.playerpart.PrivateStoreType;
import net.sf.l2j.gameserver.playerpart.recipe.Recipe;
import net.sf.l2j.gameserver.playerpart.recipe.RecipeController;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

public class Recipes implements IItemHandler {
    @Override
    public void useItem(L2Playable playable, L2ItemInstance item, boolean forceUse) {
        if (!(playable instanceof L2PcInstance)) { return; }

        L2PcInstance activeChar = (L2PcInstance) playable;

        if (activeChar.isInCraftMode()) {
            activeChar.sendPacket(SystemMessageId.CANT_ALTER_RECIPEBOOK_WHILE_CRAFTING);
            return;
        }

        Recipe rp = RecipeController.getRecipeByItem(item.getItemId());
        if (rp == null) { return; }

        if (activeChar.getRecipeController().hasRecipe(rp.getRecipeId())) {
            activeChar.sendPacket(SystemMessageId.RECIPE_ALREADY_REGISTERED);
            return;
        }

        if (rp.isDwarvenRecipe()) {
            if (activeChar.getRecipeController().hasDwarvenCraft()) {
                if (activeChar.getPrivateStoreType() == PrivateStoreType.MANUFACTURE) {
                    activeChar.sendPacket(SystemMessageId.CANT_ALTER_RECIPEBOOK_WHILE_CRAFTING);
                }
                else if (rp.getLevel() > activeChar.getRecipeController().getDwarvenCraft()) {
                    activeChar.sendPacket(SystemMessageId.CREATE_LVL_TOO_LOW_TO_REGISTER);
                }
                else if (activeChar.getRecipeController().getDwarvenRecipes().size() >= activeChar.getRecipeController().getDwarfRecipeLimit()) {
                    activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.UP_TO_S1_RECIPES_CAN_REGISTER).addNumber(activeChar.getRecipeController().getDwarfRecipeLimit()));
                }
                else {
                    activeChar.getRecipeController().registerRecipe(rp, true);
                    activeChar.destroyItem(EItemProcessPurpose.CONSUME, item.getObjectId(), 1, null, false);
                    activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_ADDED).addItemName(item));
                    activeChar.getRecipeController().requestBookOpen(true);
                }
            }
            else {
                activeChar.sendPacket(SystemMessageId.CANT_REGISTER_NO_ABILITY_TO_CRAFT);
            }
        }
        else {
            if (activeChar.getRecipeController().hasCommonCraft()) {
                if (activeChar.getPrivateStoreType() == PrivateStoreType.MANUFACTURE) {
                    activeChar.sendPacket(SystemMessageId.CANT_ALTER_RECIPEBOOK_WHILE_CRAFTING);
                }
                else if (rp.getLevel() > activeChar.getRecipeController().getCommonCraft()) {
                    activeChar.sendPacket(SystemMessageId.CREATE_LVL_TOO_LOW_TO_REGISTER);
                }
                else if (activeChar.getRecipeController().getCommonRecipes().size() >= activeChar.getRecipeController().getCommonRecipeLimit()) {
                    activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.UP_TO_S1_RECIPES_CAN_REGISTER).addNumber(activeChar.getRecipeController().getCommonRecipeLimit()));
                }
                else {
                    activeChar.getRecipeController().registerRecipe(rp, false);
                    activeChar.destroyItem(EItemProcessPurpose.CONSUME, item.getObjectId(), 1, null, false);
                    activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_ADDED).addItemName(item));
                    activeChar.getRecipeController().requestBookOpen(false);
                }
            }
            else {
                activeChar.sendPacket(SystemMessageId.CANT_REGISTER_NO_ABILITY_TO_CRAFT);
            }
        }
    }
}