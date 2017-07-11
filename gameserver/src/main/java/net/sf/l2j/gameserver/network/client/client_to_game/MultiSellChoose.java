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

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.MultisellData;
import net.sf.l2j.gameserver.model.L2Augmentation;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Armor;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.model.itemcontainer.PcInventory;
import net.sf.l2j.gameserver.model.multisell.Entry;
import net.sf.l2j.gameserver.model.multisell.Ingredient;
import net.sf.l2j.gameserver.model.multisell.ListContainer;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.ItemList;
import net.sf.l2j.gameserver.network.client.game_to_client.StatusUpdate;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.util.FloodProtectors;
import net.sf.l2j.gameserver.util.FloodProtectors.Action;

public class MultiSellChoose extends L2GameClientPacket
{
	private int _listId;
	private int _entryId;
	private int _amount;
	private int _enchantment;
	private int _transactionTax; // local handling of taxation
	
	@Override
	protected void readImpl()
	{
		_listId = readD();
		_entryId = readD();
		_amount = readD();
		_enchantment = _entryId % 100000;
		_entryId = _entryId / 100000;
		_transactionTax = 0;
	}
	
	@Override
	public void runImpl()
	{
		if (!FloodProtectors.performAction(getClient(), Action.MULTISELL))
			return;
		
		final L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;
		
		if (_amount < 1 || _amount > 9999)
			return;
		
		// Verify first if the player can interact with the trader.
		L2Npc merchant = (player.getTarget() instanceof L2Npc) ? (L2Npc) player.getTarget() : null;
		if (merchant == null || !merchant.canInteract(player))
			return;
		
		ListContainer list = MultisellData.getInstance().getList(_listId);
		if (list == null)
			return;
		
		for (Entry entry : list.getEntries())
		{
			if (entry.getEntryId() == _entryId)
			{
				doExchange(player, merchant, entry, list.getApplyTaxes(), list.getMaintainEnchantment(), _enchantment);
				return;
			}
		}
	}
	
	private void doExchange(L2PcInstance player, L2Npc merchant, Entry templateEntry, boolean applyTaxes, boolean maintainEnchantment, int enchantment)
	{
		final PcInventory inv = player.getInventory();
		
		Entry entry = prepareEntry(merchant, templateEntry, applyTaxes, maintainEnchantment, enchantment);
		
		/**
		 * Checks if the amount to purchase is exceeding the inventory slots or weight limit and returns a message to the player.
		 */
		int slots = 0;
		int weight = 0;
		for (Ingredient e : entry.getProducts())
		{
			int id = e.getItemId();
			if (id < 0)
				continue;
			
			Item template = ItemTable.getInstance().getTemplate(id);
			if (template == null)
				continue;
			
			if (!template.isStackable())
				slots += e.getItemCount() * _amount;
			else if (player.getInventory().getItemByItemId(id) == null)
				slots++;
			
			weight += e.getItemCount() * _amount * template.getWeight();
		}
		
		if (!inv.validateWeight(weight))
		{
			player.sendPacket(SystemMessageId.WEIGHT_LIMIT_EXCEEDED);
			return;
		}
		
		if (!inv.validateCapacity(slots))
		{
			player.sendPacket(SystemMessageId.SLOTS_FULL);
			return;
		}
		
		// Generate a list of distinct ingredients and counts in order to check if the correct item-counts
		// are possessed by the player
		List<Ingredient> _ingredientsList = new ArrayList<>();
		boolean newIng = true;
		
		for (Ingredient e : entry.getIngredients())
		{
			newIng = true;
			
			// at this point, the template has already been modified so that enchantments are properly included
			// whenever they need to be applied. Uniqueness of items is thus judged by item id AND enchantment level
			for (Ingredient ex : _ingredientsList)
			{
				// if the item was already added in the list, merely increment the count
				// this happens if 1 list entry has the same ingredient twice (example 2 swords = 1 dual)
				if ((ex.getItemId() == e.getItemId()) && (ex.getEnchantmentLevel() == e.getEnchantmentLevel()))
				{
					if ((double) ex.getItemCount() + e.getItemCount() > Integer.MAX_VALUE)
					{
						player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED);
						_ingredientsList.clear();
						_ingredientsList = null;
						return;
					}
					ex.setItemCount(ex.getItemCount() + e.getItemCount());
					newIng = false;
				}
			}
			
			// if it's a new ingredient, just store its info directly (item id, count, enchantment)
			if (newIng)
				_ingredientsList.add(new Ingredient(e));
		}
		
		// now check if the player has sufficient items in the inventory to cover the ingredients' expences
		for (Ingredient e : _ingredientsList)
		{
			if ((double) e.getItemCount() * _amount > Integer.MAX_VALUE)
			{
				player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED);
				_ingredientsList.clear();
				_ingredientsList = null;
				return;
			}
			
