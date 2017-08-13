package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.client.game_to_client.GetOnVehicle;
import net.sf.l2j.gameserver.network.client.game_to_client.ValidateLocation;

public class ValidatePosition extends L2GameClientPacket {
    private int posX;
    private int posY;
    private int posZ;
    private int heading;
    private int data;

    @Override
    protected void readImpl() {
        posX = readD();
        posY = readD();
        posZ = readD();
        heading = readD();
        data = readD();
    }

    @Override
    protected void runImpl() {
        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null || activeChar.isTeleporting() || activeChar.isInObserverMode()) { return; }

        int realX = activeChar.getX();
        int realY = activeChar.getY();
        int realZ = activeChar.getZ();

        if (posX == 0 && posY == 0) {
            if (realX != 0) // in this case this seems like a client error
            { return; }
        }

        int dx, dy, dz;
        double diffSq;

        if (activeChar.isInBoat()) {
            if (Config.COORD_SYNCHRONIZE == 2) {
                dx = posX - activeChar.getInVehiclePosition().getX();
                dy = posY - activeChar.getInVehiclePosition().getY();
                dz = posZ - activeChar.getInVehiclePosition().getZ();
                diffSq = dx * dx + dy * dy;
                if (diffSq > 250000) {
                    sendPacket(new GetOnVehicle(activeChar.getObjectId(), data, activeChar.getInVehiclePosition()));
                }
            }
            return;
        }

        if (activeChar.isFalling(posZ)) {
            return; // disable validations during fall to avoid "jumping"
        }

        dx = posX - realX;
        dy = posY - realY;
        dz = posZ - realZ;
        diffSq = dx * dx + dy * dy;

        if (activeChar.isFlying() || activeChar.isInsideZone(ZoneId.WATER)) {
            activeChar.getPosition().setXYZ(realX, realY, posZ);
            if (diffSq > 90000) // validate packet, may also cause z bounce if close to land
            { activeChar.sendPacket(new ValidateLocation(activeChar)); }
        }
        else if (diffSq < 360000) // if too large, messes observation
        {
            if (Config.COORD_SYNCHRONIZE == -1) // Only Z coordinate synched to server,
            // mainly used when no geodata but can be used also with geodata
            {
                activeChar.getPosition().setXYZ(realX, realY, posZ);
                return;
            }
            if (Config.COORD_SYNCHRONIZE == 1) // Trusting also client x,y coordinates (should not be used with geodata)
            {
                // Heading changed on client = possible obstacle
                if (!activeChar.isMoving() || !activeChar.validateMovementHeading(heading)) {
                    // character is not moving, take coordinates from client
                    if (diffSq < 2500) // 50*50 - attack won't work fluently if even small differences are corrected
                    { activeChar.getPosition().setXYZ(realX, realY, posZ); }
                    else { activeChar.getPosition().setXYZ(posX, posY, posZ); }
                }
                else { activeChar.getPosition().setXYZ(realX, realY, posZ); }

                activeChar.setHeading(heading);
                return;
            }
            // Sync 2 (or other), intended for geodata. Sends a validation packet to client when too far from server calculated real coordinate.
            // Due to geodata/zone errors, some Z axis checks are made. (maybe a temporary solution)
            // Important: this code part must work together with L2Character.updatePosition
            if (Config.GEODATA > 0 && (diffSq > 250000 || Math.abs(dz) > 200)) {
                if (Math.abs(dz) > 200 && Math.abs(dz) < 1500 && Math.abs(posZ - activeChar.getClientZ()) < 800) {
                    activeChar.getPosition().setXYZ(realX, realY, posZ);
                    realZ = posZ;
                }
                else {
                    activeChar.sendPacket(new ValidateLocation(activeChar));
                }
            }
        }

        activeChar.setClientX(posX);
        activeChar.setClientY(posY);
        activeChar.setClientZ(posZ);
    }
}