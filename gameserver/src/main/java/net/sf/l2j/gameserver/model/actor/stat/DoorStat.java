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
package net.sf.l2j.gameserver.model.actor.stat;

import net.sf.l2j.gameserver.instancemanager.SevenSigns;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;

public class DoorStat extends CharStat
{
	public DoorStat(L2DoorInstance activeChar)
	{
		super(activeChar);
		
		setLevel((byte) 1);
	}
	
	@Override
	public L2DoorInstance getActiveChar()
	{
		return (L2DoorInstance) super.getActiveChar();
	}
	
	@Override
	public int getMDef(L2Character target, L2Skill skill)
	{
		double defense = getActiveChar().getTemplate().getBaseMDef();
		
		final int sealOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE);
		if (sealOwner == SevenSigns.CABAL_DAWN)
			defense *= 1.2;
		else if (sealOwner == SevenSigns.CABAL_DUSK)
			defense *= 0.3;
		
		return (int) defense;
	}
	
	@Override
	public int getPDef(L2Character target)
	{
		double defense = getActiveChar().getTemplate().getBasePDef();
		
		final int sealOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE);
		if (sealOwner == SevenSigns.CABAL_DAWN)
			defense *= 1.2;
		else if (sealOwner == SevenSigns.CABAL_DUSK)
			defense *= 0.3;
		
		// is it necessary to continue calc stat? can doors receive Pdef/Mdef buff?
		return (int) defense;
	}
	
	@Override
	public final byte getLevel()
	{
		return 1;
	}
}