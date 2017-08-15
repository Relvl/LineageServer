package net.sf.l2j.commons.cron;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.time.ZonedDateTime;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CronSchedule implements Runnable {
    private static final int ONE_MINUTE_IN_MILLISECONDS = 60000;
    private static final int ONE_SECOND_IN_MILLISECONDS = 1000;

    private final ScheduledExecutorService executorService;
    private final Multimap<CronExpression, Runnable> runnables;
    private final int periodInMilliseconds;

    private ScheduledFuture<?> future;

    public CronSchedule(ScheduledExecutorService service) {
        this(service, false);
    }

    public CronSchedule(ScheduledExecutorService service, boolean seconds) {
        executorService = service;
        Multimap<CronExpression, Runnable> wrapped = HashMultimap.create();
        runnables = Multimaps.synchronizedMultimap(wrapped);
        periodInMilliseconds = seconds ? ONE_SECOND_IN_MILLISECONDS : ONE_MINUTE_IN_MILLISECONDS;
    }

    public void add(CronExpression expression, Runnable runnable) {
        runnables.put(expression, runnable);
    }

    public void remove(CronExpression expression) {
        runnables.removeAll(expression);
    }

    public void remove(CronExpression expression, Runnable runnable) {
        runnables.remove(expression, runnable);
    }

    public boolean isStarted() {
        return future != null && !future.isCancelled() && !future.isDone();
    }

    public synchronized void start() {
        if (!isStarted()) {
            long untilNextPeriod = System.currentTimeMillis() % periodInMilliseconds;
            future = executorService.scheduleAtFixedRate(this::run, untilNextPeriod, periodInMilliseconds, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void run() {
        run(ZonedDateTime.now());
    }

    public void run(ZonedDateTime time) {
        for (CronExpression expression : runnables.keySet()) {
            if (expression.matches(time)) {
                for (Runnable runnable : runnables.get(expression)) {
                    executorService.submit(runnable);
                }
            }
        }
    }

    public synchronized void stop() {
        if (future != null) {
            future.cancel(true);
            future = null;
        }
    }
}