			if (e.getItemId() != 65336)
			{
				// if this is not a list that maintains enchantment, check the count of all items that have the given id.
				// otherwise, check only the count of items with exactly the needed enchantment level
				if (inv.getInventoryItemCount(e.getItemId(), maintainEnchantment ? e.getEnchantmentLevel() : -1) < ((Config.ALT_BLACKSMITH_USE_RECIPES || !e.getMaintainIngredient()) ? (e.getItemCount() * _amount) : e.getItemCount()))
				{
					player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
					_ingredientsList.clear();
					_ingredientsList = null;
					return;
				}
			}
			else
			{
				if (player.getClan() == null)
				{
					player.sendPacket(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER);
					return;
				}
				
				if (!player.isClanLeader())
				{
					player.sendPacket(SystemMessageId.ONLY_THE_CLAN_LEADER_IS_ENABLED);
					return;
				}
				
				if (player.getClan().getReputationScore() < (e.getItemCount() * _amount))
				{
					player.sendPacket(SystemMessageId.THE_CLAN_REPUTATION_SCORE_IS_TOO_LOW);
					return;
				}
			}
		}
		
		_ingredientsList.clear();
		_ingredientsList = null;
		List<L2Augmentation> augmentation = new ArrayList<>();
		
		// All ok, send success message, remove items and add final product
		player.sendPacket(SystemMessageId.SUCCESSFULLY_TRADED_WITH_NPC);
		
		for (Ingredient e : entry.getIngredients())
		{
			if (e.getItemId() != 65336)
			{
				ItemInstance itemToTake = inv.getItemByItemId(e.getItemId());
				
				if (itemToTake == null)
				{
					// this is a cheat, transaction will be aborted
					_log.severe(player.getName() + " is trying to cheat using multisell, merchant id: " + merchant.getNpcId());
					return;
				}
				
				if (Config.ALT_BLACKSMITH_USE_RECIPES || !e.getMaintainIngredient())
				{
					// if it's a stackable item, just reduce the amount from the first (only) instance that is found in the inventory
					if (itemToTake.isStackable())
					{
						if (!player.destroyItem("Multisell", itemToTake.getObjectId(), (e.getItemCount() * _amount), player.getTarget(), true))
							return;
					}
					else
					{
						// for non-stackable items, one of two scenaria are possible:
						// a) list maintains enchantment: get the instances that exactly match the requested enchantment level
						// b) list does not maintain enchantment: get the instances with the LOWEST enchantment level
						
						// a) if enchantment is maintained, then get a list of items that exactly match this enchantment
						if (maintainEnchantment)
						{
							// loop through this list and remove (one by one) each item until the required amount is taken.
							ItemInstance[] inventoryContents = inv.getAllItemsByItemId(e.getItemId(), e.getEnchantmentLevel());
							for (int i = 0; i < (e.getItemCount() * _amount); i++)
							{
								if (inventoryContents[i].isAugmented())
									augmentation.add(inventoryContents[i].getAugmentation());
								if (!player.destroyItem("Multisell", inventoryContents[i].getObjectId(), 1, player.getTarget(), true))
									return;
							}
						}
						else
						// b) enchantment is not maintained. Get the instances with the LOWEST enchantment level
						{
							for (int i = 1; i <= (e.getItemCount() * _amount); i++)
							{
								ItemInstance[] inventoryContents = inv.getAllItemsByItemId(e.getItemId());
								
								itemToTake = inventoryContents[0];
								// get item with the LOWEST enchantment level from the inventory (0 is the lowest)
								if (itemToTake.getEnchantLevel() > 0)
								{
									for (ItemInstance item : inventoryContents)
									{
										if (item.getEnchantLevel() < itemToTake.getEnchantLevel())
										{
											itemToTake = item;
											// nothing will have enchantment less than 0. If a zero-enchanted
											// item is found, just take it
											if (itemToTake.getEnchantLevel() == 0)
												break;
										}
									}
								}
								if (!player.destroyItem("Multisell", itemToTake.getObjectId(), 1, player.getTarget(), true))
									return;
							}
						}
					}
				}
			}
			else
			{
				int totalReputation = e.getItemCount() * _amount;
				player.getClan().takeReputationScore(totalReputation);
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP).addNumber(totalReputation));
			}
		}
		
		// Generate the appropriate items
		for (Ingredient e : entry.getProducts())
		{
			if (ItemTable.getInstance().createDummyItem(e.getItemId()).isStackable())
				inv.addItem("Multisell", e.getItemId(), (e.getItemCount() * _amount), player, player.getTarget());
			else
			{
				for (int i = 0; i < (e.getItemCount() * _amount); i++)
				{
					ItemInstance product = inv.addItem("Multisell", e.getItemId(), 1, player, player.getTarget());
					if (product == null)
						continue;
					
					if (maintainEnchantment)
					{
						if (i < augmentation.size())
							product.setAugmentation(new L2Augmentation(augmentation.get(i).getAugmentationId(), augmentation.get(i).getSkill()));
						
						product.setEnchantLevel(e.getEnchantmentLevel());
					}
				}
			}
			// msg part
			SystemMessage sm;
			
			if (e.getItemCount() * _amount > 1)
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S);
				sm.addItemName(e.getItemId());
				sm.addNumber(e.getItemCount() * _amount);
				player.sendPacket(sm);
			}
			else
			{
				if (maintainEnchantment && _enchantment > 0)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.ACQUIRED_S1_S2);
					sm.addNumber(_enchantment);
					sm.addItemName(e.getItemId());
				}
				else
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1);
					sm.addItemName(e.getItemId());
				}
				player.sendPacket(sm);
			}
		}
		player.sendPacket(new ItemList(player, false));
		
		StatusUpdate su = new StatusUpdate(player);
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
		
		// finally, give the tax to the castle...
		if (merchant != null && merchant.getIsInTown() && merchant.getCastle().getOwnerId() > 0)
			merchant.getCastle().addToTreasury(_transactionTax * _amount);
	}
	
	// Regarding taxation, the following appears to be the case:
	// a) The count of aa remains unchanged (taxes do not affect aa directly).
	// b) 5/6 of the amount of aa is taxed by the normal tax rate.
	// c) the resulting taxes are added as normal adena value.
	// d) normal adena are taxed fully.
	// e) Items other than adena and ancient adena are not taxed even when the list is taxable.
	// example: If the template has an item worth 120aa, and the tax is 10%,
	// then from 120aa, take 5/6 so that is 100aa, apply the 10% tax in adena (10a)
	// so the final price will be 120aa and 10a!
	private Entry prepareEntry(L2Npc merchant, Entry templateEntry, boolean applyTaxes, boolean maintainEnchantment, int enchantLevel)
	{
		Entry newEntry = new Entry();
		newEntry.setEntryId(templateEntry.getEntryId());
		int totalAdenaCount = 0;
		boolean hasIngredient = false;
		
		for (Ingredient ing : templateEntry.getIngredients())
		{
			// load the ingredient from the template
			Ingredient newIngredient = new Ingredient(ing);
			
			if (newIngredient.getItemId() == 57 && newIngredient.isTaxIngredient())
			{
				double taxRate = 0.0;
				if (applyTaxes)
				{
					if (merchant != null && merchant.getIsInTown())
						taxRate = merchant.getCastle().getTaxRate();
				}
				
				_transactionTax = (int) Math.round(newIngredient.getItemCount() * taxRate);
				totalAdenaCount += _transactionTax;
				continue; // do not yet add this adena amount to the list as non-taxIngredient adena might be entered later (order not guaranteed)
			}
			else if (ing.getItemId() == 57) // && !ing.isTaxIngredient()
			{
				totalAdenaCount += newIngredient.getItemCount();
				continue; // do not yet add this adena amount to the list as taxIngredient adena might be entered later (order not guaranteed)
			}
			// if it is an armor/weapon, modify the enchantment level appropriately, if necessary
			else if (maintainEnchantment && newIngredient.getItemId() > 0)
			{
				Item tempItem = ItemTable.getInstance().createDummyItem(newIngredient.getItemId()).getItem();
				if ((tempItem instanceof Armor) || (tempItem instanceof Weapon))
				{
					newIngredient.setEnchantmentLevel(enchantLevel);
					hasIngredient = true;
				}
			}
			
			// finally, add this ingredient to the entry
			newEntry.addIngredient(newIngredient);
		}
		// Next add the adena amount, if any
		if (totalAdenaCount > 0)
			newEntry.addIngredient(new Ingredient(57, totalAdenaCount, false, false));
		
		// Now modify the enchantment level of products, if necessary
		for (Ingredient ing : templateEntry.getProducts())
		{
			// load the ingredient from the template
			Ingredient newIngredient = new Ingredient(ing);
			
			if (maintainEnchantment && hasIngredient)
			{
				// if it is an armor/weapon, modify the enchantment level appropriately
				// (note, if maintain enchantment is "false" this modification will result to a +0)
				Item tempItem = ItemTable.getInstance().createDummyItem(newIngredient.getItemId()).getItem();
				if ((tempItem instanceof Armor) || (tempItem instanceof Weapon))
					newIngredient.setEnchantmentLevel(enchantLevel);
			}
			newEntry.addProduct(newIngredient);
		}
		return newEntry;
	}
}