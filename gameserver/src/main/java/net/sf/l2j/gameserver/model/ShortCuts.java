package net.sf.l2j.gameserver.model;

import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.type.EtcItemType;
import net.sf.l2j.gameserver.network.client.game_to_client.ExAutoSoulShot;
import net.sf.l2j.gameserver.network.client.game_to_client.ShortCutInit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.TreeMap;

public class ShortCuts {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShortCuts.class);

    private static final int MAX_SHORTCUTS_PER_BAR = 12;
    private final L2PcInstance _owner;
    private final Map<Integer, L2ShortCut> _shortCuts = new TreeMap<>();

    public ShortCuts(L2PcInstance owner) {
        _owner = owner;
    }

    public L2ShortCut[] getAllShortCuts() {
        return _shortCuts.values().toArray(new L2ShortCut[_shortCuts.values().size()]);
    }

    public L2ShortCut getShortCut(int slot, int page) {
        L2ShortCut sc = _shortCuts.get(slot + (page * MAX_SHORTCUTS_PER_BAR));

        // verify shortcut
        if (sc != null && sc.getType() == L2ShortCut.TYPE_ITEM) {
            if (_owner.getInventory().getItemByObjectId(sc.getId()) == null) {
                deleteShortCut(sc.getSlot(), sc.getPage());
                sc = null;
            }
        }
        return sc;
    }

    public synchronized void registerShortCut(L2ShortCut shortcut) {
        // verify shortcut
        if (shortcut.getType() == L2ShortCut.TYPE_ITEM) {
            L2ItemInstance item = _owner.getInventory().getItemByObjectId(shortcut.getId());
            if (item == null) { return; }

            if (item.isEtcItem()) { shortcut.setSharedReuseGroup(item.getEtcItem().getSharedReuseGroup()); }
        }
        L2ShortCut oldShortCut = _shortCuts.put(shortcut.getSlot() + (shortcut.getPage() * MAX_SHORTCUTS_PER_BAR), shortcut);
        registerShortCutInDb(shortcut, oldShortCut);
    }

    private void registerShortCutInDb(L2ShortCut shortcut, L2ShortCut oldShortCut) {
        if (oldShortCut != null) { deleteShortCutFromDb(oldShortCut); }

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("REPLACE INTO character_shortcuts (char_obj_id,slot,page,type,shortcut_id,level,class_index) VALUES(?,?,?,?,?,?,?)");
            statement.setInt(1, _owner.getObjectId());
            statement.setInt(2, shortcut.getSlot());
            statement.setInt(3, shortcut.getPage());
            statement.setInt(4, shortcut.getType());
            statement.setInt(5, shortcut.getId());
            statement.setInt(6, shortcut.getLevel());
            statement.setInt(7, _owner.getClassIndex());
            statement.execute();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Could not store character shortcut: {}", e.getMessage(), e);
        }
    }

    /**
     * @param slot
     * @param page
     */
    public synchronized void deleteShortCut(int slot, int page) {
        L2ShortCut old = _shortCuts.remove(slot + page * 12);
        if (old == null || _owner == null) { return; }

        deleteShortCutFromDb(old);
        if (old.getType() == L2ShortCut.TYPE_ITEM) {
            L2ItemInstance item = _owner.getInventory().getItemByObjectId(old.getId());

            if (item != null && item.getItemType() == EtcItemType.SHOT) {
                if (_owner.removeAutoSoulShot(item.getItemId())) { _owner.sendPacket(new ExAutoSoulShot(item.getItemId(), 0)); }
            }
        }

        _owner.sendPacket(new ShortCutInit(_owner));

        for (int shotId : _owner.getAutoSoulShot()) { _owner.sendPacket(new ExAutoSoulShot(shotId, 1)); }
    }

    public synchronized void deleteShortCutByObjectId(int objectId) {
        for (L2ShortCut shortcut : _shortCuts.values()) {
            if (shortcut.getType() == L2ShortCut.TYPE_ITEM && shortcut.getId() == objectId) {
                deleteShortCut(shortcut.getSlot(), shortcut.getPage());
                break;
            }
        }
    }

    /**
     * @param shortcut
     */
    private void deleteShortCutFromDb(L2ShortCut shortcut) {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("DELETE FROM character_shortcuts WHERE char_obj_id=? AND slot=? AND page=? AND class_index=?");
            statement.setInt(1, _owner.getObjectId());
            statement.setInt(2, shortcut.getSlot());
            statement.setInt(3, shortcut.getPage());
            statement.setInt(4, _owner.getClassIndex());
            statement.execute();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Could not delete character shortcut: {}", e.getMessage(), e);
        }
    }

    public void restore() {
        _shortCuts.clear();
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT char_obj_id, slot, page, type, shortcut_id, level FROM character_shortcuts WHERE char_obj_id=? AND class_index=?");
            statement.setInt(1, _owner.getObjectId());
            statement.setInt(2, _owner.getClassIndex());

            ResultSet rset = statement.executeQuery();

            while (rset.next()) {
                int slot = rset.getInt("slot");
                int page = rset.getInt("page");
                int type = rset.getInt("type");
                int id = rset.getInt("shortcut_id");
                int level = rset.getInt("level");

                L2ShortCut sc = new L2ShortCut(slot, page, type, id, level, 1);
                _shortCuts.put(slot + (page * MAX_SHORTCUTS_PER_BAR), sc);
            }

            rset.close();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Could not restore character shortcuts: {}", e.getMessage(), e);
        }

        // verify shortcuts
        for (L2ShortCut sc : getAllShortCuts()) {
            if (sc.getType() == L2ShortCut.TYPE_ITEM) {
                L2ItemInstance item = _owner.getInventory().getItemByObjectId(sc.getId());
                if (item == null) { deleteShortCut(sc.getSlot(), sc.getPage()); }
                else if (item.isEtcItem()) { sc.setSharedReuseGroup(item.getEtcItem().getSharedReuseGroup()); }
            }
        }
    }
}