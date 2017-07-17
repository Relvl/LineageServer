package net.sf.l2j.gameserver.model.actor.position;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.model.world.L2WorldRegion;

import java.util.logging.Logger;

public class ObjectPosition {
    private static final Logger _log = Logger.getLogger(ObjectPosition.class.getName());

    private final L2Object _activeObject;
    private Location _worldPosition;
    private L2WorldRegion _worldRegion; // Object localization : Used for items/chars that are seen in the world

    public ObjectPosition(L2Object activeObject) {
        _activeObject = activeObject;
        setWorldRegion(L2World.getInstance().getRegion(getWorldPosition()));
    }

    public final void setXYZ(int x, int y, int z) {
        assert getWorldRegion() != null;
        setWorldPosition(x, y, z);

        try {
            if (L2World.getInstance().getRegion(getWorldPosition()) != getWorldRegion()) { updateWorldRegion(); }
        }
        catch (Exception e) {
            _log.warning("Object Id at bad coords: (x: " + getX() + ", y: " + getY() + ", z: " + getZ() + ").");
            badCoords();
        }
    }

    protected void badCoords() {
    }

    public final void setXYZInvisible(int x, int y, int z) {
        assert getWorldRegion() == null;

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
        if (newRegion != getWorldRegion()) {
            getWorldRegion().removeVisibleObject(getActiveObject());

            setWorldRegion(newRegion);

            // Add the L2Oject spawn to _visibleObjects and if necessary to _allplayers of its L2WorldRegion
            getWorldRegion().addVisibleObject(getActiveObject());
        }
    }

    public L2Object getActiveObject() {
        return _activeObject;
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
        if (_worldPosition == null) { _worldPosition = new Location(0, 0, 0); }
        return _worldPosition;
    }

    public final void setWorldPosition(int x, int y, int z) {
        getWorldPosition().setXYZ(x, y, z);
    }

    public final void setWorldPosition(Location newPosition) {
        setWorldPosition(newPosition.getX(), newPosition.getY(), newPosition.getZ());
    }

    public final L2WorldRegion getWorldRegion() {
        return _worldRegion;
    }

    public void setWorldRegion(L2WorldRegion value) {
        // confirm revalidation of old region's zones
        if (_worldRegion != null && getActiveObject() instanceof L2Character) {
            if (value != null) { _worldRegion.revalidateZones((L2Character) getActiveObject()); }
            else { _worldRegion.removeFromZones((L2Character) getActiveObject()); }
        }

        _worldRegion = value;
    }
}