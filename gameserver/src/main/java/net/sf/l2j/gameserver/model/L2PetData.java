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
package net.sf.l2j.gameserver.model;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is particular :
 * <ul>
 * <li>loads generic petdata (L2PetData), such as maxLoad, foodId,...</li>
 * <li>loads generated data from XML (stats) aswell (L2PetLevelData) : patk, matk, pdef, mdef, HPs,...</li>
 * </ul>
 **/
public class L2PetData
{
	private final Map<Integer, L2PetLevelData> _levelStats = new HashMap<>();
	
	private int _minlvl = Byte.MAX_VALUE;
	private int[] _food = {};
	
	public void addNewStat(int level, L2PetLevelData data)
	{
		if (_minlvl > level)
			_minlvl = level;
		
		_levelStats.put(level, data);
	}
	
	public L2PetLevelData getPetLevelData(int petLevel)
	{
		return _levelStats.get(petLevel);
	}
	
	public int getLoad()
	{
		return 54510;
	}
	
	// food Id
	public int[] getFood()
	{
		return _food;
	}
	
	public void setFood(int[] food)
	{
		_food = food;
	}
	
	public static class L2PetLevelData
	{
		private float _ownerExpTaken;
		private long _petMaxExp;
		
		private int _petMaxHP, _petMaxMP;
		private int _petPAtk, _petPDef;
		private int _petMAtk, _petMDef;
		private int _petMaxFeed, _petFeedBattle, _petFeedNormal;
		private int _petRegenHP, _petRegenMP;
		
		// Max Exp
		public long getPetMaxExp()
		{
			return _petMaxExp;
		}
		
		public void setPetMaxExp(long pPetMaxExp)
		{
			_petMaxExp = pPetMaxExp;
		}
		
		// XP retained
		public float getOwnerExpTaken()
		{
			return _ownerExpTaken;
		}
		
		public void setOwnerExpTaken(float pOwnerExpTaken)
		{
			_ownerExpTaken = pOwnerExpTaken;
		}
		
		// Max HP
		public int getPetMaxHP()
		{
			return _petMaxHP;
		}
		
		public void setPetMaxHP(int pPetMaxHP)
		{
			_petMaxHP = pPetMaxHP;
		}
		
		// Max Mp
		public int getPetMaxMP()
		{
			return _petMaxMP;
		}
		
		public void setPetMaxMP(int pPetMaxMP)
		{
			_petMaxMP = pPetMaxMP;
		}
		
		// PAtk
		public int getPetPAtk()
		{
			return _petPAtk;
		}
		
		public void setPetPAtk(int pPetPAtk)
		{
			_petPAtk = pPetPAtk;
		}
		
		// PDef
		public int getPetPDef()
		{
			return _petPDef;
		}
		
		public void setPetPDef(int pPetPDef)
		{
			_petPDef = pPetPDef;
		}
		
		// MAtk
		public int getPetMAtk()
		{
			return _petMAtk;
		}
		
		public void setPetMAtk(int pPetMAtk)
		{
			_petMAtk = pPetMAtk;
		}
		
		// MDef
		public int getPetMDef()
		{
			return _petMDef;
		}
		
		public void setPetMDef(int pPetMDef)
		{
			_petMDef = pPetMDef;
		}
		
		// MaxFeed
		public int getPetMaxFeed()
		{
			return _petMaxFeed;
		}
		
		public void setPetMaxFeed(int pPetMaxFeed)
		{
			_petMaxFeed = pPetMaxFeed;
		}
		
		// Normal Feed
		public int getPetFeedNormal()
		{
			return _petFeedNormal;
		}
		
		public void setPetFeedNormal(int pPetFeedNormal)
		{
			_petFeedNormal = pPetFeedNormal;
		}
		
		// Battle Feed
		public int getPetFeedBattle()
		{
			return _petFeedBattle;
		}
		
		public void setPetFeedBattle(int pPetFeedBattle)
		{
			_petFeedBattle = pPetFeedBattle;
		}
		
		// Regen HP
		public int getPetRegenHP()
		{
			return _petRegenHP;
		}
		
		public void setPetRegenHP(int pPetRegenHP)
		{
			_petRegenHP = pPetRegenHP;
		}
		
		// Regen MP
		public int getPetRegenMP()
		{
			return _petRegenMP;
		}
		
		public void setPetRegenMP(int pPetRegenMP)
		{
			_petRegenMP = pPetRegenMP;
		}
	}
}