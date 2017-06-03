package net.sf.l2j.gameserver;

import net.sf.l2j.Config;
import net.sf.l2j.commons.lang.StringUtil;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class ThreadPoolManager {
    protected static final Logger _log = Logger.getLogger(ThreadPoolManager.class.getName());

    private static final class RunnableWrapper implements Runnable {
        private final Runnable _r;

        public RunnableWrapper(final Runnable r) {
            _r = r;
        }

        @Override
        public final void run() {
            try {
                _r.run();
            } catch (final Throwable e) {
                final Thread t = Thread.currentThread();
                final UncaughtExceptionHandler h = t.getUncaughtExceptionHandler();
                if (h != null) { h.uncaughtException(t, e); }
            }
        }
    }

    protected ScheduledThreadPoolExecutor _effectsScheduledThreadPool;
    protected ScheduledThreadPoolExecutor _generalScheduledThreadPool;
    protected ScheduledThreadPoolExecutor _aiScheduledThreadPool;
    private final ThreadPoolExecutor _generalPacketsThreadPool;
    private final ThreadPoolExecutor _ioPacketsThreadPool;
    private final ThreadPoolExecutor _generalThreadPool;

    /** temp workaround for VM issue */
    private static final long MAX_DELAY = Long.MAX_VALUE / 1000000 / 2;

    private boolean _shutdown;

    public static ThreadPoolManager getInstance() {
        return SingletonHolder._instance;
    }

    protected ThreadPoolManager() {
        _effectsScheduledThreadPool = new ScheduledThreadPoolExecutor(Config.THREAD_P_EFFECTS, new PriorityThreadFactory("EffectsSTPool", Thread.NORM_PRIORITY));
        _generalScheduledThreadPool = new ScheduledThreadPoolExecutor(Config.THREAD_P_GENERAL, new PriorityThreadFactory("GeneralSTPool", Thread.NORM_PRIORITY));
        _ioPacketsThreadPool = new ThreadPoolExecutor(Config.IO_PACKET_THREAD_CORE_SIZE, Integer.MAX_VALUE, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory("I/O Packet Pool", Thread.NORM_PRIORITY + 1));
        _generalPacketsThreadPool = new ThreadPoolExecutor(Config.GENERAL_PACKET_THREAD_CORE_SIZE, Config.GENERAL_PACKET_THREAD_CORE_SIZE + 2, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory("Normal Packet Pool", Thread.NORM_PRIORITY + 1));
        _generalThreadPool = new ThreadPoolExecutor(Config.GENERAL_THREAD_CORE_SIZE, Config.GENERAL_THREAD_CORE_SIZE + 2, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory("General Pool", Thread.NORM_PRIORITY));
        _aiScheduledThreadPool = new ScheduledThreadPoolExecutor(Config.AI_MAX_THREAD, new PriorityThreadFactory("AISTPool", Thread.NORM_PRIORITY));

        scheduleGeneralAtFixedRate(new PurgeTask(), 10 * 60 * 1000l, 5 * 60 * 1000l);
    }

    public static long validateDelay(long delay) {
        if (delay < 0) { delay = 0; }
        else if (delay > MAX_DELAY) { delay = MAX_DELAY; }
        return delay;
    }

    public ScheduledFuture<?> scheduleEffect(Runnable r, long delay) {
        try {
            delay = ThreadPoolManager.validateDelay(delay);
            return _effectsScheduledThreadPool.schedule(new RunnableWrapper(r), delay, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            return null;
        }
    }

    public ScheduledFuture<?> scheduleEffectAtFixedRate(Runnable r, long initial, long delay) {
        try {
            delay = ThreadPoolManager.validateDelay(delay);
            initial = ThreadPoolManager.validateDelay(initial);
            return _effectsScheduledThreadPool.scheduleAtFixedRate(new RunnableWrapper(r), initial, delay, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            return null; /* shutdown, ignore */
        }
    }

    @Deprecated
    public boolean removeEffect(RunnableScheduledFuture<?> r) {
        return _effectsScheduledThreadPool.remove(r);
    }

    public ScheduledFuture<?> scheduleGeneral(Runnable r, long delay) {
        try {
            delay = ThreadPoolManager.validateDelay(delay);
            return _generalScheduledThreadPool.schedule(new RunnableWrapper(r), delay, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            return null; /* shutdown, ignore */
        }
    }

    public ScheduledFuture<?> scheduleGeneralAtFixedRate(Runnable r, long initial, long delay) {
        try {
            delay = ThreadPoolManager.validateDelay(delay);
            initial = ThreadPoolManager.validateDelay(initial);
            return _generalScheduledThreadPool.scheduleAtFixedRate(new RunnableWrapper(r), initial, delay, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            return null; /* shutdown, ignore */
        }
    }

    @Deprecated
    public boolean removeGeneral(RunnableScheduledFuture<?> r) {
        return _generalScheduledThreadPool.remove(r);
    }

    public ScheduledFuture<?> scheduleAi(Runnable r, long delay) {
        try {
            delay = ThreadPoolManager.validateDelay(delay);
            return _aiScheduledThreadPool.schedule(new RunnableWrapper(r), delay, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            return null; /* shutdown, ignore */
        }
    }

    public ScheduledFuture<?> scheduleAiAtFixedRate(Runnable r, long initial, long delay) {
        try {
            delay = ThreadPoolManager.validateDelay(delay);
            initial = ThreadPoolManager.validateDelay(initial);
            return _aiScheduledThreadPool.scheduleAtFixedRate(new RunnableWrapper(r), initial, delay, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            return null; /* shutdown, ignore */
        }
    }

    public void executePacket(Runnable pkt) {
        _generalPacketsThreadPool.execute(pkt);
    }

    public void executeCommunityPacket(Runnable r) {
        _generalPacketsThreadPool.execute(r);
    }

    public void executeIOPacket(Runnable pkt) {
        _ioPacketsThreadPool.execute(pkt);
    }

    public void executeTask(Runnable r) {
        _generalThreadPool.execute(r);
    }

    public void executeAi(Runnable r) {
        _aiScheduledThreadPool.execute(new RunnableWrapper(r));
    }

    private static class PriorityThreadFactory implements ThreadFactory {
        private final int _prio;
        private final String _name;
        private final AtomicInteger _threadNumber = new AtomicInteger(1);
        private final ThreadGroup _group;

        public PriorityThreadFactory(String name, int prio) {
            _prio = prio;
            _name = name;
            _group = new ThreadGroup(_name);
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(_group, r);
            t.setName(_name + "-" + _threadNumber.getAndIncrement());
            t.setPriority(_prio);
            return t;
        }

        public ThreadGroup getGroup() {
            return _group;
        }
    }

    public void shutdown() {
        _shutdown = true;

        _effectsScheduledThreadPool.shutdown();
        _generalScheduledThreadPool.shutdown();
        _generalPacketsThreadPool.shutdown();
        _ioPacketsThreadPool.shutdown();
        _generalThreadPool.shutdown();

        _log.info("All ThreadPools are now stopped.");
    }

    public boolean isShutdown() {
        return _shutdown;
    }

    public void purge() {
        _effectsScheduledThreadPool.purge();
        _generalScheduledThreadPool.purge();
        _aiScheduledThreadPool.purge();
        _ioPacketsThreadPool.purge();
        _generalPacketsThreadPool.purge();
        _generalThreadPool.purge();
    }

    public String getPacketStats() {
        final StringBuilder sb = new StringBuilder(1000);
        ThreadFactory tf = _generalPacketsThreadPool.getThreadFactory();
        if (tf instanceof PriorityThreadFactory) {
            PriorityThreadFactory ptf = (PriorityThreadFactory) tf;
            int count = ptf.getGroup().activeCount();
            Thread[] threads = new Thread[count + 2];
            ptf.getGroup().enumerate(threads);
            StringUtil.append(sb, "General Packet Thread Pool:\r\nTasks in the queue: ", _generalPacketsThreadPool.getQueue().size(), "\r\nShowing threads stack trace:\r\nThere should be ", count, " Threads\r\n");
            for (Thread t : threads) {
                if (t == null) { continue; }

                StringUtil.append(sb, t.getName(), "\r\n");
                for (StackTraceElement ste : t.getStackTrace()) {
                    StringUtil.append(sb, ste.toString(), "\r\n");
                }
            }
        }

        sb.append("Packet Tp stack traces printed.\r\n");

        return sb.toString();
    }

    public String getIOPacketStats() {
        final StringBuilder sb = new StringBuilder(1000);
        ThreadFactory tf = _ioPacketsThreadPool.getThreadFactory();

        if (tf instanceof PriorityThreadFactory) {
            PriorityThreadFactory ptf = (PriorityThreadFactory) tf;
            int count = ptf.getGroup().activeCount();
            Thread[] threads = new Thread[count + 2];
            ptf.getGroup().enumerate(threads);
            StringUtil.append(sb, "I/O Packet Thread Pool:\r\nTasks in the queue: ", _ioPacketsThreadPool.getQueue().size(), "\r\nShowing threads stack trace:\r\nThere should be ", count, " Threads\r\n");

            for (Thread t : threads) {
                if (t == null) { continue; }

                StringUtil.append(sb, t.getName(), "\r\n");

                for (StackTraceElement ste : t.getStackTrace()) {
                    StringUtil.append(sb, ste.toString(), "\r\n");
                }
            }
        }

        sb.append("Packet Tp stack traces printed.\r\n");

        return sb.toString();
    }

    public String getGeneralStats() {
        final StringBuilder sb = new StringBuilder(1000);
        ThreadFactory tf = _generalThreadPool.getThreadFactory();

        if (tf instanceof PriorityThreadFactory) {
            PriorityThreadFactory ptf = (PriorityThreadFactory) tf;
            int count = ptf.getGroup().activeCount();
            Thread[] threads = new Thread[count + 2];
            ptf.getGroup().enumerate(threads);
            StringUtil.append(sb, "General Thread Pool:\r\nTasks in the queue: ", _generalThreadPool.getQueue().size(), "\r\nShowing threads stack trace:\r\nThere should be ", count, " Threads\r\n");

            for (Thread t : threads) {
                if (t == null) { continue; }

                StringUtil.append(sb, t.getName(), "\r\n");

                for (StackTraceElement ste : t.getStackTrace()) {
                    StringUtil.append(sb, ste.toString(), "\r\n");
                }
            }
        }

        sb.append("Packet Tp stack traces printed.\r\n");

        return sb.toString();
    }

    protected class PurgeTask implements Runnable {
        @Override
        public void run() {
            _effectsScheduledThreadPool.purge();
            _generalScheduledThreadPool.purge();
            _aiScheduledThreadPool.purge();
        }
    }

    private static class SingletonHolder {
        protected static final ThreadPoolManager _instance = new ThreadPoolManager();
    }
}