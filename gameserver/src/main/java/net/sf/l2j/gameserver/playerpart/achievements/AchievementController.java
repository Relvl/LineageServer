package net.sf.l2j.gameserver.playerpart.achievements;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * @author Johnson / 23.07.2017
 */
public class AchievementController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AchievementController.class);

    private final L2PcInstance player;
    private final List<IAchieveElement> completed = new ArrayList<>();
    private final Map<IAchieveElement, Integer> partialCompleted = new ConcurrentHashMap<>();

    @SuppressWarnings("resource")
    private static final PlayerAchievementModifyCall storeCall = new PlayerAchievementModifyCall();
    private static final Map<String, AchievementStoreData> storeQueue = new ConcurrentHashMap<>();
    private static final ScheduledFuture<?> task = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new AchievementStoreTask(), 1000, 10000);
    private static volatile boolean stored = true;

    public AchievementController(L2PcInstance player) {
        this.player = player;
    }

    public void set(IAchieveElement achieve, int count) {
        if (completed.contains(achieve)) { return; }
        if (count >= achieve.getCount()) {
            complete(achieve);
        }
        else {
            partialCompleted.put(achieve, count);
            // todo store
        }
        stored = false;
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
            // todo store
        }
        stored = false;
    }

    public void complete(IAchieveElement achieve) {
        if (completed.contains(achieve)) { return; }
        partialCompleted.remove(achieve);
        completed.add(achieve);
        // todo store
        // TODO! notify!
    }

    public void clear(IAchieveElement achieve) {
        if (!completed.contains(achieve)) { return; }
        partialCompleted.remove(achieve);
        completed.remove(achieve);
        // todo store
    }

    public static void forceStore() {
        new AchievementStoreTask().run();
    }

    private static class AchievementStoreTask implements Runnable {
        @Override
        public void run() {

            stored = true;
        }
    }
}
