package net.sf.l2j.gameserver.util.threading;

import net.sf.l2j.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

@SuppressWarnings("ClassHasNoToStringMethod")
public class ThreadPoolManager implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPoolManager.class);

    protected ScheduledThreadPoolExecutor generalScheduledThreadPool;
    protected ScheduledThreadPoolExecutor effectsScheduledThreadPool;
    protected ScheduledThreadPoolExecutor aiScheduledThreadPool;
    private final ThreadPoolExecutor generalThreadPool;
    private final ThreadPoolExecutor generalPacketsThreadPool;
    private final ThreadPoolExecutor ioPacketsThreadPool;

    private static final long MAX_DELAY = Long.MAX_VALUE / 1000000 / 2;

    private boolean shutdown;

    protected ThreadPoolManager() {
        effectsScheduledThreadPool = new ScheduledThreadPoolExecutor(Config.THREAD_P_EFFECTS, new PriorityThreadFactory("EffectsSTPool", Thread.NORM_PRIORITY));
        generalScheduledThreadPool = new ScheduledThreadPoolExecutor(Config.THREAD_P_GENERAL, new PriorityThreadFactory("GeneralSTPool", Thread.NORM_PRIORITY));
        ioPacketsThreadPool = new ThreadPoolExecutor(Config.IO_PACKET_THREAD_CORE_SIZE, Integer.MAX_VALUE, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new PriorityThreadFactory("I/O Packet Pool", Thread.NORM_PRIORITY + 1));
        generalPacketsThreadPool = new ThreadPoolExecutor(Config.GENERAL_PACKET_THREAD_CORE_SIZE, Config.GENERAL_PACKET_THREAD_CORE_SIZE + 2, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new PriorityThreadFactory("Normal Packet Pool", Thread.NORM_PRIORITY + 1));
        generalThreadPool = new ThreadPoolExecutor(Config.GENERAL_THREAD_CORE_SIZE, Config.GENERAL_THREAD_CORE_SIZE + 2, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new PriorityThreadFactory("General Pool", Thread.NORM_PRIORITY));
        aiScheduledThreadPool = new ScheduledThreadPoolExecutor(Config.AI_MAX_THREAD, new PriorityThreadFactory("AISTPool", Thread.NORM_PRIORITY));

        scheduleAtFixedRate(this, 10 * 60 * 1000L, 5 * 60 * 1000L);
    }

    public static long validateDelay(long delay) {
        if (delay < 0) { delay = 0; }
        else if (delay > MAX_DELAY) { delay = MAX_DELAY; }
        return delay;
    }

    public ScheduledFuture<?> scheduleEffect(Runnable r, long delay) {
        try {
            return effectsScheduledThreadPool.schedule(new RunnableWrapper(r), validateDelay(delay), TimeUnit.MILLISECONDS);
        }
        catch (RejectedExecutionException ignored) {
            return null; /* shutdown, ignore */
        }
    }

    public ScheduledFuture<?> scheduleEffectAtFixedRate(Runnable r, long initial, long delay) {
        try {
            return effectsScheduledThreadPool.scheduleAtFixedRate(new RunnableWrapper(r), validateDelay(initial), validateDelay(delay), TimeUnit.MILLISECONDS);
        }
        catch (RejectedExecutionException ignored) {
            return null; /* shutdown, ignore */
        }
    }

    public ScheduledFuture<?> scheduleAi(Runnable r, long delay) {
        try {
            return aiScheduledThreadPool.schedule(new RunnableWrapper(r), validateDelay(delay), TimeUnit.MILLISECONDS);
        }
        catch (RejectedExecutionException ignored) {
            return null; /* shutdown, ignore */
        }
    }

    public ScheduledFuture<?> scheduleAiAtFixedRate(Runnable r, long initial, long delay) {
        try {
            return aiScheduledThreadPool.scheduleAtFixedRate(new RunnableWrapper(r), validateDelay(initial), validateDelay(delay), TimeUnit.MILLISECONDS);
        }
        catch (RejectedExecutionException ignored) {
            return null; /* shutdown, ignore */
        }
    }

    /**
     * Планирует исполнение через указанное время, используя обычную (жесткую) ссылку.
     * Удерживает ссылку на раннейбл до момента его исполнения, тем самым препятствуя сборке этого раннейбла мусорщиком.
     * Если нет уверенности, какой именно метод использовать - следует использовать именно этот метод.
     */
    public ScheduledFuture<?> schedule(Runnable r, long delay) {
        try {
            return generalScheduledThreadPool.schedule(new RunnableWrapper(r), validateDelay(delay), TimeUnit.MILLISECONDS);
        }
        catch (RejectedExecutionException ignored) {
            return null; /* shutdown, ignore */
        }
    }

    /**
     * Планирует периодическое исполнение через указанное время, используя обычную (жесткую) ссылку.
     * Удерживает ссылку на раннейбл до момента остановки пула, тем самым препятствуя сборке этого раннейбла мусорщиком.
     * Если нет уверенности, какой именно метод использовать - следует использовать именно этот метод.
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable r, long initial, long delay) {
        try {
            return generalScheduledThreadPool.scheduleAtFixedRate(new RunnableWrapper(r), validateDelay(initial), validateDelay(delay), TimeUnit.MILLISECONDS);
        }
        catch (RejectedExecutionException ignored) {
            return null; /* shutdown, ignore */
        }
    }

    /**
     * Планирует исполнение через указанное время, используя слабую ссылку. НЕ ГАРАНТИРУЕТ, ЧТО РАННЕЙБЛ ДОЖИВЕТ ДО МОМЕНТА ИСПОЛНЕНИЯ!
     * Не удерживает жесткую ссылку на объект, соответственно не препятствует сборке объекта мусорщиком (если нет других жестких ссылок).
     * Если объект (собственно, раннейбл) за это время умрёт - то просто ни чего не произойдёт и пул вернет поток в доступные.
     * <p>
     * По-хорошему, если на штатный объект навешивается интерфейс Runnable, и нет необходимости держать объект в памяти до момента исполнения -
     * то лучше всего использовать именно этот метод.
     * Следует с осторожностью применять его в связке с лямбдами или анонимными классами - скорее всего они просто умрут, не дождавшись исполнения.
     */
    public ScheduledFuture<?> weakSchedule(Runnable r, long delay) {
        try {
            return generalScheduledThreadPool.schedule(new RunnableWeakWrapper(r), validateDelay(delay), TimeUnit.MILLISECONDS);
        }
        catch (RejectedExecutionException ignored) {
            return null; /* shutdown, ignore */
        }
    }

    /**
     * Планирует периодическое исполнение через указанное время, используя слабую ссылку. НЕ ГАРАНТИРУЕТ, ЧТО РАННЕЙБЛ ДОЖИВЕТ ДО МОМЕНТА ИСПОЛНЕНИЯ!
     * Не удерживает жесткую ссылку на объект, соответственно не препятствует сборке объекта мусорщиком (если нет других жестких ссылок).
     * Если объект (собственно, раннейбл) за это время умрёт - то просто ни чего не произойдёт и пул вернет поток в доступные.
     * <p>
     * По-хорошему, если на штатный объект навешивается интерфейс Runnable, и нет необходимости держать объект в памяти до момента исполнения -
     * то лучше всего использовать именно этот метод.
     * Следует с осторожностью применять его в связке с лямбдами или анонимными классами - скорее всего они просто умрут, не дождавшись исполнения.
     */
    public ScheduledFuture<?> weakScheduleAtFixedRate(Runnable r, long initial, long delay) {
        try {
            return generalScheduledThreadPool.scheduleAtFixedRate(new RunnableWeakWrapper(r), validateDelay(initial), validateDelay(delay), TimeUnit.MILLISECONDS);
        }
        catch (RejectedExecutionException ignored) {
            return null; /* shutdown, ignore */
        }
    }

    public void executePacket(Runnable pkt) { generalPacketsThreadPool.execute(pkt); }

    public void executeIOPacket(Runnable pkt) { ioPacketsThreadPool.execute(pkt); }

    public void executeTask(Runnable r) { generalThreadPool.execute(r); }

    public void executeAi(Runnable r) { aiScheduledThreadPool.execute(new RunnableWrapper(r)); }

    public void shutdown() {
        shutdown = true;
        aiScheduledThreadPool.shutdown();
        effectsScheduledThreadPool.shutdown();
        generalScheduledThreadPool.shutdown();
        generalPacketsThreadPool.shutdown();
        ioPacketsThreadPool.shutdown();
        generalThreadPool.shutdown();
        LOGGER.info("All ThreadPools are now stopped.");
    }

    public boolean isShutdown() { return shutdown; }

    @Override
    public void run() {
        effectsScheduledThreadPool.purge();
        generalScheduledThreadPool.purge();
        aiScheduledThreadPool.purge();
    }

    public static ThreadPoolManager getInstance() { return SingletonHolder.INSTANCE; }

    private static final class SingletonHolder {
        private static final ThreadPoolManager INSTANCE = new ThreadPoolManager();
    }
}