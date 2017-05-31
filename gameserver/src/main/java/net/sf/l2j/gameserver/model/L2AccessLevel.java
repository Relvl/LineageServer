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

import java.util.logging.Logger;

import net.sf.l2j.gameserver.datatables.AccessLevels;

/**
 * @author FBIagent<br>
 */
public class L2AccessLevel
{
	private static Logger _log = Logger.getLogger(L2AccessLevel.class.getName());
	
	private int _accessLevel = 0;
	private String _name = null;
	
	L2AccessLevel[] _childsAccessLevel = null;
	private String _childs = null;
	
	private int _nameColor = 0;
	private int _titleColor = 0;
	
	private boolean _isGm = false;
	
	private boolean _allowPeaceAttack = false;
	private boolean _allowFixedRes = false;
	private boolean _allowTransaction = false;
	private boolean _allowAltG = false;
	private boolean _giveDamage = false;
	private boolean _takeAggro = false;
	private boolean _gainExp = false;
	
	/**
	 * Initializes members<br>
	 * <br>
	 * @param accessLevel as int<br>
	 * @param name as String<br>
	 * @param nameColor as int<br>
	 * @param titleColor as int<br>
	 * @param childs as String<br>
	 * @param isGm as boolean<br>
	 * @param allowPeaceAttack as boolean<br>
	 * @param allowFixedRes as boolean<br>
	 * @param allowTransaction as boolean<br>
	 * @param allowAltG as boolean<br>
	 * @param giveDamage as boolean<br>
	 * @param takeAggro as boolean<br>
	 * @param gainExp as boolean<br>
	 */
	public L2AccessLevel(int accessLevel, String name, int nameColor, int titleColor, String childs, boolean isGm, boolean allowPeaceAttack, boolean allowFixedRes, boolean allowTransaction, boolean allowAltG, boolean giveDamage, boolean takeAggro, boolean gainExp)
	{
		_accessLevel = accessLevel;
		_name = name;
		_nameColor = nameColor;
		_titleColor = titleColor;
		_childs = childs;
		_isGm = isGm;
		_allowPeaceAttack = allowPeaceAttack;
		_allowFixedRes = allowFixedRes;
		_allowTransaction = allowTransaction;
		_allowAltG = allowAltG;
		_giveDamage = giveDamage;
		_takeAggro = takeAggro;
		_gainExp = gainExp;
	}
	
	public int getLevel()
	{
		return _accessLevel;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public int getNameColor()
	{
		return _nameColor;
	}
	
	public int getTitleColor()
	{
		return _titleColor;
	}
	
	public boolean isGm()
	{
		return _isGm;
	}
	
	public boolean allowPeaceAttack()
	{
		return _allowPeaceAttack;
	}
	
	public boolean allowFixedRes()
	{
		return _allowFixedRes;
	}
	
	public boolean allowTransaction()
	{
		return _allowTransaction;
	}
	
	public boolean allowAltG()
	{
		return _allowAltG;
	}
	
	public boolean canGiveDamage()
	{
		return _giveDamage;
	}
	
	public boolean canTakeAggro()
	{
		return _takeAggro;
	}
	
	public boolean canGainExp()
	{
		return _gainExp;
	}
	
	/**
	 * Returns if the access level contains allowedAccess as child<br>
	 * <br>
	 * @param accessLevel as AccessLevel<br>
	 * <br>
	 * @return boolean: true if a child access level is equals to allowedAccess, otherwise false<br>
	 */
	public boolean hasChildAccess(L2AccessLevel accessLevel)
	{
		if (_childsAccessLevel == null)
		{
			if (_childs == null)
				return false;
			
			setChildAccess(_childs);
			for (L2AccessLevel childAccess : _childsAccessLevel)
			{
				if (childAccess != null && (childAccess.getLevel() == accessLevel.getLevel() || childAccess.hasChildAccess(accessLevel)))
					return true;
			}
		}
		else
		{
			for (L2AccessLevel childAccess : _childsAccessLevel)
			{
				if (childAccess != null && (childAccess.getLevel() == accessLevel.getLevel() || childAccess.hasChildAccess(accessLevel)))
					return true;
			}
		}
		return false;
	}
	
	private void setChildAccess(String childs)
	{
		String[] childsSplit = childs.split(";");
		
		_childsAccessLevel = new L2AccessLevel[childsSplit.length];
		
		for (int i = 0; i < childsSplit.length; ++i)
		{
			L2AccessLevel accessLevelInst = AccessLevels.getInstance().getAccessLevel(Integer.parseInt(childsSplit[i]));
			
			if (accessLevelInst == null)
			{
				_log.warning("AccessLevel: Undefined child access level " + childsSplit[i]);
				continue;
			}
			
			if (accessLevelInst.hasChildAccess(this))
			{
				_log.warning("AccessLevel: Child access tree overlapping for " + _name + " and " + accessLevelInst.getName());
				continue;
			}
			
			_childsAccessLevel[i] = accessLevelInst;
		}
	}
}