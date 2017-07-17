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
package net.sf.l2j.gameserver.model.actor;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.ai.model.L2CharacterAI;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.model.VehiclePathPoint;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.knownlist.VehicleKnownList;
import net.sf.l2j.gameserver.model.actor.stat.VehicleStat;
import net.sf.l2j.gameserver.model.actor.template.CharTemplate;
import net.sf.l2j.gameserver.model.item.instance.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.HeadedLocation;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.model.world.L2WorldRegion;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.InventoryUpdate;
import net.sf.l2j.gameserver.network.client.game_to_client.L2GameServerPacket;
import net.sf.l2j.gameserver.taskmanager.MovementTaskManager;
import net.sf.l2j.gameserver.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * @author DS
 */
public abstract class L2Vehicle extends L2Character {
    protected final List<L2PcInstance> _passengers = new ArrayList<>();
    protected int _dockId = 0;
    protected Location _oustLoc = null;
    protected VehiclePathPoint[] _currentPath = null;
    protected int _runState = 0;
    private Runnable _engine = null;

    public L2Vehicle(int objectId, CharTemplate template) {
        super(objectId, template);
    }

    @Override
    public boolean isFlying() {
        return true;
    }

    public boolean isBoat() {
        return false;
    }

    public boolean canBeControlled() {
        return _engine == null;
    }

    public void registerEngine(Runnable r) {
        _engine = r;
    }

    public void runEngine(int delay) {
        if (_engine != null) { ThreadPoolManager.getInstance().scheduleGeneral(_engine, delay); }
    }

    public void executePath(VehiclePathPoint[] path) {
        _runState = 0;
        _currentPath = path;

        if (_currentPath != null && _currentPath.length > 0) {
            final VehiclePathPoint point = _currentPath[0];
            if (point.moveSpeed > 0) { getStat().setMoveSpeed(point.moveSpeed); }
            if (point.rotationSpeed > 0) { getStat().setRotationSpeed(point.rotationSpeed); }

            getAI().setIntention(EIntention.MOVE_TO, new HeadedLocation(point.x, point.y, point.z, 0));
            return;
        }
        getAI().setIntention(EIntention.ACTIVE);
    }

    @Override
    public boolean moveToNextRoutePoint() {
        _move = null;

        if (_currentPath != null) {
            _runState++;
            if (_runState < _currentPath.length) {
                final VehiclePathPoint point = _currentPath[_runState];
                if (!isMovementDisabled()) {
                    if (point.moveSpeed == 0) {
                        teleToLocation(point.x, point.y, point.z, 0);
                        _currentPath = null;
                    }
                    else {
                        if (point.moveSpeed > 0) { getStat().setMoveSpeed(point.moveSpeed); }
                        if (point.rotationSpeed > 0) { getStat().setRotationSpeed(point.rotationSpeed); }

                        MoveData m = new MoveData();
                        m.disregardingGeodata = false;
                        m.onGeodataPathIndex = -1;
                        m._xDestination = point.x;
                        m._yDestination = point.y;
                        m._zDestination = point.z;
                        m._heading = 0;

                        final double dx = point.x - getX();
                        final double dy = point.y - getY();
                        final double distance = Math.sqrt(dx * dx + dy * dy);
                        if (distance > 1) // vertical movement heading check
                        { setHeading(Util.calculateHeadingFrom(getX(), getY(), point.x, point.y)); }

                        m._moveStartTime = System.currentTimeMillis();
                        _move = m;

                        MovementTaskManager.getInstance().add(this);
                        return true;
                    }
                }
            }
            else { _currentPath = null; }
        }

        runEngine(10);
        return false;
    }

    @Override
    public void initKnownList() {
        setKnownList(new VehicleKnownList(this));
    }

    @Override
    public VehicleStat getStat() {
        return (VehicleStat) super.getStat();
    }

    @Override
    public void initCharStat() {
        setStat(new VehicleStat(this));
    }

    public boolean isInDock() {
        return _dockId > 0;
    }

    public void setInDock(int d) {
        _dockId = d;
    }

    public int getDockId() {
        return _dockId;
    }

    public Location getOustLoc() {
        return _oustLoc != null ? _oustLoc : MapRegionTable.getInstance().getTeleToLocation(this, MapRegionTable.TeleportWhereType.Town);
    }

    public void setOustLoc(Location loc) {
        _oustLoc = loc;
    }

    public void oustPlayers() {
        for (L2PcInstance player : _passengers) { oustPlayer(player, false); }

        _passengers.clear();
    }

    public void oustPlayer(L2PcInstance player, boolean removeFromList) {
        player.setVehicle(null);
        player.setInVehiclePosition(null);

        if (removeFromList) { removePassenger(player); }

        player.setInsideZone(ZoneId.PEACE, false);
        player.sendPacket(SystemMessageId.EXIT_PEACEFUL_ZONE);
    }

