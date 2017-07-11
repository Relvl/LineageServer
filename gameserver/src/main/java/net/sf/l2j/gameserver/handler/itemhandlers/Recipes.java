/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.datatables.RecipeTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance.PrivateStoreType;
import net.sf.l2j.gameserver.model.item.RecipeList;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;

public class Recipes implements IItemHandler
{
	@Override
	public void useItem(L2Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof L2PcInstance))
			return;
		
		final L2PcInstance activeChar = (L2PcInstance) playable;
		
		if (activeChar.isInCraftMode())
		{
			activeChar.sendPacket(SystemMessageId.CANT_ALTER_RECIPEBOOK_WHILE_CRAFTING);
			return;
		}
		
		final RecipeList rp = RecipeTable.getInstance().getRecipeByItemId(item.getItemId());
		if (rp == null)
			return;
		
		if (activeChar.hasRecipeList(rp.getId()))
		{
			activeChar.sendPacket(SystemMessageId.RECIPE_ALREADY_REGISTERED);
			return;
		}
		
		if (rp.isDwarvenRecipe())
		{
			if (activeChar.hasDwarvenCraft())
			{
				if (activeChar.getPrivateStoreType() == PrivateStoreType.MANUFACTURE)
					activeChar.sendPacket(SystemMessageId.CANT_ALTER_RECIPEBOOK_WHILE_CRAFTING);
				else if (rp.getLevel() > activeChar.getDwarvenCraft())
					activeChar.sendPacket(SystemMessageId.CREATE_LVL_TOO_LOW_TO_REGISTER);
				else if (activeChar.getDwarvenRecipeBook().size() >= activeChar.getDwarfRecipeLimit())
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.UP_TO_S1_RECIPES_CAN_REGISTER).addNumber(activeChar.getDwarfRecipeLimit()));
				else
				{
					activeChar.registerDwarvenRecipeList(rp);
					activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_ADDED).addItemName(item));
				}
			}
			else
				activeChar.sendPacket(SystemMessageId.CANT_REGISTER_NO_ABILITY_TO_CRAFT);
		}
		else
		{
			if (activeChar.hasCommonCraft())
			{
				if (activeChar.getPrivateStoreType() == PrivateStoreType.MANUFACTURE)
					activeChar.sendPacket(SystemMessageId.CANT_ALTER_RECIPEBOOK_WHILE_CRAFTING);
				else if (rp.getLevel() > activeChar.getCommonCraft())
					activeChar.sendPacket(SystemMessageId.CREATE_LVL_TOO_LOW_TO_REGISTER);
				else if (activeChar.getCommonRecipeBook().size() >= activeChar.getCommonRecipeLimit())
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.UP_TO_S1_RECIPES_CAN_REGISTER).addNumber(activeChar.getCommonRecipeLimit()));
				else
				{
					activeChar.registerCommonRecipeList(rp);
					activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_ADDED).addItemName(item));
				}
			}
			else
				activeChar.sendPacket(SystemMessageId.CANT_REGISTER_NO_ABILITY_TO_CRAFT);
		}
	}
}