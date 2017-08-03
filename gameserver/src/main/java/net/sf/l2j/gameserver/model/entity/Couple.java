package net.sf.l2j.gameserver.model.entity;

import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.instancemanager.CoupleManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Couple {
    private static final Logger LOGGER = LoggerFactory.getLogger(Couple.class);

    private int _Id;
    private int _player1Id;
    private int _player2Id;
    private static int _partnerId;
    private boolean _maried;

    public Couple(int coupleId) {
        _Id = coupleId;

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT * FROM mods_wedding WHERE id = ?");
            statement.setInt(1, _Id);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                _player1Id = rs.getInt("player1Id");
                _player2Id = rs.getInt("player2Id");
                _maried = rs.getBoolean("married");
            }
            rs.close();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Exception: Couple.load(): {}", e.getMessage(), e);
        }
    }

    public Couple(L2PcInstance player1, L2PcInstance player2) {
        int _tempPlayer1Id = player1.getObjectId();
        int _tempPlayer2Id = player2.getObjectId();

        _player1Id = _tempPlayer1Id;
        _player2Id = _tempPlayer2Id;

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            _Id = IdFactory.getInstance().getNextId();
            PreparedStatement statement = con.prepareStatement("INSERT INTO mods_wedding (id, player1Id, player2Id, married) VALUES (?,?,?,?)");
            statement.setInt(1, _Id);
            statement.setInt(2, _player1Id);
            statement.setInt(3, _player2Id);
            statement.setBoolean(4, false);
            statement.execute();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Exception: Couple creation: {}", e.getMessage(), e);
        }
    }

    public void marry() {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("UPDATE mods_wedding SET married = ? WHERE id = ?");
            statement.setBoolean(1, true);
            statement.setInt(2, _Id);
            statement.execute();
            statement.close();
            _maried = true;
        }
        catch (Exception e) {
            LOGGER.error("Exception: Couple.marry(): {}", e.getMessage(), e);
        }
    }

    public void divorce() {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("DELETE FROM mods_wedding WHERE id = ?");
            statement.setInt(1, _Id);
            statement.execute();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Exception: Couple.divorce(): {}", e.getMessage(), e);
        }
    }

    public final int getId() {
        return _Id;
    }

    public final int getPlayer1Id() {
        return _player1Id;
    }

    public final int getPlayer2Id() {
        return _player2Id;
    }

    public static final int getPartnerId(int playerId) {
        for (Couple cl : CoupleManager.getInstance().getCouples()) {
            if (cl.getPlayer1Id() == playerId || cl.getPlayer2Id() == playerId) {
                if (cl.getPlayer1Id() == playerId) { _partnerId = cl.getPlayer2Id(); }
                else { _partnerId = cl.getPlayer1Id(); }
            }
        }

        return _partnerId;
    }

    public final boolean getMaried() {
        return _maried;
    }
}