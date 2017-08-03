package net.sf.l2j.gameserver.datatables;

import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.instancemanager.SevenSigns;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.PlayerRace;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.model.zone.type.L2ArenaZone;
import net.sf.l2j.gameserver.model.zone.type.L2ClanHallZone;
import net.sf.l2j.gameserver.model.zone.type.L2TownZone;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.File;
import java.util.List;

public class MapRegionTable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapRegionTable.class);

    private static final int REGIONS_X = 11;
    private static final int REGIONS_Y = 16;
    private static final int[][] _regions = new int[REGIONS_X][REGIONS_Y];
    private static final int[] _castleIdArray = {0, 0, 0, 0, 0, 1, 0, 2, 3, 4, 5, 0, 0, 6, 8, 7, 9, 0, 0};

    protected MapRegionTable() {
        int count = 0;

        try {
            File f = new File("./data/xml/map_region.xml");
            Document doc = XMLDocumentFactory.getInstance().loadDocument(f);

            Node n = doc.getFirstChild();
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                if (d.getNodeName().equalsIgnoreCase("map")) {
                    NamedNodeMap attrs = d.getAttributes();
                    int rY = Integer.valueOf(attrs.getNamedItem("geoY").getNodeValue()) - 10;
                    for (int rX = 0; rX < REGIONS_X; rX++) {
                        _regions[rX][rY] = Integer.valueOf(attrs.getNamedItem("geoX_" + (rX + 16)).getNodeValue());
                        count++;
                    }
                }
            }
        }
        catch (Exception e) {
            LOGGER.error("MapRegionTable: Error while loading \"map_region.xml\".", e);
        }
        LOGGER.info("MapRegionTable: Loaded {} regions.", count);
    }

    public static MapRegionTable getInstance() {
        return SingletonHolder._instance;
    }

    public static int getMapRegion(int posX, int posY) {
        try {
            return _regions[getMapRegionX(posX)][getMapRegionY(posY)];
        }
        catch (ArrayIndexOutOfBoundsException e) {
            // Position sent is outside MapRegionTable area.
            LOGGER.error("MapRegionTable: Player outside map regions at X,Y={},{}", posX, posY, e);

            return 0;
        }
    }

    public static int getMapRegionX(int posX) {
        // +4 to shift coords center
        return (posX >> 15) + 4;
    }

    public static int getMapRegionY(int posY) {
        // +8 to shift coords center
        return (posY >> 15) + 8;
    }

    /**
     * @param x
     * @param y
     * @return the castle id associated to the town, based on X/Y points.
     */
    public static int getAreaCastle(int x, int y) {
        switch (getMapRegion(x, y)) {
            case 0: // Talking Island Village
            case 5: // Town of Gludio
            case 6: // Gludin Village
                return 1;

            case 7: // Town of Dion
                return 2;

            case 8: // Town of Giran
            case 12: // Giran Harbor
                return 3;

            case 1: // Elven Village
            case 2: // Dark Elven Village
            case 9: // Town of Oren
            case 17: // Floran Village
                return 4;

            case 10: // Town of Aden
            case 11: // Hunters Village
            default: // Town of Aden
                return 5;

            case 13: // Heine
                return 6;

            case 15: // Town of Goddard
                return 7;

            case 14: // Rune Township
            case 18: // Primeval Isle Wharf
                return 8;

            case 3: // Orc Village
            case 4: // Dwarven Village
            case 16: // Town of Schuttgart
                return 9;
        }
    }

    /**
     * A specific method, used ONLY by players. There's a Race condition.
     *
     * @param race : The Race of the player, got an effect for Elf and Dark Elf.
     * @param x    : The current player's X location.
     * @param y    : The current player's Y location.
     * @return the closest L2TownZone based on a X/Y location.
     */
    private static L2TownZone getClosestTown(PlayerRace race, int x, int y) {
        switch (getMapRegion(x, y)) {
            case 0: // TI
                return getTown(2);

            case 1:// Elven
                return getTown((race == PlayerRace.DarkElf) ? 1 : 3);

            case 2:// DE
                return getTown((race == PlayerRace.Elf) ? 3 : 1);

            case 3: // Orc
                return getTown(4);

            case 4:// Dwarven
                return getTown(6);

            case 5:// Gludio
                return getTown(7);

            case 6:// Gludin
                return getTown(5);

            case 7: // Dion
                return getTown(8);

            case 8: // Giran
            case 12: // Giran Harbor
                return getTown(9);

            case 9: // Oren
                return getTown(10);

            case 10: // Aden
                return getTown(12);

            case 11: // HV
                return getTown(11);

            case 13: // Heine
                return getTown(15);

            case 14: // Rune
                return getTown(14);

            case 15: // Goddard
                return getTown(13);

            case 16: // Schuttgart
                return getTown(17);

            case 17:// Floran
                return getTown(16);

            case 18:// Primeval Isle
                return getTown(19);
        }
        return getTown(16); // Default to floran
    }

    /**
     * @param x : The current character's X location.
     * @param y : The current character's Y location.
     * @return the closest L2TownZone based on a X/Y location.
     */
    private static L2TownZone getClosestTown(int x, int y) {
        switch (getMapRegion(x, y)) {
            case 0: // TI
                return getTown(2);

            case 1:// Elven
                return getTown(3);

            case 2:// DE
                return getTown(1);

            case 3: // Orc
                return getTown(4);

            case 4:// Dwarven
                return getTown(6);

            case 5:// Gludio
                return getTown(7);

            case 6:// Gludin
                return getTown(5);

            case 7: // Dion
                return getTown(8);

            case 8: // Giran
            case 12: // Giran Harbor
                return getTown(9);

            case 9: // Oren
                return getTown(10);

            case 10: // Aden
                return getTown(12);

            case 11: // HV
                return getTown(11);

            case 13: // Heine
                return getTown(15);

            case 14: // Rune
                return getTown(14);

            case 15: // Goddard
                return getTown(13);

            case 16: // Schuttgart
                return getTown(17);

            case 17:// Floran
                return getTown(16);

            case 18:// Primeval Isle
                return getTown(19);
        }
        return getTown(16); // Default to floran
    }

    /**
     * @param x : The current character's X location.
     * @param y : The current character's Y location.
     * @return the second closest L2TownZone based on a X/Y location.
     */
    private static L2TownZone getSecondClosestTown(int x, int y) {
        switch (getMapRegion(x, y)) {
            case 0: // TI
            case 1: // Elven
            case 2: // DE
            case 5: // Gludio
            case 6: // Gludin
                return getTown(5);

            case 3: // Orc
                return getTown(4);

            case 4: // Dwarven
            case 16: // Schuttgart
                return getTown(6);

            case 7: // Dion
                return getTown(7);

            case 8: // Giran
            case 9: // Oren
            case 10:// Aden
            case 11: // HV
                return getTown(11);

            case 12: // Giran Harbour
            case 13: // Heine
            case 17:// Floran
                return getTown(16);

            case 14: // Rune
                return getTown(13);

            case 15: // Goddard
                return getTown(12);

            case 18: // Primeval Isle
                return getTown(19);
        }
        return getTown(16); // Default to floran
    }

    /**
     * @param x : The current character's X location.
     * @param y : The current character's Y location.
     * @return the closest region based on a X/Y location.
     */
    public static int getClosestLocation(int x, int y) {
        switch (getMapRegion(x, y)) {
            case 0: // TI
                return 1;

            case 1: // Elven
                return 4;

            case 2: // DE
                return 3;

            case 3: // Orc
            case 4: // Dwarven
            case 16:// Schuttgart
                return 9;

            case 5: // Gludio
            case 6: // Gludin
                return 2;

            case 7: // Dion
                return 5;

            case 8: // Giran
            case 12: // Giran Harbor
                return 6;

            case 9: // Oren
                return 10;

            case 10: // Aden
                return 13;

            case 11: // HV
                return 11;

            case 13: // Heine
                return 12;

            case 14: // Rune
                return 14;

            case 15: // Goddard
                return 15;
        }
        return 0;
    }

    /**
     * Retrieves town's siege statut.
     *
     * @param x coords to check.
     * @param y coords to check.
     * @return true if a siege is currently in progress in that town.
     */
    public static boolean townHasCastleInSiege(int x, int y) {
        final int castleIndex = _castleIdArray[getMapRegion(x, y)];
        if (castleIndex > 0) {
            final Castle castle = CastleManager.getInstance().getCastles().get(CastleManager.getInstance().getCastleIndex(castleIndex));
            if (castle != null) { return castle.getSiege().isInProgress(); }
        }
        return false;
    }

    /**
     * @param townId the townId to match.
     * @return a L2TownZone based on the overall list of L2TownZone, matching the townId.
     */
    public static L2TownZone getTown(int townId) {
        for (L2TownZone temp : ZoneManager.getInstance().getAllZones(L2TownZone.class)) {
            if (temp.getTownId() == townId) { return temp; }
        }
        return null;
    }

    /**
     * @param x coords to check.
     * @param y coords to check.
     * @param z coords to check.
     * @return a L2TownZone based on the overall list of zones, matching a 3D location.
     */
    public static L2TownZone getTown(int x, int y, int z) {
        for (L2ZoneType temp : ZoneManager.getZones(x, y, z)) {
            if (temp instanceof L2TownZone) { return (L2TownZone) temp; }
        }
        return null;
    }

    /**
     * @param x
     * @param y
     * @return a String consisting of town name, based on X/Y points.
     */
    public String getClosestTownName(int x, int y) {
        return getClosestTownName(getMapRegion(x, y));
    }

    public String getClosestTownName(int townId) {
        switch (townId) {
            case 0:
                return "Talking Island Village";

            case 1:
                return "Elven Village";

            case 2:
                return "Dark Elven Village";

            case 3:
                return "Orc Village";

            case 4:
                return "Dwarven Village";

            case 5:
                return "Town of Gludio";

            case 6:
                return "Gludin Village";

            case 7:
                return "Town of Dion";

            case 8:
                return "Town of Giran";

            case 9:
                return "Town of Oren";

            case 10:
                return "Town of Aden";

            case 11:
                return "Hunters Village";

            case 12:
                return "Giran Harbor";

            case 13:
                return "Heine";

            case 14:
                return "Rune Township";

            case 15:
                return "Town of Goddard";

            case 16:
                return "Town of Schuttgart";

            case 17:
                return "Floran Village";

            case 18:
                return "Primeval Isle";

            default:
                return "Town of Aden";
        }
    }

    public Location getTeleToLocation(L2Character activeChar, TeleportWhereType teleportWhere) {
        if (activeChar instanceof L2PcInstance) {
            L2PcInstance player = ((L2PcInstance) activeChar);

            // If in Monster Derby Track
            if (player.isInsideZone(ZoneId.MONSTER_TRACK)) { return new Location(12661, 181687, -3560); }

            Castle castle;
            ClanHall clanhall;

            if (player.getClan() != null) {
                // If teleport to clan hall
                if (teleportWhere == TeleportWhereType.ClanHall) {
                    clanhall = ClanHallManager.getInstance().getClanHallByOwner(player.getClan());
                    if (clanhall != null) {
                        L2ClanHallZone zone = clanhall.getZone();
                        if (zone != null) { return zone.getSpawnLoc(); }
                    }
                }

                // If teleport to castle
                if (teleportWhere == TeleportWhereType.Castle) {
                    castle = CastleManager.getInstance().getCastleByOwner(player.getClan());

                    // check if player is on castle and player's clan is defender
                    if (castle == null) {
                        castle = CastleManager.getInstance().getCastle(player);
                        if (!(castle != null && castle.getSiege().isInProgress() && castle.getSiege().getDefenderClan(player.getClan()) != null)) { castle = null; }
                    }

                    if (castle != null && castle.getCastleId() > 0) { return castle.getCastleZone().getSpawnLoc(); }
                }

                // If teleport to SiegeHQ
                if (teleportWhere == TeleportWhereType.SiegeFlag) {
                    castle = CastleManager.getInstance().getCastle(player);

                    if (castle != null && castle.getSiege().isInProgress()) {
                        // Check if player's clan is attacker
                        List<L2Npc> flags = castle.getSiege().getFlag(player.getClan());
                        if (flags != null && !flags.isEmpty()) {
                            // Spawn to flag - Need more work to get player to the nearest flag
                            L2Npc flag = flags.get(0);
                            return new Location(flag.getX(), flag.getY(), flag.getZ());
                        }
                    }
                }
            }

            // Karma player land out of city
            if (player.getKarma() > 0) { return getClosestTown(player.getTemplate().getRace(), activeChar.getX(), activeChar.getY()).getChaoticSpawnLoc(); }

            // Checking if in arena
            L2ArenaZone arena = ZoneManager.getArena(player);
            if (arena != null) {
                return arena.getSpawnLoc();
            }

            // Checking if needed to be respawned in "far" town from the castle;
            castle = CastleManager.getInstance().getCastle(player);
            if (castle != null) {
                if (castle.getSiege().isInProgress()) {
                    // Check if player's clan is participating
                    if ((castle.getSiege().checkIsDefender(player.getClan()) || castle.getSiege().checkIsAttacker(player.getClan())) && SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DAWN) {
                        return getSecondClosestTown(activeChar.getX(), activeChar.getY()).getSpawnLoc();
                    }
                }
            }

            // Get the nearest town
            return getClosestTown(player.getTemplate().getRace(), activeChar.getX(), activeChar.getY()).getSpawnLoc();
        }

        // Get the nearest town
        return getClosestTown(activeChar.getX(), activeChar.getY()).getSpawnLoc();
    }

    public static enum TeleportWhereType {
        Castle,
        ClanHall,
        SiegeFlag,
        Town
    }

    private static class SingletonHolder {
        protected static final MapRegionTable _instance = new MapRegionTable();
    }
}