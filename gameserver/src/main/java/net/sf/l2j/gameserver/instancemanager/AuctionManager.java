package net.sf.l2j.gameserver.instancemanager;

import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.gameserver.model.entity.Auction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AuctionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionManager.class);
    private static final String[] ITEM_INIT_DATA = {
            "(22, 0, '', '', 22, 'Moonstone Hall', 20000000, 0, 1164841200000)",
            "(23, 0, '', '', 23, 'Onyx Hall', 20000000, 0, 1164841200000)",
            "(24, 0, '', '', 24, 'Topaz Hall', 20000000, 0, 1164841200000)",
            "(25, 0, '', '', 25, 'Ruby Hall', 20000000, 0, 1164841200000)",
            "(26, 0, '', '', 26, 'Crystal Hall', 20000000, 0, 1164841200000)",
            "(27, 0, '', '', 27, 'Onyx Hall', 20000000, 0, 1164841200000)",
            "(28, 0, '', '', 28, 'Sapphire Hall', 20000000, 0, 1164841200000)",
            "(29, 0, '', '', 29, 'Moonstone Hall', 20000000, 0, 1164841200000)",
            "(30, 0, '', '', 30, 'Emerald Hall', 20000000, 0, 1164841200000)",
            "(31, 0, '', '', 31, 'Atramental Barracks', 8000000, 0, 1164841200000)",
            "(32, 0, '', '', 32, 'Scarlet Barracks', 8000000, 0, 1164841200000)",
            "(33, 0, '', '', 33, 'Viridian Barracks', 8000000, 0, 1164841200000)",
            "(36, 0, '', '', 36, 'Golden Chamber', 50000000, 0, 1164841200000)",
            "(37, 0, '', '', 37, 'Silver Chamber', 50000000, 0, 1164841200000)",
            "(38, 0, '', '', 38, 'Mithril Chamber', 50000000, 0, 1164841200000)",
            "(39, 0, '', '', 39, 'Silver Manor', 50000000, 0, 1164841200000)",
            "(40, 0, '', '', 40, 'Gold Manor', 50000000, 0, 1164841200000)",
            "(41, 0, '', '', 41, 'Bronze Chamber', 50000000, 0, 1164841200000)",
            "(42, 0, '', '', 42, 'Golden Chamber', 50000000, 0, 1164841200000)",
            "(43, 0, '', '', 43, 'Silver Chamber', 50000000, 0, 1164841200000)",
            "(44, 0, '', '', 44, 'Mithril Chamber', 50000000, 0, 1164841200000)",
            "(45, 0, '', '', 45, 'Bronze Chamber', 50000000, 0, 1164841200000)",
            "(46, 0, '', '', 46, 'Silver Manor', 50000000, 0, 1164841200000)",
            "(47, 0, '', '', 47, 'Moonstone Hall', 50000000, 0, 1164841200000)",
            "(48, 0, '', '', 48, 'Onyx Hall', 50000000, 0, 1164841200000)",
            "(49, 0, '', '', 49, 'Emerald Hall', 50000000, 0, 1164841200000)",
            "(50, 0, '', '', 50, 'Sapphire Hall', 50000000, 0, 1164841200000)",
            "(51, 0, '', '', 51, 'Mont Chamber', 50000000, 0, 1164841200000)",
            "(52, 0, '', '', 52, 'Astaire Chamber', 50000000, 0, 1164841200000)",
            "(53, 0, '', '', 53, 'Aria Chamber', 50000000, 0, 1164841200000)",
            "(54, 0, '', '', 54, 'Yiana Chamber', 50000000, 0, 1164841200000)",
            "(55, 0, '', '', 55, 'Roien Chamber', 50000000, 0, 1164841200000)",
            "(56, 0, '', '', 56, 'Luna Chamber', 50000000, 0, 1164841200000)",
            "(57, 0, '', '', 57, 'Traban Chamber', 50000000, 0, 1164841200000)",
            "(58, 0, '', '', 58, 'Eisen Hall', 50000000, 0, 1164841200000)",
            "(59, 0, '', '', 59, 'Heavy Metal Hall', 50000000, 0, 1164841200000)",
            "(60, 0, '', '', 60, 'Molten Ore Hall', 50000000, 0, 1164841200000)",
            "(61, 0, '', '', 61, 'Titan Hall', 50000000, 0, 1164841200000)"
    };
    private static final int[] ItemInitDataId = { 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61 };
    private final List<Auction> _auctions;

    protected AuctionManager() {
        _auctions = new ArrayList<>();
        load();
    }

    public static final AuctionManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public void reload() {
        _auctions.clear();
        load();
    }

    private final void load() {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT id FROM auction ORDER BY id");
            ResultSet rs = statement.executeQuery();

            while (rs.next()) { _auctions.add(new Auction(rs.getInt("id"))); }

            rs.close();
            statement.close();

            LOGGER.info("AuctionManager: Loaded {} auction(s)", getAuctions().size());
        }
        catch (Exception e) {
            LOGGER.error("AuctionManager: an exception occured at auction.sql loading: {}", e.getMessage(), e);
        }
    }

    public final Auction getAuction(int auctionId) {
        int index = getAuctionIndex(auctionId);
        if (index >= 0) { return getAuctions().get(index); }

        return null;
    }

    public final int getAuctionIndex(int auctionId) {
        Auction auction;
        for (int i = 0; i < getAuctions().size(); i++) {
            auction = getAuctions().get(i);
            if (auction != null && auction.getId() == auctionId) { return i; }
        }
        return -1;
    }

    public final List<Auction> getAuctions() {
        return _auctions;
    }

    /**
     * Init Clan NPC aution
     *
     * @param id
     */
    public void initNPC(int id) {
        int i;
        for (i = 0; i < ItemInitDataId.length; i++) {
            if (ItemInitDataId[i] == id) { break; }
        }

        if (i >= ItemInitDataId.length || ItemInitDataId[i] != id) {
            LOGGER.warn("ClanHall auction not found for Id: {}", id);
            return;
        }

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("INSERT INTO `auction` VALUES " + ITEM_INIT_DATA[i]);
            statement.execute();
            statement.close();
            _auctions.add(new Auction(id));
        }
        catch (Exception e) {
            LOGGER.error("AuctionManager: an exception occured at initNPC loading: {}", e.getMessage(), e);
        }
    }

    private static final class SingletonHolder {
        private static final AuctionManager INSTANCE = new AuctionManager();
    }
}