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

import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.instancemanager.CastleManorManager;
import net.sf.l2j.gameserver.model.L2Manor;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.L2ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;

/**
 * @author l3x
 */
public class Seed implements IItemHandler
{
	@Override
	public void useItem(L2Playable playable, L2ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof L2PcInstance))
			return;
		
		if (CastleManorManager.getInstance().isDisabled())
			return;
		
		final L2Object tgt = playable.getTarget();
		
		if (!(tgt instanceof L2Attackable) || !((L2Attackable) tgt).isSeedable())
		{
			playable.sendPacket(SystemMessageId.THE_TARGET_IS_UNAVAILABLE_FOR_SEEDING);
			return;
		}
		
		final L2Attackable target = (L2Attackable) tgt;
		if (target.isDead() || target.isSeeded())
		{
			playable.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
		
		final int seedId = item.getItemId();
		if (areaValid(seedId, MapRegionTable.getAreaCastle(playable.getX(), playable.getY())))
		{
			target.setSeeded(seedId, playable.getObjectId());
			final IntIntHolder[] skills = item.getEtcItem().getSkills();
			if (skills != null)
			{
				if (skills[0] == null)
					return;
				
				playable.useMagic(skills[0].getSkill(), false, false);
			}
		}
		else
			playable.sendPacket(SystemMessageId.THIS_SEED_MAY_NOT_BE_SOWN_HERE);
	}
	
	private static boolean areaValid(int seedId, int castleId)
	{
		return L2Manor.getInstance().getCastleIdForSeed(seedId) == castleId;
	}
}