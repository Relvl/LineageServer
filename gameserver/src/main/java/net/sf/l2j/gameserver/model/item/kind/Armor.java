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
package net.sf.l2j.gameserver.model.item.kind;

import net.sf.l2j.gameserver.model.item.type.ArmorType;
import net.sf.l2j.gameserver.templates.StatsSet;

/**
 * This class is dedicated to the management of armors.
 */
public final class Armor extends Item
{
	private ArmorType _type;
	
	/**
	 * Constructor for Armor.<BR>
	 * <BR>
	 * <U><I>Variables filled :</I></U><BR>
	 * <LI>_avoidModifier</LI> <LI>_pDef & _mDef</LI> <LI>_mpBonus & _hpBonus</LI>
	 * @param set : StatsSet designating the set of couples (key,value) caracterizing the armor
	 * @see Item constructor
	 */
	public Armor(StatsSet set)
	{
		super(set);
		_type = ArmorType.valueOf(set.getString("armor_type", "none").toUpperCase());
		
		int _bodyPart = getBodyPart();
		if (_bodyPart == Item.SLOT_NECK || _bodyPart == Item.SLOT_FACE || _bodyPart == Item.SLOT_HAIR || _bodyPart == Item.SLOT_HAIRALL || (_bodyPart & Item.SLOT_L_EAR) != 0 || (_bodyPart & Item.SLOT_L_FINGER) != 0 || (_bodyPart & Item.SLOT_BACK) != 0)
		{
			_type1 = Item.TYPE1_WEAPON_RING_EARRING_NECKLACE;
			_type2 = Item.TYPE2_ACCESSORY;
		}
		else
		{
			if (_type == ArmorType.NONE && getBodyPart() == Item.SLOT_L_HAND) // retail define shield as NONE
				_type = ArmorType.SHIELD;
			
			_type1 = Item.TYPE1_SHIELD_ARMOR;
			_type2 = Item.TYPE2_SHIELD_ARMOR;
		}
	}
	
	/**
	 * Returns the type of the armor.
	 * @return ArmorType
	 */
	@Override
	public ArmorType getItemType()
	{
		return _type;
	}
	
	/**
	 * Returns the ID of the item after applying the mask.
	 * @return int : ID of the item
	 */
	@Override
	public final int getItemMask()
	{
		return getItemType().mask();
	}
}