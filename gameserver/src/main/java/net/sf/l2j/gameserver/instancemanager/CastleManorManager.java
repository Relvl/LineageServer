package net.sf.l2j.gameserver.instancemanager;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Manor;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.itemcontainer.ClanWarehouse;
import net.sf.l2j.gameserver.model.itemcontainer.ItemContainer;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.network.SystemMessageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

public class CastleManorManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CastleManorManager.class);

    public static final int PERIOD_CURRENT = 0;
    public static final int PERIOD_NEXT = 1;
    protected static final long MAINTENANCE_PERIOD = Config.ALT_MANOR_MAINTENANCE_PERIOD; // 6 mins
    private static final String CASTLE_MANOR_LOAD_PROCURE = "SELECT * FROM castle_manor_procure WHERE castle_id=?";
    private static final String CASTLE_MANOR_LOAD_PRODUCTION = "SELECT * FROM castle_manor_production WHERE castle_id=?";

    private static final int NEXT_PERIOD_APPROVE = Config.ALT_MANOR_APPROVE_TIME; // 6:00
    private static final int NEXT_PERIOD_APPROVE_MIN = Config.ALT_MANOR_APPROVE_MIN;
    private static final int MANOR_REFRESH = Config.ALT_MANOR_REFRESH_TIME; // 20:00
    private static final int MANOR_REFRESH_MIN = Config.ALT_MANOR_REFRESH_MIN;
    protected ScheduledFuture<?> _scheduledManorRefresh;
    protected ScheduledFuture<?> _scheduledMaintenanceEnd;
    protected ScheduledFuture<?> _scheduledNextPeriodapprove;
    private Calendar _manorRefresh;
    private Calendar _periodApprove;
    private boolean _underMaintenance;

    protected CastleManorManager() {
        load(); // load data from database
        init(); // schedule all manor related events
        _underMaintenance = false;

        boolean isApproved;
        if (_periodApprove.getTimeInMillis() > _manorRefresh.getTimeInMillis())
        // Next approve period already scheduled
        { isApproved = _manorRefresh.getTimeInMillis() > Calendar.getInstance().getTimeInMillis(); }
        else { isApproved = _periodApprove.getTimeInMillis() < Calendar.getInstance().getTimeInMillis() && _manorRefresh.getTimeInMillis() > Calendar.getInstance().getTimeInMillis(); }

        for (Castle c : CastleManager.getInstance().getCastles()) { c.setNextPeriodApproved(isApproved); }
    }

    public static CastleManorManager getInstance() {
        return SingletonHolder._instance;
    }

    private static void load() {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statementProduction = con.prepareStatement(CASTLE_MANOR_LOAD_PRODUCTION);
            PreparedStatement statementProcure = con.prepareStatement(CASTLE_MANOR_LOAD_PROCURE);

            for (Castle castle : CastleManager.getInstance().getCastles()) {
                List<SeedProduction> production = new ArrayList<>();
                List<SeedProduction> productionNext = new ArrayList<>();
                List<CropProcure> procure = new ArrayList<>();
                List<CropProcure> procureNext = new ArrayList<>();

                // restore seed production info
                statementProduction.setInt(1, castle.getCastleId());
                ResultSet rs = statementProduction.executeQuery();
                statementProduction.clearParameters();
                while (rs.next()) {
                    int seedId = rs.getInt("seed_id");
                    int canProduce = rs.getInt("can_produce");
                    int startProduce = rs.getInt("start_produce");
                    int price = rs.getInt("seed_price");
                    int period = rs.getInt("period");
                    if (period == PERIOD_CURRENT) { production.add(new SeedProduction(seedId, canProduce, price, startProduce)); }
                    else { productionNext.add(new SeedProduction(seedId, canProduce, price, startProduce)); }
                }
                rs.close();

                castle.setSeedProduction(production, PERIOD_CURRENT);
                castle.setSeedProduction(productionNext, PERIOD_NEXT);

                // restore procure info
                statementProcure.setInt(1, castle.getCastleId());
                rs = statementProcure.executeQuery();
                statementProcure.clearParameters();
                while (rs.next()) {
                    int cropId = rs.getInt("crop_id");
                    int canBuy = rs.getInt("can_buy");
                    int startBuy = rs.getInt("start_buy");
                    int rewardType = rs.getInt("reward_type");
                    int price = rs.getInt("price");
                    int period = rs.getInt("period");
                    if (period == PERIOD_CURRENT) { procure.add(new CropProcure(cropId, canBuy, rewardType, startBuy, price)); }
                    else { procureNext.add(new CropProcure(cropId, canBuy, rewardType, startBuy, price)); }
                }
                rs.close();

                castle.setCropProcure(procure, PERIOD_CURRENT);
                castle.setCropProcure(procureNext, PERIOD_NEXT);

                if (!procure.isEmpty() || !procureNext.isEmpty() || !production.isEmpty() || !productionNext.isEmpty()) {
                    LOGGER.info("{} manor: data loaded.", castle.getName());
                }
            }
            statementProduction.close();
            statementProcure.close();
        }
        catch (Exception e) {
            LOGGER.error("Error restoring manor data: {}", e.getMessage(), e);
        }
    }

    private static List<SeedProduction> getNewSeedsList(int castleId) {
        List<SeedProduction> seeds = new ArrayList<>();
        List<Integer> seedsIds = L2Manor.getInstance().getSeedsForCastle(castleId);
        for (int sd : seedsIds) { seeds.add(new SeedProduction(sd)); }

        return seeds;
    }

    private static List<CropProcure> getNewCropsList(int castleId) {
        List<CropProcure> crops = new ArrayList<>();
        List<Integer> cropsIds = L2Manor.getInstance().getCropsForCastle(castleId);
        for (int cr : cropsIds) { crops.add(new CropProcure(cr)); }

        return crops;
    }

    private void init() {
        _manorRefresh = Calendar.getInstance();
        _manorRefresh.set(Calendar.HOUR_OF_DAY, MANOR_REFRESH);
        _manorRefresh.set(Calendar.MINUTE, MANOR_REFRESH_MIN);

        _periodApprove = Calendar.getInstance();
        _periodApprove.set(Calendar.HOUR_OF_DAY, NEXT_PERIOD_APPROVE);
        _periodApprove.set(Calendar.MINUTE, NEXT_PERIOD_APPROVE_MIN);

        updateManorRefresh();
        updatePeriodApprove();
    }

    public void updateManorRefresh() {
        LOGGER.info("CastleManorManager: Manor refresh updated.");

        _scheduledManorRefresh = ThreadPoolManager.getInstance().scheduleGeneral(() -> {
            setUnderMaintenance(true);
            LOGGER.info("CastleManorManager: Under maintenance mode started.");
            _scheduledMaintenanceEnd = ThreadPoolManager.getInstance().scheduleGeneral(() -> {
                LOGGER.info("CastleManorManager: Next period started.");
                setNextPeriod();
                try {
                    save();
                }
                catch (Exception e) {
                    LOGGER.error("CastleManorManager: Failed to save manor data: {}", e.getMessage(), e);
                }
                setUnderMaintenance(false);
            }, MAINTENANCE_PERIOD);
            updateManorRefresh();
        }, getMillisToManorRefresh());
    }

    public void updatePeriodApprove() {
        LOGGER.info("CastleManorManager: Manor period approve updated.");

        _scheduledNextPeriodapprove = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable() {
            @Override
            public void run() {
                approveNextPeriod();
                LOGGER.info("CastleManorManager: Next period approved.");
                updatePeriodApprove();
            }
        }, getMillisToNextPeriodApprove());
    }

    public long getMillisToManorRefresh() {
        // use safe interval 120s to prevent double run
        if (_manorRefresh.getTimeInMillis() - Calendar.getInstance().getTimeInMillis() < 120000) { setNewManorRefresh(); }

        return _manorRefresh.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
    }

    public void setNewManorRefresh() {
        _manorRefresh = Calendar.getInstance();
        _manorRefresh.set(Calendar.HOUR_OF_DAY, MANOR_REFRESH);
        _manorRefresh.set(Calendar.MINUTE, MANOR_REFRESH_MIN);
        _manorRefresh.set(Calendar.SECOND, 0);
        _manorRefresh.add(Calendar.HOUR_OF_DAY, 24);

        LOGGER.info("CastleManorManager: New refresh period @ {}", _manorRefresh.getTime());
    }

    public long getMillisToNextPeriodApprove() {
        // use safe interval 120s to prevent double run
        if (_periodApprove.getTimeInMillis() - Calendar.getInstance().getTimeInMillis() < 120000) { setNewPeriodApprove(); }

        return _periodApprove.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
    }

    public void setNewPeriodApprove() {
        _periodApprove = Calendar.getInstance();
        _periodApprove.set(Calendar.HOUR_OF_DAY, NEXT_PERIOD_APPROVE);
        _periodApprove.set(Calendar.MINUTE, NEXT_PERIOD_APPROVE_MIN);
        _periodApprove.set(Calendar.SECOND, 0);
        _periodApprove.add(Calendar.HOUR_OF_DAY, 24);

        LOGGER.info("CastleManorManager: New approve period @ {}", _periodApprove.getTime());
    }

    public void setNextPeriod() {
        for (Castle c : CastleManager.getInstance().getCastles()) {
            if (c.getOwnerId() <= 0) { continue; }
            L2Clan clan = ClanTable.getInstance().getClan(c.getOwnerId());
            if (clan == null) { continue; }

            ItemContainer cwh = clan.getWarehouse();
            if (!(cwh instanceof ClanWarehouse)) {
                LOGGER.info("Can't get clan warehouse for clan {}", ClanTable.getInstance().getClan(c.getOwnerId()));
                continue;
            }

            for (CropProcure crop : c.getCropProcure(PERIOD_CURRENT)) {
                if (crop.getStartAmount() == 0) { continue; }

                // adding bought crops to clan warehouse
                if (crop.getStartAmount() - crop.getAmount() > 0) {
                    int count = crop.getStartAmount() - crop.getAmount();
                    count = count * 90 / 100;
                    if (count < 1) {
                        if (Rnd.get(99) < 90) { count = 1; }
                    }
                    if (count > 0) { cwh.addItem(EItemProcessPurpose.MANOR, L2Manor.getInstance().getMatureCrop(crop.getId()), count, null, null); }
                }
                // reserved and not used money giving back to treasury
                if (crop.getAmount() > 0) { c.addToTreasuryNoTax(crop.getAmount() * crop.getPrice()); }
            }

            c.setSeedProduction(c.getSeedProduction(PERIOD_NEXT), PERIOD_CURRENT);
            c.setCropProcure(c.getCropProcure(PERIOD_NEXT), PERIOD_CURRENT);

            if (c.getTreasury() < c.getManorCost(PERIOD_CURRENT)) {
                c.setSeedProduction(getNewSeedsList(c.getCastleId()), PERIOD_NEXT);
                c.setCropProcure(getNewCropsList(c.getCastleId()), PERIOD_NEXT);
            }
            else {
                List<SeedProduction> production = new ArrayList<>();
                for (SeedProduction s : c.getSeedProduction(PERIOD_CURRENT)) {
                    s.setCanProduce(s.getStartProduce());
                    production.add(s);
                }
                c.setSeedProduction(production, PERIOD_NEXT);

                List<CropProcure> procure = new ArrayList<>();
                for (CropProcure cr : c.getCropProcure(PERIOD_CURRENT)) {
                    cr.setAmount(cr.getStartAmount());
                    procure.add(cr);
                }
                c.setCropProcure(procure, PERIOD_NEXT);
            }

            if (Config.ALT_MANOR_SAVE_ALL_ACTIONS) {
                c.saveCropData();
                c.saveSeedData();
            }

            // Sending notification to a clan leader
            L2PcInstance clanLeader = null;
            clanLeader = L2World.getInstance().getPlayer(clan.getLeader().getName());
            if (clanLeader != null) { clanLeader.sendPacket(SystemMessageId.THE_MANOR_INFORMATION_HAS_BEEN_UPDATED); }

            c.setNextPeriodApproved(false);
        }
    }

    public void approveNextPeriod() {
        for (Castle c : CastleManager.getInstance().getCastles()) {
            boolean notFunc = false;

            // Castle has no owner
            if (c.getOwnerId() <= 0) {
                c.setCropProcure(new ArrayList<CropProcure>(), PERIOD_NEXT);
                c.setSeedProduction(new ArrayList<SeedProduction>(), PERIOD_NEXT);
            }
            else if (c.getTreasury() < c.getManorCost(PERIOD_NEXT)) {
                notFunc = true;
                LOGGER.info("{} castle manor disabled, not enough adena in treasury: {}, {} required.", c.getName(), c.getTreasury(), c.getManorCost(PERIOD_NEXT));

                c.setSeedProduction(getNewSeedsList(c.getCastleId()), PERIOD_NEXT);
                c.setCropProcure(getNewCropsList(c.getCastleId()), PERIOD_NEXT);
            }
            else {
                ItemContainer cwh = ClanTable.getInstance().getClan(c.getOwnerId()).getWarehouse();
                if (!(cwh instanceof ClanWarehouse)) {
                    LOGGER.info("Can't get clan warehouse for clan {}", ClanTable.getInstance().getClan(c.getOwnerId()));
                    continue;
                }

                int slots = 0;
                for (CropProcure crop : c.getCropProcure(PERIOD_NEXT)) {
                    if (crop.getStartAmount() > 0) {
                        if (cwh.getItemByItemId(L2Manor.getInstance().getMatureCrop(crop.getId())) == null) { slots++; }
                    }
                }

                if (!cwh.validateCapacity(slots)) {
                    notFunc = true;
                    LOGGER.info("{} castle manor disabled, not enough free slots in CWH: {}, {} required.", c.getName(), Config.WAREHOUSE_SLOTS_CLAN - cwh.getSize(), slots);

                    c.setSeedProduction(getNewSeedsList(c.getCastleId()), PERIOD_NEXT);
                    c.setCropProcure(getNewCropsList(c.getCastleId()), PERIOD_NEXT);
                }
            }
            c.setNextPeriodApproved(true);
            c.addToTreasuryNoTax(-1 * c.getManorCost(PERIOD_NEXT));

            if (notFunc) {
                L2Clan clan = ClanTable.getInstance().getClan(c.getOwnerId());
                if (clan != null) {
                    L2PcInstance clanLeader = clan.getLeader().getPlayerInstance();
                    if (clanLeader != null) { clanLeader.sendPacket(SystemMessageId.THE_AMOUNT_IS_NOT_SUFFICIENT_AND_SO_THE_MANOR_IS_NOT_IN_OPERATION); }
                }
            }
        }
    }

    public boolean isUnderMaintenance() {
        return _underMaintenance;
    }

    public void setUnderMaintenance(boolean mode) {
        _underMaintenance = mode;
    }

    public SeedProduction getNewSeedProduction(int id, int amount, int price, int sales) {
        return new SeedProduction(id, amount, price, sales);
    }

    public CropProcure getNewCropProcure(int id, int amount, int type, int price, int buy) {
        return new CropProcure(id, amount, type, buy, price);
    }

    public void save() {
        for (Castle c : CastleManager.getInstance().getCastles()) {
            c.saveSeedData();
            c.saveCropData();
        }
    }

    public static class CropProcure {
        final int _cropId;
        final int _rewardType;
        final int _buy;
        final int _price;
        int _buyResidual;

        public CropProcure(int id) {
            _cropId = id;
            _buyResidual = 0;
            _rewardType = 0;
            _buy = 0;
            _price = 0;
        }

        public CropProcure(int id, int amount, int type, int buy, int price) {
            _cropId = id;
            _buyResidual = amount;
            _rewardType = type;
            _buy = buy;
            _price = price;
        }

        public int getReward() {
            return _rewardType;
        }

        public int getId() {
            return _cropId;
        }

        public int getStartAmount() {
            return _buy;
        }

        public int getPrice() {
            return _price;
        }

        public int getAmount() {
            return _buyResidual;
        }

        public void setAmount(int amount) {
            _buyResidual = amount;
        }
    }

    public static class SeedProduction {
        final int _seedId;
        final int _price;
        final int _sales;
        int _residual;

        public SeedProduction(int id) {
            _seedId = id;
            _residual = 0;
            _price = 0;
            _sales = 0;
        }

        public SeedProduction(int id, int amount, int price, int sales) {
            _seedId = id;
            _residual = amount;
            _price = price;
            _sales = sales;
        }

        public int getId() {
            return _seedId;
        }

        public int getPrice() {
            return _price;
        }

        public int getStartProduce() {
            return _sales;
        }

        public int getCanProduce() {
            return _residual;
        }

        public void setCanProduce(int amount) {
            _residual = amount;
        }
    }

    private static class SingletonHolder {
        protected static final CastleManorManager _instance = new CastleManorManager();
    }
}