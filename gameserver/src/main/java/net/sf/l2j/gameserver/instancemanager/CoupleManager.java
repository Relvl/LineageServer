package net.sf.l2j.gameserver.instancemanager;

import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Couple;
import net.sf.l2j.gameserver.model.world.L2World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CoupleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoupleManager.class);

    protected CoupleManager() {
        load();
    }

    public static final CoupleManager getInstance() {
        return SingletonHolder._instance;
    }

    private List<Couple> _couples;

    public void reload() {
        _couples.clear();
        load();
    }

    private void load() {
        _couples = new ArrayList<>();

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT id FROM mods_wedding ORDER BY id");
            ResultSet rs = statement.executeQuery();

            while (rs.next()) { _couples.add(new Couple(rs.getInt("id"))); }

            rs.close();
            statement.close();

            LOGGER.info("CoupleManager : Loaded {} couples.", _couples.size());
        }
        catch (Exception e) {
            LOGGER.error("Exception: CoupleManager.load(): {}", e.getMessage(), e);
        }
    }

    public final Couple getCouple(int coupleId) {
        int index = getCoupleIndex(coupleId);
        if (index >= 0) { return _couples.get(index); }

        return null;
    }

    public void createCouple(L2PcInstance player1, L2PcInstance player2) {
        if (player1 != null && player2 != null) {
            Couple _new = new Couple(player1, player2);
            _couples.add(_new);
            player1.setCoupleId(_new.getId());
            player2.setCoupleId(_new.getId());
        }
    }

    public void deleteCouple(int coupleId) {
        int index = getCoupleIndex(coupleId);
        Couple couple = _couples.get(index);
        if (couple != null) {
            L2PcInstance player1 = L2World.getInstance().getPlayer(couple.getPlayer1Id());
            L2PcInstance player2 = L2World.getInstance().getPlayer(couple.getPlayer2Id());

            if (player1 != null) {
                player1.setMarried(false);
                player1.setCoupleId(0);
            }

            if (player2 != null) {
                player2.setMarried(false);
                player2.setCoupleId(0);
            }
            couple.divorce();
            _couples.remove(index);
        }
    }

    public final int getCoupleIndex(int coupleId) {
        int i = 0;
        for (Couple temp : _couples) {
            if (temp != null && temp.getId() == coupleId) { return i; }

            i++;
        }
        return -1;
    }

    public final List<Couple> getCouples() {
        return _couples;
    }

    private static class SingletonHolder {
        protected static final CoupleManager _instance = new CoupleManager();
    }
}