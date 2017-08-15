package net.sf.l2j.commons.cron;

import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

/** Matches once only. */
final class RebootCronExpression extends CronExpression {
    private final AtomicBoolean matchOnce;

    protected RebootCronExpression() {
        matchOnce = new AtomicBoolean(true);
    }

    @Override
    public boolean matches(ZonedDateTime t) {
        return matchOnce.getAndSet(false);
    }
}
