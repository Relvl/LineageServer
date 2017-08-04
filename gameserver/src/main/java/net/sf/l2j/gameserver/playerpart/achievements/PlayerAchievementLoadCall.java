package net.sf.l2j.gameserver.playerpart.achievements;

import net.sf.l2j.commons.database.IndexedCall;
import net.sf.l2j.commons.database.annotation.OrmParamCursor;
import net.sf.l2j.commons.database.annotation.OrmParamIn;
import net.sf.l2j.commons.database.annotation.OrmParamOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Johnson / 23.07.2017
 */
public class PlayerAchievementLoadCall extends IndexedCall {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerAchievementLoadCall.class);

    @OrmParamIn(1)
    private final Integer playerId;

    @OrmParamOut(value = 2, cursorClass = AchievementData.class)
    private List<AchievementData> completed = new ArrayList<>();
    @OrmParamOut(value = 3, cursorClass = AchievementData.class)
    private List<AchievementData> partial = new ArrayList<>();
    @OrmParamOut(4)
    private Integer resultCode;

    protected PlayerAchievementLoadCall(Integer playerId) {
        super("player_achievement_load", 4, false);
        this.playerId = playerId;
    }

    public List<AchievementData> getCompleted() { return completed; }

    public List<AchievementData> getPartial() { return partial; }

    @Override
    public Logger getLogger() { return LOGGER; }

    @Override
    public Integer getResultCode() { return resultCode; }

    public static final class AchievementData {
        @OrmParamCursor("PLAYER_ID")
        private Integer playerId;
        @OrmParamCursor("ACHIEVEMENT_ID")
        private String achievementId;
        @OrmParamCursor("COUNT")
        private Integer count;

        public String getAchievementId() { return achievementId; }

        public Integer getCount() { return count; }
    }
}
