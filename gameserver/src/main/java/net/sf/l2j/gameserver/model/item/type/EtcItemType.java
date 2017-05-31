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
package net.sf.l2j.gameserver.model.item.type;

public enum EtcItemType implements ItemType
{
	NONE,
	ARROW,
	POTION,
	SCRL_ENCHANT_WP,
	SCRL_ENCHANT_AM,
	SCROLL,
	RECIPE,
	MATERIAL,
	PET_COLLAR,
	CASTLE_GUARD,
	LOTTO,
	RACE_TICKET,
	DYE,
	SEED,
	CROP,
	MATURECROP,
	HARVEST,
	SEED2,
	TICKET_OF_LORD,
	LURE,
	BLESS_SCRL_ENCHANT_WP,
	BLESS_SCRL_ENCHANT_AM,
	COUPON,
	ELIXIR,
	
	// L2J CUSTOM, BACKWARD COMPATIBILITY
	SHOT,
	HERB,
	QUEST;
	
	/**
	 * Returns the ID of the item after applying the mask.
	 * @return int : ID of the item
	 */
	@Override
	public int mask()
	{
		return 0;
	}
}