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
package net.sf.l2j.gameserver.model.item;

import net.sf.l2j.gameserver.datatables.HennaTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.StatsSet;

public final class Henna
{
	private final int symbolId;
	private final int dye;
	private final int price;
	private final int statINT;
	private final int statSTR;
	private final int statCON;
	private final int statMEN;
	private final int statDEX;
	private final int statWIT;
	
	public Henna(StatsSet set)
	{
		symbolId = set.getInteger("symbol_id");
		dye = set.getInteger("dye");
		price = set.getInteger("price");
		statINT = set.getInteger("INT");
		statSTR = set.getInteger("STR");
		statCON = set.getInteger("CON");
		statMEN = set.getInteger("MEN");
		statDEX = set.getInteger("DEX");
		statWIT = set.getInteger("WIT");
	}
	
	public int getSymbolId()
	{
		return symbolId;
	}
	
	public int getDyeId()
	{
		return dye;
	}
	
	public int getPrice()
	{
		return price;
	}
	
	public static final int getAmountDyeRequire()
	{
		return 10;
	}
	
	public int getStatINT()
	{
		return statINT;
	}
	
	public int getStatSTR()
	{
		return statSTR;
	}
	
	public int getStatCON()
	{
		return statCON;
	}
	
	public int getStatMEN()
	{
		return statMEN;
	}
	
	public int getStatDEX()
	{
		return statDEX;
	}
	
	public int getStatWIT()
	{
		return statWIT;
	}
	
	public boolean isForThisClass(L2PcInstance player)
	{
		for (Henna henna : HennaTable.getInstance().getAvailableHenna(player.getClassId().getId()))
			if (henna.equals(this))
				return true;
		
		return false;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof Henna && symbolId == ((Henna) obj).symbolId && dye == ((Henna) obj).dye;
	}
}