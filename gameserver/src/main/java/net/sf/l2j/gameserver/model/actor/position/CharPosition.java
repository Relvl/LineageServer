package net.sf.l2j.gameserver.model.actor.position;

import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.world.L2WorldRegion;

public class CharPosition extends ObjectPosition {
    public CharPosition(L2Character activeObject) {
        super(activeObject);
    }

    @Override
    protected void badCoords() {
        getActiveObject().decayMe();
    }

    @Override
    public final void setWorldRegion(L2WorldRegion value) {
        // confirm revalidation of old region's zones
        if (getWorldRegion() != null && getActiveObject() instanceof L2Character) {
            if (value != null) { getWorldRegion().revalidateZones((L2Character) getActiveObject()); }
            else { getWorldRegion().removeFromZones((L2Character) getActiveObject()); }
        }
        super.setWorldRegion(value);
    }
}
