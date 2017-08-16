package net.sf.l2j.gameserver.playerpart.achievements;

import net.sf.l2j.commons.database.CallException;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.client.game_to_client.MagicSkillUse;
import net.sf.l2j.gameserver.playerpart.achievements.PlayerAchievementLoadCall.AchievementData;
import net.sf.l2j.gameserver.playerpart.achievements.impl.EAchieveCraft;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.gameserver.util.threading.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;

/**
 * @author Johnson / 23.07.2017
 */
@SuppressWarnings("ClassHasNoToStringMethod")
public final class AchievementController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AchievementController.class);
    /**  */
    private static final Map<String, IAchieveElement> ACHIEMENETS = new HashMap<>();
    /**  */
    @SuppressWarnings("resource")
    private static final PlayerAchievementModifyCall STORE_CALL = new PlayerAchievementModifyCall();
    /**  */
    private static final Queue<AchievementStoreData> STORE_QUEUE = new ConcurrentLinkedQueue<>();
    /**  */
    @SuppressWarnings("unused")
    private static final ScheduledFuture<?> STORE_TASK = ThreadPoolManager.getInstance().scheduleAtFixedRate(new AchievementStoreTask(), 1000, 5000);

    private final L2PcInstance player;
    private final Collection<IAchieveElement> completed = new ArrayList<>();
    private final Map<IAchieveElement, Integer> partialCompleted = new ConcurrentHashMap<>();

    static {
        registerAchievement(EAchieveCraft.values());
    }

    public AchievementController(L2PcInstance player) {
        this.player = player;
        reload();
    }

    private static void registerAchievement(EAchieveCraft... achievements) {
        for (EAchieveCraft achievement : achievements) {
            ACHIEMENETS.put(achievement.getId(), achievement);
        }
    }

    public void reload() {
        try (PlayerAchievementLoadCall call = new PlayerAchievementLoadCall(player.getObjectId())) {
            call.execute();
            for (AchievementData data : call.getCompleted()) {
                IAchieveElement achiemenet = ACHIEMENETS.get(data.getAchievementId());
                if (achiemenet != null) { completed.add(achiemenet); }
            }
            for (AchievementData data : call.getPartial()) {
                IAchieveElement achiemenet = ACHIEMENETS.get(data.getAchievementId());
                if (achiemenet != null) {partialCompleted.put(achiemenet, data.getCount()); }
            }
        }
        catch (CallException e) {
            LOGGER.error("Cannot load player achievements", e);
        }
    }

    public void set(IAchieveElement achieve, int count) {
        if (completed.contains(achieve)) { return; }
        if (count >= achieve.getCount()) {
            complete(achieve);
        }
        else {
            // Не сохраняем, если счётчик такой же.
            if (partialCompleted.containsKey(achieve) && partialCompleted.get(achieve).equals(count)) { return; }
            partialCompleted.put(achieve, count);
            STORE_QUEUE.add(new AchievementStoreData(player.getObjectId(), achieve.getId(), count, false));
        }
    }

    public void increase(IAchieveElement achieve, int count) {
        if (completed.contains(achieve)) { return; }
        Integer tempCount = partialCompleted.get(achieve);
        if (tempCount == null) { tempCount = 0; }
        tempCount += count;

        if (tempCount >= achieve.getCount()) {
            complete(achieve);
        }
        else {
            partialCompleted.put(achieve, tempCount);
            STORE_QUEUE.add(new AchievementStoreData(player.getObjectId(), achieve.getId(), tempCount, false));
        }
    }

    public void complete(IAchieveElement achieve) {
        if (completed.contains(achieve)) { return; }
        partialCompleted.remove(achieve);
        completed.add(achieve);
        STORE_QUEUE.add(new AchievementStoreData(player.getObjectId(), achieve.getId(), 0, true));
        Broadcast.announceToOnlinePlayers(String.format("Игрок %s заработал достижение \"%s\"!", player.getName(), achieve.title()), true);
        player.broadcastPacket(new MagicSkillUse(player, player, 2024, 1, 0, 0));
    }

    public void clear(IAchieveElement achieve) {
        if (!completed.contains(achieve)) { return; }
        partialCompleted.remove(achieve);
        completed.remove(achieve);
        STORE_QUEUE.add(new AchievementStoreData(player.getObjectId(), achieve.getId(), 0, false));
    }

    public boolean hasAchievement(IAchieveElement achieve) {
        return completed.contains(achieve);
    }

    public Integer getAchievementPartialCount(IAchieveElement achieve) {
        if (partialCompleted.containsKey(achieve)) {
            return partialCompleted.get(achieve);
        }
        return 0;
    }

    public static void forceStore() {
        new AchievementStoreTask().run();
    }

    private static class AchievementStoreTask implements Runnable {
        @Override
        public void run() {
            if (STORE_QUEUE.isEmpty()) { return; }
            STORE_CALL.clear();
            AchievementStoreData data;
            while ((data = STORE_QUEUE.poll()) != null) {
                STORE_CALL.add(data);
            }
            try {
                STORE_CALL.execute();
            }
            catch (CallException e) {
                LOGGER.error("Cannot store achievements changed data!", e);
            }
        }
    }
}
