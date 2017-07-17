package net.sf.l2j.gameserver.model.zone;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import net.sf.l2j.gameserver.model.zone.type.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Johnson / 17.07.2017
 */
public enum EZoneType {
    ARENA("ArenaZone", L2ArenaZone.class),
    BOSS("BossZone", L2BossZone.class),
    CASTLE_TELEPORT_ZONE("CastleTeleportZone", L2CastleTeleportZone.class),
    CASTLE_ZONE("CastleZone", L2CastleZone.class),
    CLAN_HALL_ZONE("ClanHallZone", L2ClanHallZone.class),
    DAMAGE_ZONE("DamageZone", L2DamageZone.class),
    DERBY_TRACK_ZONE("DerbyTrackZone", L2DerbyTrackZone.class),
    DYNAMIC_ZONE("DynamicZone", L2DynamicZone.class),
    EFFECT_ZONE("EffectZone", L2EffectZone.class),
    FISHING_ZONE("FishingZone", L2FishingZone.class),
    HQ_ZONE("HqZone", L2HqZone.class),
    JAIL_ZONE("JailZone", L2JailZone.class),
    MOTHER_TREE_ZONE("MotherTreeZone", L2MotherTreeZone.class),
    NO_LANDING_ZONE("NoLandingZone", L2NoLandingZone.class),
    NO_RESTART_ZONE("NoRestartZone", L2NoRestartZone.class),
    NO_STORE_ZONE("NoStoreZone", L2NoStoreZone.class),
    NO_SUMMON_FRIEND_ZONE("NoSummonFriendZone", L2NoSummonFriendZone.class),
    OLYMPIAD_STADIUM_ZONE("OlympiadStadiumZone", L2OlympiadStadiumZone.class),
    PEACE_ZONE("PeaceZone", L2PeaceZone.class),
    PRAYER_ZONE("PrayerZone", L2PrayerZone.class),
    SCRIPT_ZONE("ScriptZone", L2ScriptZone.class),
    SIEGE_ZONE("SiegeZone", L2SiegeZone.class),
    SWAMP_ZONE("SwampZone", L2SwampZone.class),
    TOWN_ZONE("TownZone", L2TownZone.class),
    WATER_ZONE("WaterZone", L2WaterZone.class),

    UNKNOWN("", null);

    private final String code;
    private final Class<? extends L2ZoneType> zoneClass;

    EZoneType(String code, Class<? extends L2ZoneType> zoneClass) {
        this.code = code;
        this.zoneClass = zoneClass;
    }

    @JsonCreator
    public static EZoneType getByCode(String code) {
        for (EZoneType zoneType : values()) {
            if (zoneType.code.equals(code)) {
                return zoneType;
            }
        }
        return UNKNOWN;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public Class<? extends L2ZoneType> getZoneClass() {
        return zoneClass;
    }

    public L2ZoneType instantiate(int zoneId) {
        try {
            Constructor<? extends L2ZoneType> constructor = zoneClass.getConstructor(int.class);
            return constructor.newInstance(zoneId);
        }
        catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
            throw new IllegalArgumentException("Failed to instantiate zone " + name() + " with zoneId " + zoneId, e);
        }
    }
}