    public boolean addPassenger(L2PcInstance player) {
        if (player == null || _passengers.contains(player)) { return false; }

        // already in other vehicle
        if (player.getVehicle() != null && player.getVehicle() != this) { return false; }

        _passengers.add(player);

        player.setInsideZone(ZoneId.PEACE, true);
        player.sendPacket(SystemMessageId.ENTER_PEACEFUL_ZONE);

        return true;
    }

    public void removePassenger(L2PcInstance player) {
        _passengers.remove(player);
    }

    public boolean isEmpty() {
        return _passengers.isEmpty();
    }

    public List<L2PcInstance> getPassengers() {
        return _passengers;
    }

    public void broadcastToPassengers(L2GameServerPacket sm) {
        for (L2PcInstance player : _passengers) {
            if (player != null) { player.sendPacket(sm); }
        }
    }

    /**
     * Consume ticket(s) and teleport player from boat if no correct ticket
     *
     * @param itemId Ticket itemId
     * @param count  Ticket count
     * @param oustX
     * @param oustY
     * @param oustZ
     */
    public void payForRide(int itemId, int count, int oustX, int oustY, int oustZ) {
        for (L2PcInstance player : getKnownList().getKnownTypeInRadius(L2PcInstance.class, 1000)) {
            if (player.isInBoat() && player.getBoat() == this) {
                if (itemId > 0) {
                    final L2ItemInstance ticket = player.getInventory().getItemByItemId(itemId);
                    if (ticket == null || player.getInventory().destroyItem("Boat", ticket, count, player, this) == null) {
                        player.sendPacket(SystemMessageId.NOT_CORRECT_BOAT_TICKET);
                        player.teleToLocation(oustX, oustY, oustZ, 20);
                        continue;
                    }

                    final InventoryUpdate iu = new InventoryUpdate();
                    if (ticket.getCount() == 0) { iu.addRemovedItem(ticket); }
                    else { iu.addModifiedItem(ticket); }

                    player.sendPacket(iu);
                }
                addPassenger(player);
            }
        }
    }

    @Override
    public boolean updatePosition() {
        final boolean result = super.updatePosition();

        for (L2PcInstance player : _passengers) {
            if (player != null && player.getVehicle() == this) {
                player.getPosition().setXYZ(getX(), getY(), getZ());
                player.revalidateZone(false);
            }
        }
        return result;
    }

    @Override
    public void teleToLocation(int x, int y, int z, int randomOffset) {
        if (isMoving()) { stopMove(null); }

        setIsTeleporting(true);

        getAI().setIntention(EIntention.ACTIVE);

        for (L2PcInstance player : _passengers) {
            if (player != null) { player.teleToLocation(x, y, z, randomOffset); }
        }

        decayMe();
        setXYZ(x, y, z);

        onTeleported();
        revalidateZone(true);
    }

    @Override
    public void stopMove(HeadedLocation pos) {
        _move = null;
        if (pos != null) {
            setXYZ(pos.getX(), pos.getY(), pos.getZ());
            setHeading(pos.getHeading());
            revalidateZone(true);
        }
    }

    @Override
    public void deleteMe() {
        _engine = null;

        try {
            if (isMoving()) { stopMove(null); }
        }
        catch (Exception e) {
            _log.log(Level.SEVERE, "Failed stopMove().", e);
        }

        try {
            oustPlayers();
        }
        catch (Exception e) {
            _log.log(Level.SEVERE, "Failed oustPlayers().", e);
        }

        final L2WorldRegion oldRegion = getWorldRegion();

        try {
            decayMe();
        }
        catch (Exception e) {
            _log.log(Level.SEVERE, "Failed decayMe().", e);
        }

        if (oldRegion != null) { oldRegion.removeFromZones(this); }

        try {
            getKnownList().removeAllKnownObjects();
        }
        catch (Exception e) {
            _log.log(Level.SEVERE, "Failed cleaning knownlist.", e);
        }

        // Remove L2Object object from _allObjects of L2World
        L2World.getInstance().removeObject(this);

        super.deleteMe();
    }

    @Override
    public void updateAbnormalEffect() {
    }

    @Override
    public L2ItemInstance getActiveWeaponInstance() {
        return null;
    }

    @Override
    public Weapon getActiveWeaponItem() {
        return null;
    }

    @Override
    public L2ItemInstance getSecondaryWeaponInstance() {
        return null;
    }

    @Override
    public Weapon getSecondaryWeaponItem() {
        return null;
    }

    @Override
    public int getLevel() {
        return 0;
    }

    @Override
    public boolean isAutoAttackable(L2Character attacker) {
        return false;
    }

    @Override
    public void setAI(L2CharacterAI newAI) {
        if (ai == null) { ai = newAI; }
    }

    @Override
    public void detachAI() {
    }
}