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
package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.datatables.RecipeTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance.PrivateStoreType;
import net.sf.l2j.gameserver.model.item.RecipeList;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.RecipeBookItemList;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;

public final class RequestRecipeBookDestroy extends L2GameClientPacket
{
	private int _recipeID;
	
	@Override
	protected void readImpl()
	{
		_recipeID = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		
		if (activeChar.getPrivateStoreType() == PrivateStoreType.MANUFACTURE)
		{
			activeChar.sendPacket(SystemMessageId.CANT_ALTER_RECIPEBOOK_WHILE_CRAFTING);
			return;
		}
		
		final RecipeList rp = RecipeTable.getInstance().getRecipeList(_recipeID);
		if (rp == null)
			return;
		
		activeChar.unregisterRecipeList(_recipeID);
		activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_BEEN_DELETED).addItemName(_recipeID));
		
		RecipeBookItemList response = new RecipeBookItemList(rp.isDwarvenRecipe(), activeChar.getMaxMp());
		if (rp.isDwarvenRecipe())
			response.addRecipes(activeChar.getDwarvenRecipeBook());
		else
			response.addRecipes(activeChar.getCommonRecipeBook());
		
		activeChar.sendPacket(response);
	}
}