package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.ai.model.L2BoatAI;
import net.sf.l2j.gameserver.model.actor.L2Vehicle;
import net.sf.l2j.gameserver.model.actor.template.CharTemplate;
import net.sf.l2j.gameserver.model.location.HeadedLocation;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.client.game_to_client.VehicleDeparture;
import net.sf.l2j.gameserver.network.client.game_to_client.VehicleInfo;
import net.sf.l2j.gameserver.network.client.game_to_client.VehicleStarted;

/**
 * @author Maktakien, reworked by DS
 */
public class L2BoatInstance extends L2Vehicle {
    public L2BoatInstance(int objectId, CharTemplate template) {
        super(objectId, template);

        setAI(new L2BoatAI(this));
    }

    @Override
    public boolean isBoat() {
        return true;
    }

    @Override
    public boolean moveToNextRoutePoint() {
        final boolean result = super.moveToNextRoutePoint();
        if (result) { broadcastPacket(new VehicleDeparture(this)); }

        return result;
    }

    @Override
    public void oustPlayer(L2PcInstance player, boolean removeFromList) {
        super.oustPlayer(player, removeFromList);

        final Location loc = getOustLoc();
        if (player.isOnline()) { player.teleToLocation(loc.getX(), loc.getY(), loc.getZ(), 0); }
        else {
            player.getPosition().setXYZInvisible(loc.getX(), loc.getY(), loc.getZ()); // disconnects handling
        }
    }

    @Override
    public void stopMove(HeadedLocation pos) {
        super.stopMove(pos);

        broadcastPacket(new VehicleStarted(this, 0));
        broadcastPacket(new VehicleInfo(this));
    }

    @Override
    public void sendInfo(L2PcInstance activeChar) {
        activeChar.sendPacket(new VehicleInfo(this));
    }
}
