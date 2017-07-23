package net.sf.l2j.gameserver.playerpart.achievements;

import net.sf.l2j.commons.database.IndexedCall;
import net.sf.l2j.commons.database.annotation.OrmParamIn;
import net.sf.l2j.commons.database.annotation.OrmParamOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Johnson / 23.07.2017
 */
public class PlayerAchievementModifyCall extends IndexedCall {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerAchievementModifyCall.class);

    @OrmParamIn(value = 1, arrayElementClass = AchievementStoreData.class)
    private final Collection<AchievementStoreData> data = new ArrayList<>();
    @OrmParamOut(2)
    private Integer resultCode;

    protected PlayerAchievementModifyCall() {
        super("game_server.player_achievement_modify", 2, false);
    }

    public void clear() { data.clear(); }

    public void add(AchievementStoreData storeData) { data.add(storeData); }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Integer getResultCode() {
        return resultCode;
    }
}
