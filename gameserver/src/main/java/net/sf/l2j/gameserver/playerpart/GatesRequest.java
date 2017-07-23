package net.sf.l2j.gameserver.playerpart;

import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;

/**
 * @author Johnson / 19.07.2017
 */
public class GatesRequest {
    private L2DoorInstance door;

    public void setTarget(L2DoorInstance door) {
        this.door = door;
    }

    public L2DoorInstance getDoor() {
        return door;
    }
}
