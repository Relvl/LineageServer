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
package net.sf.l2j.gameserver.model.itemcontainer;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.EItemLocation;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;

public class PcWarehouse extends Warehouse {
    private final L2PcInstance _owner;

    public PcWarehouse(L2PcInstance owner) {
        _owner = owner;
    }

    @Override
    public String getName() {
        return "Warehouse";
    }

    @Override
    public EItemProcessPurpose getItemInteractionPurpose() {
        return EItemProcessPurpose.WAREHOUSE;
    }

    @Override
    public L2PcInstance getOwner() {
        return _owner;
    }

    @Override
    public EItemLocation getBaseLocation() {
        return EItemLocation.WAREHOUSE;
    }

    public String getLocationId() {
        return "0";
    }

    public int getLocationId(boolean dummy) {
        return 0;
    }

    public void setLocationId(L2PcInstance dummy) {
    }

    @Override
    public boolean validateCapacity(int slots) {
        return (items.size() + slots <= _owner.getWareHouseLimit());
    }
}