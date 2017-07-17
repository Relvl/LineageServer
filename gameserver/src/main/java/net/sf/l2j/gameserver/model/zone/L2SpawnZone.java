package net.sf.l2j.gameserver.model.zone;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.location.ChaoticLocation;
import net.sf.l2j.gameserver.model.location.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract zone with spawn locations
 *
 * @author DS
 */
public abstract class L2SpawnZone extends L2ZoneType {
    private List<Location> locations;
    private List<Location> chaoticSpawnLocs;

    public L2SpawnZone(int id) {
        super(id);
    }

    public final void addSpawn(int x, int y, int z) {
        if (locations == null) { locations = new ArrayList<>(); }
        locations.add(new Location(x, y, z));
    }

    public final void addSpawn(Location location) {
        if (locations == null) { locations = new ArrayList<>(); }
        locations.add(location);
    }

    public final void addChaoticSpawn(int x, int y, int z) {
        if (chaoticSpawnLocs == null) { chaoticSpawnLocs = new ArrayList<>(); }
        chaoticSpawnLocs.add(new Location(x, y, z));
    }

    public final void addChaoticSpawn(ChaoticLocation location) {
        if (chaoticSpawnLocs == null) { chaoticSpawnLocs = new ArrayList<>(); }
        chaoticSpawnLocs.add(location);
    }

    public final List<Location> getSpawns() {
        return locations;
    }

    public final Location getSpawnLoc() {
        return Rnd.get(locations);
    }

    public final Location getChaoticSpawnLoc() {
        if (chaoticSpawnLocs != null) { return Rnd.get(chaoticSpawnLocs); }
        return getSpawnLoc();
    }
}