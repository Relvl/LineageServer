package net.sf.l2j.gameserver.model.actor.position;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Erb
 */
public class PcPosition extends CharPosition {
    public PcPosition(L2PcInstance activeObject) {
        super(activeObject);
    }

    @Override
    public L2PcInstance getActiveObject() {
        return super.getActiveObject().getActingPlayer();
    }

    @Override
    protected void badCoords() {
        getActiveObject().teleToLocation(0, 0, 0, 0);
        getActiveObject().sendMessage("Error with your coords, Please ask a GM for help!");
    }
}