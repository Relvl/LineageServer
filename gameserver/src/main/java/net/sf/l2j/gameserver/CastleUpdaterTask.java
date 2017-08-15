package net.sf.l2j.gameserver;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.util.threading.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CastleUpdaterTask implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(CastleUpdaterTask.class);

    private final L2Clan _clan;
    private int _runCount = 0;

    public CastleUpdaterTask(L2Clan clan, int runCount) {
        _clan = clan;
        _runCount = runCount;
    }

    @Override
    public void run() {
        try {
            // Move current castle treasury to clan warehouse every 2 hour
            if (_clan.getWarehouse() != null && _clan.hasCastle()) {
                Castle castle = CastleManager.getInstance().getCastleById(_clan.getCastleId());
                if (!Config.ALT_MANOR_SAVE_ALL_ACTIONS && castle != null) {
                    if (_runCount % Config.ALT_MANOR_SAVE_PERIOD_RATE == 0) {
                        castle.saveSeedData();
                        castle.saveCropData();
                        LOGGER.info("Manor System: all data for {} saved", castle.getName());
                    }
                }
                _runCount++;
                ThreadPoolManager.getInstance().schedule(new CastleUpdaterTask(_clan, _runCount), 3600000);
            }
        }
        catch (Throwable e) {
            LOGGER.error("", e);
        }
    }
}