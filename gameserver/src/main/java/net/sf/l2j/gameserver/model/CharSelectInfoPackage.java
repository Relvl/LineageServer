package net.sf.l2j.gameserver.model;

import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.gameserver.model.item.EPaperdollSlot;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.EnumMap;
import java.util.Map;

public class CharSelectInfoPackage {
    private String _name;
    private int _objectId;
    private int _charId = 0x00030b7a;
    private long _exp;
    private int _sp;
    private int _clanId;
    private int _race;
    private int _classId;
    private int _baseClassId;
    private long _deleteTimer;
    private long _lastAccess;
    private int _face;
    private int _hairStyle;
    private int _hairColor;
    private int _sex;
    private int _level = 1;
    private int _maxHp;
    private double _currentHp;
    private int _maxMp;
    private double _currentMp;
    private int _karma;
    private int _pkKills;
    private int _pvpKills;
    private int _augmentationId;
    private int _x;
    private int _y;
    private int _z;
    private int _accessLevel;

    private final Map<EPaperdollSlot, int[]> paperdoll = new EnumMap<>(EPaperdollSlot.class);

    public CharSelectInfoPackage(int objectId, String name) {
        _objectId = objectId;
        _name = name;
        int[][] restoreVisibleInventory = restoreVisibleInventory(objectId);
        for (int i = 0; i < restoreVisibleInventory.length; i++) {
            paperdoll.put(EPaperdollSlot.getByIndex(i), restoreVisibleInventory[i]);
        }
    }

    public static int[][] restoreVisibleInventory(int objectId) {
        int[][] paperdoll = new int[EPaperdollSlot.values().length][3];
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement2 = con.prepareStatement("SELECT object_id,item_id,loc_data,enchant_level FROM items WHERE owner_id=? AND loc='PAPERDOLL'");
            statement2.setInt(1, objectId);
            ResultSet invdata = statement2.executeQuery();
            while (invdata.next()) {
                int slot = invdata.getInt("loc_data");
                paperdoll[slot][0] = invdata.getInt("object_id");
                paperdoll[slot][1] = invdata.getInt("item_id");
                paperdoll[slot][2] = invdata.getInt("enchant_level");
            }
            invdata.close();
            statement2.close();
        }
        catch (Exception e) {
            LoggerFactory.getLogger(CharSelectInfoPackage.class).error("Could not restore inventory: {}", e.getMessage(), e);
        }
        return paperdoll;
    }

    public int getObjectId() {
        return _objectId;
    }

    public void setObjectId(int objectId) {
        _objectId = objectId;
    }

    public int getAccessLevel() {
        return _accessLevel;
    }

    public void setAccessLevel(int level) {
        _accessLevel = level;
    }

    public int getCharId() {
        return _charId;
    }

    public void setCharId(int charId) {
        _charId = charId;
    }

    public int getClanId() {
        return _clanId;
    }

    public void setClanId(int clanId) {
        _clanId = clanId;
    }

    public int getClassId() {
        return _classId;
    }

    public void setClassId(int classId) {
        _classId = classId;
    }

    public int getBaseClassId() {
        return _baseClassId;
    }

    public void setBaseClassId(int baseClassId) {
        _baseClassId = baseClassId;
    }

    public double getCurrentHp() {
        return _currentHp;
    }

    public void setCurrentHp(double currentHp) {
        _currentHp = currentHp;
    }

    public double getCurrentMp() {
        return _currentMp;
    }

    public void setCurrentMp(double currentMp) {
        _currentMp = currentMp;
    }

    public long getDeleteTimer() {
        return _deleteTimer;
    }

    public void setDeleteTimer(long deleteTimer) {
        _deleteTimer = deleteTimer;
    }

    public long getLastAccess() {
        return _lastAccess;
    }

    public void setLastAccess(long lastAccess) {
        _lastAccess = lastAccess;
    }

    public long getExp() {
        return _exp;
    }

    public void setExp(long exp) {
        _exp = exp;
    }

    public int getFace() {
        return _face;
    }

    public void setFace(int face) {
        _face = face;
    }

    public int getHairColor() {
        return _hairColor;
    }

    public void setHairColor(int hairColor) {
        _hairColor = hairColor;
    }

    public int getHairStyle() {
        return _hairStyle;
    }

    public void setHairStyle(int hairStyle) {
        _hairStyle = hairStyle;
    }

    public int getPaperdollObjectId(EPaperdollSlot slot) {
        return paperdoll.get(slot)[0];
    }

    public int getPaperdollItemId(EPaperdollSlot slot) {
        return paperdoll.get(slot)[1];
    }

    public int getLevel() {
        return _level;
    }

    public void setLevel(int level) {
        _level = level;
    }

    public int getMaxHp() {
        return _maxHp;
    }

    public void setMaxHp(int maxHp) {
        _maxHp = maxHp;
    }

    public int getMaxMp() {
        return _maxMp;
    }

    public void setMaxMp(int maxMp) {
        _maxMp = maxMp;
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public int getRace() {
        return _race;
    }

    public void setRace(int race) {
        _race = race;
    }

    public int getSex() {
        return _sex;
    }

    public void setSex(int sex) {
        _sex = sex;
    }

    public int getSp() {
        return _sp;
    }

    public void setSp(int sp) {
        _sp = sp;
    }

    public int getEnchantEffect() {
        return paperdoll.get(EPaperdollSlot.PAPERDOLL_RHAND)[2];
    }

    public int getKarma() {
        return _karma;
    }

    public void setKarma(int k) {
        _karma = k;
    }

    public int getAugmentationId() {
        return _augmentationId;
    }

    public void setAugmentationId(int augmentationId) {
        _augmentationId = augmentationId;
    }

    public int getPkKills() {
        return _pkKills;
    }

    public void setPkKills(int PkKills) {
        _pkKills = PkKills;
    }

    public int getPvPKills() {
        return _pvpKills;
    }

    public void setPvPKills(int PvPKills) {
        _pvpKills = PvPKills;
    }

    public int getX() {
        return _x;
    }

    public void setX(int x) {
        _x = x;
    }

    public int getY() {
        return _y;
    }

    public void setY(int y) {
        _y = y;
    }

    public int getZ() {
        return _z;
    }

    public void setZ(int z) {
        _z = z;
    }
}