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
package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class ExStorageMaxCount extends L2GameServerPacket
{
	private final int _inventoryLimit;
	private final int _warehouseLimit;
	private final int _freightLimit;
	private final int _privateSellLimit;
	private final int _privateBuyLimit;
	private final int _dwarfRecipeLimit;
	private final int _commonRecipeLimit;
	
	public ExStorageMaxCount(L2PcInstance player)
	{
		_inventoryLimit = player.getInventoryLimit();
		_warehouseLimit = player.getWareHouseLimit();
		_freightLimit = player.getFreightLimit();
		_privateSellLimit = player.getPrivateSellStoreLimit();
		_privateBuyLimit = player.getPrivateBuyStoreLimit();
		_dwarfRecipeLimit = player.getDwarfRecipeLimit();
		_commonRecipeLimit = player.getCommonRecipeLimit();
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x2e);
		writeD(_inventoryLimit);
		writeD(_warehouseLimit);
		writeD(_freightLimit);
		writeD(_privateSellLimit);
		writeD(_privateBuyLimit);
		writeD(_dwarfRecipeLimit);
		writeD(_commonRecipeLimit);
	}
}