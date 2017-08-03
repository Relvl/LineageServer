package net.sf.l2j.gameserver.taskmanager;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.ECtrlEvent;
import net.sf.l2j.gameserver.ai.model.L2CharacterAI;
import net.sf.l2j.gameserver.model.actor.L2Character;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public final class MovementTaskManager extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(MovementTaskManager.class);

    // Update the position of all moving characters each MILLIS_PER_UPDATE.
    private static final int MILLIS_PER_UPDATE = 100;

    private final Map<Integer, L2Character> _characters = new ConcurrentHashMap<>();

    public static MovementTaskManager getInstance() {
        return SingletonHolder._instance;
    }

    protected MovementTaskManager() {
        super("MovementTaskManager");
        setDaemon(true);
        setPriority(MAX_PRIORITY);
        start();
    }

    /**
     * Add a {@link L2Character} to MovementTask in order to update its location every MILLIS_PER_UPDATE ms.
     *
     * @param cha The L2Character to add to movingObjects of GameTimeController
     */
    public void add(L2Character cha) {
        _characters.putIfAbsent(cha.getObjectId(), cha);
    }

    @Override
    public void run() {
        LOGGER.info("MovementTaskManager: Started.");

        long time = System.currentTimeMillis();

        while (true) {
            // set next check time
            time += MILLIS_PER_UPDATE;

            try {
                // For all moving characters.
                for (Iterator<Entry<Integer, L2Character>> iterator = _characters.entrySet().iterator(); iterator.hasNext(); ) {
                    // Get entry of current iteration.
                    Entry<Integer, L2Character> entry = iterator.next();

                    // Get character.
                    L2Character character = entry.getValue();

                    // Update character position, final position isn't reached yet.
                    if (!character.updatePosition()) { continue; }

                    // Destination reached, remove from map.
                    iterator.remove();

                    // Get character AI, if AI doesn't exist, skip.
                    L2CharacterAI ai = character.getAI();
                    if (ai == null) { continue; }

                    // Inform AI about arrival.
                    ThreadPoolManager.getInstance().executeAi(() -> {
                        try {
                            ai.notifyEvent(ECtrlEvent.EVT_ARRIVED);
                        }
                        catch (Throwable e) {
                            LOGGER.error("", e);
                        }
                    });
                }
            }
            catch (Throwable e) {
                LOGGER.error("", e);
            }

            // Sleep thread till next tick.
            long sleepTime = time - System.currentTimeMillis();
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                }
                catch (InterruptedException e) {
                    LOGGER.error("", e);
                }
            }
        }
    }

    private static class SingletonHolder {
        protected static final MovementTaskManager _instance = new MovementTaskManager();
    }
}