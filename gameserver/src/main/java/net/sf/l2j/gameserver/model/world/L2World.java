package net.sf.l2j.gameserver.model.world;

import net.sf.l2j.gameserver.datatables.CharNameTable;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.location.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class L2World {
    // Geodata min/max tiles
    public static final int TILE_X_MIN = 16;
    public static final int TILE_X_MAX = 26;
    public static final int TILE_Y_MIN = 10;
    public static final int TILE_Y_MAX = 25;
    // Map dimensions
    public static final int TILE_SIZE = 32768;
    public static final int WORLD_X_MIN = (TILE_X_MIN - 20) * TILE_SIZE;
    public static final int WORLD_X_MAX = (TILE_X_MAX - 19) * TILE_SIZE;
    public static final int WORLD_Y_MIN = (TILE_Y_MIN - 18) * TILE_SIZE;
    public static final int WORLD_Y_MAX = (TILE_Y_MAX - 17) * TILE_SIZE;

    private static final Logger LOGGER = LoggerFactory.getLogger(L2World.class);

    // Regions and offsets
    private static final int REGION_SIZE = 4096;
    private static final int REGIONS_X = (WORLD_X_MAX - WORLD_X_MIN) / REGION_SIZE;
    private static final int REGIONS_Y = (WORLD_Y_MAX - WORLD_Y_MIN) / REGION_SIZE;
    private static final int REGION_X_OFFSET = Math.abs(WORLD_X_MIN / REGION_SIZE);
    private static final int REGION_Y_OFFSET = Math.abs(WORLD_Y_MIN / REGION_SIZE);

    private final Map<Integer, L2Object> objects = new ConcurrentHashMap<>();
    private final Map<Integer, L2PetInstance> pets = new ConcurrentHashMap<>();
    private final Map<Integer, L2PcInstance> players = new ConcurrentHashMap<>();

    private final L2WorldRegion[][] worldRegions = new L2WorldRegion[REGIONS_X + 1][REGIONS_Y + 1];

    private L2World() {
        for (int i = 0; i <= REGIONS_X; i++) {
            for (int j = 0; j <= REGIONS_Y; j++) {
                worldRegions[i][j] = new L2WorldRegion(i, j);
            }
        }

        for (int x = 0; x <= REGIONS_X; x++) {
            for (int y = 0; y <= REGIONS_Y; y++) {
                for (int neighborX = -1; neighborX <= 1; neighborX++) {
                    for (int neighborY = -1; neighborY <= 1; neighborY++) {
                        if (isValidRegion(x + neighborX, y + neighborY)) {
                            worldRegions[x + neighborX][y + neighborY].addSurroundingRegion(worldRegions[x][y]);
                        }
                    }
                }
            }
        }
        LOGGER.info("L2World: WorldRegion grid (" + REGIONS_X + " by " + REGIONS_Y + ") is now setted up.");
    }

    public static List<L2Object> getVisibleObjects(L2Object object, int radius) {
        if (object == null || !object.isVisible()) {
            return Collections.emptyList();
        }

        int x = object.getX();
        int y = object.getY();
        int sqRadius = radius * radius;

        List<L2Object> result = new ArrayList<>();

        for (L2WorldRegion region : object.getWorldRegion().getNeighbors()) {
            for (L2Object visibleObject : region.getVisibleObjects().values()) {
                if (visibleObject == null || visibleObject.equals(object)) {
                    continue;
                }

                int x1 = visibleObject.getX();
                int y1 = visibleObject.getY();

                double dx = x1 - x;
                double dy = y1 - y;

                if (dx * dx + dy * dy < sqRadius) { result.add(visibleObject); }
            }
        }

        return result;
    }

    public static int getRegionX(int regionX) {
        return (regionX - REGION_X_OFFSET) * REGION_SIZE;
    }

    public static int getRegionY(int regionY) {
        return (regionY - REGION_Y_OFFSET) * REGION_SIZE;
    }

    private static boolean isValidRegion(int x, int y) {
        return x >= 0 && x <= REGIONS_X && y >= 0 && y <= REGIONS_Y;
    }

    public static L2World getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public void addObject(L2Object object) {
        objects.putIfAbsent(object.getObjectId(), object);
    }

    public void removeObject(L2Object object) {
        objects.remove(object.getObjectId());
    }

    public Collection<L2Object> getObjects() {
        return objects.values();
    }

    public L2Object getObject(int objectId) {
        return objects.get(objectId);
    }

    public void addPlayer(L2PcInstance cha) {
        players.putIfAbsent(cha.getObjectId(), cha);
    }

    public void removePlayer(L2PcInstance cha) {
        players.remove(cha.getObjectId());
    }

    public Collection<L2PcInstance> getPlayers() {
        return players.values();
    }

    public L2PcInstance getPlayer(String name) {
        return getPlayer(CharNameTable.getInstance().getIdByName(name));
    }

    public L2PcInstance getPlayer(int objectId) {
        return players.get(objectId);
    }

    public L2PetInstance addPet(int ownerId, L2PetInstance pet) {
        return pets.putIfAbsent(ownerId, pet);
    }

    public void removePet(int ownerId) {
        pets.remove(ownerId);
    }

    public L2PetInstance getPet(int ownerId) {
        return pets.get(ownerId);
    }

    public void addVisibleObject(L2Object object, L2WorldRegion newRegion) {
        if (object.isPlayer()) {
            L2PcInstance player = (L2PcInstance) object;

            if (!player.isTeleporting()) {
                L2PcInstance tmp = players.get(player.getObjectId());
                if (tmp != null) {
                    LOGGER.warn("Duplicate character!? Closing both characters (" + player.getName() + ")");
                    player.logout();
                    tmp.logout();
                    return;
                }
                players.putIfAbsent(player.getObjectId(), player);
            }
        }

        if (!newRegion.isActive()) { return; }

        for (L2Object visible : getVisibleObjects(object, 2000)) {
            if (visible.getKnownList() != null) {
                visible.getKnownList().addKnownObject(object);
            }
            if (object.getKnownList() != null) {
                object.getKnownList().addKnownObject(visible);
            }
        }
    }

    public void removeVisibleObject(L2Object object, L2WorldRegion oldRegion) {
        if (object == null) { return; }

        if (oldRegion != null) {
            oldRegion.removeVisibleObject(object);

            boolean objectHasKnownlist = object.getKnownList() != null;

            for (L2WorldRegion reg : oldRegion.getNeighbors()) {
                for (L2Object obj : reg.getVisibleObjects().values()) {
                    if (obj.getKnownList() != null) {
                        obj.getKnownList().removeKnownObject(object);
                    }

                    if (objectHasKnownlist) {
                        object.getKnownList().removeKnownObject(obj);
                    }
                }
            }

            if (objectHasKnownlist) {
                object.getKnownList().removeAllKnownObjects();
            }

            if (object.isPlayer()) {
                if (!((L2PcInstance) object).isTeleporting()) {
                    removePlayer((L2PcInstance) object);
                }
            }
        }
    }

    public L2WorldRegion getRegion(Location point) {
        return getRegion(point.getX(), point.getY());
    }

    public L2WorldRegion getRegion(int x, int y) {
        return worldRegions[(x - WORLD_X_MIN) / REGION_SIZE][(y - WORLD_Y_MIN) / REGION_SIZE];
    }

    public L2WorldRegion[][] getWorldRegions() {
        return worldRegions;
    }

    private static final class SingletonHolder {
        private static final L2World INSTANCE = new L2World();
    }
}