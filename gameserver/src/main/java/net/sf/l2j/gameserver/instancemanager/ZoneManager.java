package net.sf.l2j.gameserver.instancemanager;

import net.sf.l2j.commons.serialize.Serializer;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.location.ChaoticLocation;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.model.world.L2WorldRegion;
import net.sf.l2j.gameserver.model.zone.L2SpawnZone;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.model.zone.data.ZoneElement;
import net.sf.l2j.gameserver.model.zone.data.ZoneStatElement;
import net.sf.l2j.gameserver.model.zone.data.ZoneXmlFile;
import net.sf.l2j.gameserver.model.zone.form.ZoneCuboid;
import net.sf.l2j.gameserver.model.zone.form.ZoneCylinder;
import net.sf.l2j.gameserver.model.zone.form.ZoneNPoly;
import net.sf.l2j.gameserver.model.zone.type.L2ArenaZone;
import net.sf.l2j.gameserver.model.zone.type.L2OlympiadStadiumZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public class ZoneManager {
    protected static final Logger LOGGER = LoggerFactory.getLogger(ZoneManager.class);

    private final Map<Class<? extends L2ZoneType>, Map<Integer, ? extends L2ZoneType>> zonesByClass = new HashMap<>();

    private final List<L2ItemInstance> debugItems = new ArrayList<>();
    private int idFactory;

    private ZoneManager() {
        load();
    }

    public static ZoneManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public static L2ArenaZone getArena(L2Character character) {
        if (character == null) { return null; }

        for (L2ZoneType temp : ZoneManager.getZones(character.getX(), character.getY(), character.getZ())) {
            if (temp instanceof L2ArenaZone && temp.isCharacterInZone(character)) { return (L2ArenaZone) temp; }
        }

        return null;
    }

    public static L2OlympiadStadiumZone getOlympiadStadium(L2Character character) {
        if (character == null) { return null; }

        for (L2ZoneType temp : ZoneManager.getZones(character.getX(), character.getY(), character.getZ())) {
            if (temp instanceof L2OlympiadStadiumZone && temp.isCharacterInZone(character)) { return (L2OlympiadStadiumZone) temp; }
        }
        return null;
    }

    public void reload() {
        // Get the world regions
        int count = 0;
        for (L2WorldRegion[] worldRegion : L2World.getInstance().getWorldRegions()) {
            for (L2WorldRegion element : worldRegion) {
                element.getZones().clear();
                count++;
            }
        }
        GrandBossManager.getInstance().getZones().clear();
        LOGGER.info("Removed zones in {} regions.", count);

        // Load the zones
        load();

        for (L2Object o : L2World.getInstance().getObjects()) {
            if (o instanceof L2Character) { ((L2Character) o).revalidateZone(true); }
        }
    }

    private void load() {
        LOGGER.info("Loading zones...");
        zonesByClass.clear();

        // Load the zone xml
        try {
            File mainDir = new File("./data/xml/zones");
            if (!mainDir.isDirectory()) {
                LOGGER.error("ZoneManager: Main dir {} hasn't been found.", mainDir.getAbsolutePath());
                return;
            }

            int fileCounter = 0;
            for (File file : mainDir.listFiles()) {
                if (file.isFile() && file.getName().endsWith(".xml")) {
                    // Set dynamically the ID range of next XML loading file.
                    idFactory = fileCounter++ * 1000;
                    loadFileZone(file);
                }
            }
        }
        catch (IOException e) {
            LOGGER.error("Error while loading zones.", e);
            return;
        }

        LOGGER.info("ZoneManager: loaded {} zones classes and {} zones.", zonesByClass.size(), getSize());
    }

    private void loadFileZone(File file) throws IOException {
        ZoneXmlFile zoneXmlFile = Serializer.MAPPER.readValue(file, ZoneXmlFile.class);
        for (ZoneElement zoneElement : zoneXmlFile.getZones()) {
            int zoneId = idFactory++;
            if (zoneElement.getId() != null) {
                zoneId = zoneElement.getId();
            }

            L2ZoneType zone = zoneElement.getType().instantiate(zoneId);
            switch (zoneElement.getShape()) {
                case CUBOID:
                    if (zoneElement.getNodes().length != 2) {
                        LOGGER.warn("Wrong cuboid node data in zone file {}, zone id={}, name={}", file.getName(), zoneElement.getId(), zoneElement.getName());
                        continue;
                    }
                    zone.setZone(new ZoneCuboid(
                            zoneElement.getNodes()[0].getX(),
                            zoneElement.getNodes()[0].getY(),
                            zoneElement.getNodes()[1].getX(),
                            zoneElement.getNodes()[1].getY(),
                            zoneElement.getMinZ(),
                            zoneElement.getMaxZ()
                    ));
                    break;
                case CYLINDER:
                    if (zoneElement.getNodes().length != 1) {
                        LOGGER.warn("Wrong cylinder node data in zone file {}, zone id={}, name={}", file.getName(), zoneElement.getId(), zoneElement.getName());
                        continue;
                    }
                    zone.setZone(new ZoneCylinder(
                            zoneElement.getNodes()[0].getX(),
                            zoneElement.getNodes()[0].getY(),
                            zoneElement.getMinZ(),
                            zoneElement.getMaxZ(),
                            zoneElement.getRadius()
                    ));
                    break;
                case NPOLY:
                    if (zoneElement.getNodes().length <= 2) {
                        LOGGER.warn("Wrong npoly node data in zone file {}, zone id={}, name={}", file.getName(), zoneElement.getId(), zoneElement.getName());
                        continue;
                    }
                    int[] positionsX = new int[zoneElement.getNodes().length];
                    int[] positionsY = new int[zoneElement.getNodes().length];
                    for (int i = 0; i < zoneElement.getNodes().length; i++) {
                        positionsX[i] = zoneElement.getNodes()[i].getX();
                        positionsY[i] = zoneElement.getNodes()[i].getY();
                    }
                    zone.setZone(new ZoneNPoly(
                            positionsX,
                            positionsY,
                            zoneElement.getMinZ(),
                            zoneElement.getMaxZ()
                    ));
                    break;
                case UNKNOWN:
                    LOGGER.warn("Unknown zone shape in file {}, zone id={}, name={}", file.getName(), zoneElement.getId(), zoneElement.getName());
                    break;
            }

            for (ZoneStatElement statElement : zoneElement.getStats()) {
                zone.setParameter(statElement.getName(), statElement.getVal());
            }

            if (zone instanceof L2SpawnZone) {
                for (ChaoticLocation location : zoneElement.getSpawns()) {
                    if (location.isChaotic()) {
                        ((L2SpawnZone) zone).addChaoticSpawn(location);
                    }
                    else {
                        ((L2SpawnZone) zone).addSpawn(location);
                    }
                }
            }

            addZone(zoneId, zone);

            // Register the zone into any world region it intersects with...
            // currently 11136 test for each zone :>
            for (int x = 0; x < L2World.getInstance().getWorldRegions().length; x++) {
                for (int y = 0; y < L2World.getInstance().getWorldRegions()[x].length; y++) {
                    if (zone.getZone().isIntersectsRectangle(L2World.getRegionX(x), L2World.getRegionX(x + 1), L2World.getRegionY(y), L2World.getRegionY(y + 1))) {
                        L2World.getInstance().getWorldRegions()[x][y].addZone(zone);
                    }
                }
            }

        }
    }

    public int getSize() {
        int i = 0;
        for (Map<Integer, ? extends L2ZoneType> map : zonesByClass.values()) {
            i += map.size();
        }
        return i;
    }

    public <T extends L2ZoneType> void addZone(Integer id, T zone) {
        // _zones.put(id, zone);
        Map<Integer, T> map = (Map<Integer, T>) zonesByClass.get(zone.getClass());
        if (map == null) {
            map = new HashMap<>();
            map.put(id, zone);
            zonesByClass.put(zone.getClass(), map);
        }
        else { map.put(id, zone); }
    }

    public <T extends L2ZoneType> Collection<T> getAllZones(Class<T> zoneType) {
        return (Collection<T>) zonesByClass.get(zoneType).values();
    }

    public L2ZoneType getZoneById(int id) {
        for (Map<Integer, ? extends L2ZoneType> map : zonesByClass.values()) {
            if (map.containsKey(id)) { return map.get(id); }
        }
        return null;
    }

    public <T extends L2ZoneType> T getZoneById(int id, Class<T> zoneType) {
        return (T) zonesByClass.get(zoneType).get(id);
    }

    public static List<L2ZoneType> getZones(L2Object object) {
        return getZones(object.getX(), object.getY(), object.getZ());
    }

    public static <T extends L2ZoneType> T getZone(L2Object object, Class<T> type) {
        if (object == null) { return null; }
        return getZone(object.getX(), object.getY(), object.getZ(), type);
    }

    public static List<L2ZoneType> getZones(int x, int y) {
        L2WorldRegion region = L2World.getInstance().getRegion(x, y);
        List<L2ZoneType> temp = new ArrayList<>();
        for (L2ZoneType zone : region.getZones()) {
            if (zone.isInsideZone(x, y)) { temp.add(zone); }
        }
        return temp;
    }

    public static List<L2ZoneType> getZones(int x, int y, int z) {
        L2WorldRegion region = L2World.getInstance().getRegion(x, y);
        List<L2ZoneType> temp = new ArrayList<>();
        for (L2ZoneType zone : region.getZones()) {
            if (zone.isInsideZone(x, y, z)) { temp.add(zone); }
        }
        return temp;
    }

    public static <T extends L2ZoneType> T getZone(int x, int y, int z, Class<T> type) {
        L2WorldRegion region = L2World.getInstance().getRegion(x, y);
        for (L2ZoneType zone : region.getZones()) {
            if (zone.isInsideZone(x, y, z) && type.isInstance(zone)) {
                return (T) zone;
            }
        }
        return null;
    }

    public List<L2ItemInstance> getDebugItems() {
        return debugItems;
    }

    public void clearDebugItems() {
        for (L2ItemInstance item : debugItems) { item.decayMe(); }
        debugItems.clear();
    }

    private static final class SingletonHolder {
        private static final ZoneManager INSTANCE = new ZoneManager();
    }
}