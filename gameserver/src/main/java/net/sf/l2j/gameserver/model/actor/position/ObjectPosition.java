package net.sf.l2j.gameserver.model.actor.position;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.model.world.L2WorldRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectPosition {
    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectPosition.class);

    private final L2Object activeObject;
    private Location worldPosition;
    private L2WorldRegion worldRegion;

    public ObjectPosition(L2Object activeObject) {
        this.activeObject = activeObject;
        setWorldRegion(L2World.getInstance().getRegion(getWorldPosition()));
    }

    public final void setXYZ(int x, int y, int z) {
        assert worldRegion != null;
        setWorldPosition(x, y, z);

        try {
            if (L2World.getInstance().getRegion(getWorldPosition()) != worldRegion) { updateWorldRegion(); }
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

        setWorldPosition(x, y, z);
        getActiveObject().setIsVisible(false);
    }

    public void updateWorldRegion() {
        if (!getActiveObject().isVisible()) { return; }
        L2WorldRegion newRegion = L2World.getInstance().getRegion(getWorldPosition());
        if (newRegion != worldRegion) {
            worldRegion.removeVisibleObject(getActiveObject());
            setWorldRegion(newRegion);
            worldRegion.addVisibleObject(getActiveObject());
        }
    }

    public L2Object getActiveObject() {
        return activeObject;
    }

    public final int getX() {
        return getWorldPosition().getX();
    }

    public final int getY() {
        return getWorldPosition().getY();
    }

    public final int getZ() {
        return getWorldPosition().getZ();
    }

    public final Location getWorldPosition() {
        if (worldPosition == null) { worldPosition = new Location(0, 0, 0); }
        return worldPosition;
    }

    public final void setWorldPosition(int x, int y, int z) {
        getWorldPosition().setXYZ(x, y, z);
    }

    public final void setWorldPosition(Location newPosition) {
        setWorldPosition(newPosition.getX(), newPosition.getY(), newPosition.getZ());
    }

    public final L2WorldRegion getWorldRegion() {
        return worldRegion;
    }

    public void setWorldRegion(L2WorldRegion value) {
        // confirm revalidation of old region's zones
        if (worldRegion != null && getActiveObject() instanceof L2Character) {
            if (value != null) { worldRegion.revalidateZones((L2Character) getActiveObject()); }
            else { worldRegion.removeFromZones((L2Character) getActiveObject()); }
        }

        worldRegion = value;
    }
}