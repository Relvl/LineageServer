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
        L2PcInstance player = getClient().getActiveChar();
        if (player == null || player.isTeleporting() || player.isInObserverMode()) { return; }

        int realX = player.getX();
        int realY = player.getY();
        int realZ = player.getZ();

        if (posX == 0 && posY == 0) {
            // in this case this seems like a client error
            if (realX != 0) { return; }
        }

        int dx;
        int dy;
        int dz;
        double diffSq;

        if (player.isInBoat()) {
            if (Config.COORD_SYNCHRONIZE == 2) {
                dx = posX - player.getInVehiclePosition().getX();
                dy = posY - player.getInVehiclePosition().getY();
                dz = posZ - player.getInVehiclePosition().getZ();
                diffSq = dx * dx + dy * dy;
                if (diffSq > 250000) {
                    sendPacket(new GetOnVehicle(player.getObjectId(), data, player.getInVehiclePosition()));
                }
            }
            return;
        }

        // disable validations during fall to avoid "jumping"
        if (player.isFalling(posZ)) { return; }

        dx = posX - realX;
        dy = posY - realY;
        dz = posZ - realZ;
        diffSq = dx * dx + dy * dy;

        if (player.isFlying() || player.isInsideZone(ZoneId.WATER)) {
            player.getPosition().setXYZ(realX, realY, posZ);
            // validate packet, may also cause z bounce if close to land
            if (diffSq > 90000) {
                player.sendPacket(new ValidateLocation(player));
            }
        }
        // if too large, messes observation
        else if (diffSq < 360000) {
            // Only Z coordinate synched to server, mainly used when no geodata but can be used also with geodata
            if (Config.COORD_SYNCHRONIZE == -1) {
                player.getPosition().setXYZ(realX, realY, posZ);
                return;
            }
            // Trusting also client x,y coordinates (should not be used with geodata)
            if (Config.COORD_SYNCHRONIZE == 1) {
                // Heading changed on client = possible obstacle
                if (!player.isMoving() || !player.validateMovementHeading(heading)) {
                    // character is not moving, take coordinates from client
                    // 50*50 - attack won't work fluently if even small differences are corrected
                    if (diffSq < 2500) {
                        player.getPosition().setXYZ(realX, realY, posZ);
                    }
                    else {
                        player.getPosition().setXYZ(posX, posY, posZ);
                    }
                }
                else {
                    player.getPosition().setXYZ(realX, realY, posZ);
                }

                player.setHeading(heading);
                return;
            }
            // Sync 2 (or other), intended for geodata. Sends a validation packet to client when too far from server calculated real coordinate.
            // Due to geodata/zone errors, some Z axis checks are made. (maybe a temporary solution)
            // Important: this code part must work together with L2Character.updatePosition
            if (Config.GEODATA > 0 && (diffSq > 250000 || Math.abs(dz) > 200)) {
                if (Math.abs(dz) > 200 && Math.abs(dz) < 1500 && Math.abs(posZ - player.getClientZ()) < 800) {
                    player.getPosition().setXYZ(realX, realY, posZ);
                    realZ = posZ;
                }
                else {
                    player.sendPacket(new ValidateLocation(player));
                }
            }
        }

        player.setClientX(posX);
        player.setClientY(posY);
        player.setClientZ(posZ);
    }
}