package net.sf.l2j.gameserver.geoengine;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.geoengine.geodata.GeoFormat;
import net.sf.l2j.gameserver.geoengine.pathfinding.*;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.location.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class PathFinding {
    protected static final Logger LOGGER = LoggerFactory.getLogger(PathFinding.class);

    private static PathFinding _pathFinding;

    private static List<L2ItemInstance> _debugItems;

    /**
     * Initializes pathfinding.
     */
    public static final void initialize() {
        LOGGER.info("PathFinding: Initializing...");

        // load pathfinding engine
        if (Config.GEODATA <= 0) {
            _pathFinding = new NullPathFinding();
        }
        else if (Config.GEODATA == 1) {
            if (Config.GEODATA_FORMAT != GeoFormat.L2D) { _pathFinding = new PathCheckerStd(); }
            else { _pathFinding = new PathCheckerDiag(); }
        }
        else {
            if (Config.GEODATA_FORMAT != GeoFormat.L2D) { _pathFinding = new CellPathFindingStd(); }
            else { _pathFinding = new CellPathFindingDiag(); }
        }

        // initialize debug items
        _debugItems = new CopyOnWriteArrayList<>();
    }

    public static final PathFinding getInstance() {
        return _pathFinding;
    }

    /**
     * Returns the list of location objects as a result of complete path calculation.
     *
     * @param ox       : origin x
     * @param oy       : origin y
     * @param oz       : origin z
     * @param tx       : target x
     * @param ty       : target y
     * @param tz       : target z
     * @param playable : moving object is playable?
     * @return List<Location> : complete path from nodes
     */
    public abstract List<Location> findPath(int ox, int oy, int oz, int tx, int ty, int tz, boolean playable);

    /**
     * Check line of sight from L2Object to L2Object.
     *
     * @param origin : The origin object.
     * @param target : The target object.
     * @return boolean : True if origin can see target
     */
    public boolean canSeeTarget(L2Object origin, L2Object target) {
        int oheight = 0;
        if (origin instanceof L2Character) {
            oheight = ((L2Character) origin).getTemplate().getCollisionHeight() * 2; // real height = collision height * 2
        }

        int theight = 0;
        if (target instanceof L2Character) {
            theight = ((L2Character) target).getTemplate().getCollisionHeight() * 2; // real height = collision height * 2
        }

        return canSeeTarget(origin.getX(), origin.getY(), origin.getZ(), oheight, target.getX(), target.getY(), target.getZ(), theight);
    }

    /**
     * Check line of sight from L2Object to Point3D.
     *
     * @param origin   : The origin object.
     * @param position : The target position.
     * @return boolean : True if object can see position
     */
    public boolean canSeeTarget(L2Object origin, Location position) {
        int height = 0;
        if (origin instanceof L2Character) {
            height = ((L2Character) origin).getTemplate().getCollisionHeight(); // real height = collision height * 2
        }

        return canSeeTarget(origin.getX(), origin.getY(), origin.getZ(), height, position.getX(), position.getY(), position.getZ(), 0);
    }

    /**
     * Check line of sight from coordinates to coordinates.
     *
     * @param ox      : origin X coord
     * @param oy      : origin Y coord
     * @param oz      : origin Z coord
     * @param oheight : origin height (if instance of {@link L2Character})
     * @param tx      : target X coord
     * @param ty      : target Y coord
     * @param tz      : target Z coord
     * @param theight : target height (if instance of {@link L2Character})
     * @return Location : True if target coordinates can be seen from origin coordinates
     */
    public abstract boolean canSeeTarget(int ox, int oy, int oz, int oheight, int tx, int ty, int tz, int theight);

    /**
     * Check movement from coordinates to coordinates.
     *
     * @param ox : origin X coord
     * @param oy : origin Y coord
     * @param oz : origin Z coord
     * @param tx : target X coord
     * @param ty : target Y coord
     * @param tz : target Z coord
     * @return Location : True if target coordinates are reachable from origin coordinates
     */
    public abstract boolean canMoveToTarget(int ox, int oy, int oz, int tx, int ty, int tz);

    /**
     * Check movement from origin to target. Returns last available point in the checked path.
     *
     * @param ox : origin X coord
     * @param oy : origin Y coord
     * @param oz : origin Z coord
     * @param tx : target X coord
     * @param ty : target Y coord
     * @param tz : target Z coord
     * @return Location : Last point where object can walk (just before wall)
     */
    public abstract Location canMoveToTargetLoc(int ox, int oy, int oz, int tx, int ty, int tz);

    /**
     * Return pathfinding stats, useful for getting information about pathfinding status.
     *
     * @return List<String> : stats
     */
    public abstract List<String> getStat();

    /**
     * Add new item to drop list for debug purpose.
     *
     * @param id    : Item id
     * @param count : Item count
     * @param loc   : Item location
     */
    public static final void dropDebugItem(int id, int count, Location loc) {
        final L2ItemInstance item = new L2ItemInstance(IdFactory.getInstance().getNextId(), id);
        item.setCount(count);
        item.spawnMe(loc.getX(), loc.getY(), loc.getZ());
        _debugItems.add(item);
    }

    /**
     * Clear item drop list for debugging paths.
     */
    public static final void clearDebugItems() {
        for (L2ItemInstance item : _debugItems) {
            if (item == null) { continue; }

            item.decayMe();
        }

        _debugItems.clear();
    }
}