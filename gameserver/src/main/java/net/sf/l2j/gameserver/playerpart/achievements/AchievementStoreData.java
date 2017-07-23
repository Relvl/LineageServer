package net.sf.l2j.gameserver.playerpart.achievements;

import net.sf.l2j.commons.database.annotation.OrmTypeName;

/**
 * @author Johnson / 23.07.2017
 */
@OrmTypeName("game_server.achievement_modify_data")
public class AchievementStoreData {
    private final Integer playerId;
    private final String achievementId;
    private final Integer count;
    private final Boolean complete;

    /** Конструктор для установки завершенного или удаления данных. */
    public AchievementStoreData(Integer playerId, String achievementId, Boolean complete) {
        this.playerId = playerId;
        this.achievementId = achievementId;
        this.complete = complete;
        this.count = 0;
    }

    public AchievementStoreData(Integer playerId, String achievementId, Integer count, Boolean complete) {
        this.playerId = playerId;
        this.achievementId = achievementId;
        this.count = count;
        this.complete = complete;
    }

    public Integer getPlayerId() { return playerId; }

    public String getAchievementId() { return achievementId; }

    public Integer getCount() { return count; }

    public Boolean isComplete() { return complete; }

    /** Стоит обратить внимание, что toString в UDT постгреса должен возвращать значения в правильном порядке внутри скобочек, через запятую. */
    @Override
    public String toString() { return "(" + playerId + "," + achievementId + "," + count + "," + complete + ')'; }
}
