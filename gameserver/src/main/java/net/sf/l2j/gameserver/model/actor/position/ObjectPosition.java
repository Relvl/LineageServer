package net.sf.l2j.gameserver.model.actor.position;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.location.HeadedLocation;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.model.world.L2WorldRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectPosition extends HeadedLocation {
    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectPosition.class);

    private final L2Object activeObject;
    private L2WorldRegion worldRegion;

    public ObjectPosition(L2Object activeObject) {
        this.activeObject = activeObject;
        setWorldRegion(L2World.getInstance().getRegion(this));
    }

    public final void setXYZ(Location newPosition) { setXYZ(newPosition.getX(), newPosition.getY(), newPosition.getZ()); }

    @Override
    public final void setXYZ(int x, int y, int z) {
        assert worldRegion != null;

        super.setXYZ(x, y, z);

        try {
            if (L2World.getInstance().getRegion(this) != worldRegion) {
                if (!getActiveObject().isVisible()) { return; }
                L2WorldRegion newRegion = L2World.getInstance().getRegion(this);
                if (newRegion != worldRegion) {
                    worldRegion.removeVisibleObject(getActiveObject());
                    setWorldRegion(newRegion);
                    worldRegion.addVisibleObject(getActiveObject());
                }
            }
        }
        catch (RuntimeException e) {
            LOGGER.error("Object Id at bad coords: (x: {}, y: {}, z: {}).", getX(), getY(), getZ(), e);
            badCoords();
        }
    }

    protected void badCoords() {}

    public final void setXYZInvisible(int x, int y, int z) {
        assert worldRegion == null;

        if (x > L2World.WORLD_X_MAX) { x = L2World.WORLD_X_MAX - 5000; }
        if (x < L2World.WORLD_X_MIN) { x = L2World.WORLD_X_MIN + 5000; }
        if (y > L2World.WORLD_Y_MAX) { y = L2World.WORLD_Y_MAX - 5000; }
        if (y < L2World.WORLD_Y_MIN) { y = L2World.WORLD_Y_MIN + 5000; }

        setXYZ(x, y, z);
        getActiveObject().setIsVisible(false);
    }

    public L2Object getActiveObject() { return activeObject; }

    public final L2WorldRegion getWorldRegion() { return worldRegion; }

    public void setWorldRegion(L2WorldRegion value) {
        if (worldRegion != null && getActiveObject().isCharacter()) {
            if (value == null) { worldRegion.removeFromZones((L2Character) getActiveObject()); }
            else { worldRegion.revalidateZones((L2Character) getActiveObject()); }
        }
        worldRegion = value;
    }
}