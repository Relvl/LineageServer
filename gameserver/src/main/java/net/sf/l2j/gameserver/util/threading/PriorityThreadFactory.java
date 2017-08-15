package net.sf.l2j.gameserver.util.threading;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Johnson / 15.08.2017
 */
final class PriorityThreadFactory implements ThreadFactory {
    private final int prio;
    private final String name;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final ThreadGroup threadGroup;

    PriorityThreadFactory(String name, int prio) {
        this.prio = prio;
        this.name = name;
        this.threadGroup = new ThreadGroup(this.name);
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(threadGroup, r);
        t.setName(name + '-' + threadNumber.getAndIncrement());
        t.setPriority(prio);
        return t;
    }

    public ThreadGroup getGroup() {
        return threadGroup;
    }

    @Override
    public String toString() {
        return "PriorityThreadFactory{" +
                "prio=" + prio +
                ", name='" + name + '\'' +
                ", threadNumber=" + threadNumber +
                ", threadGroup=" + threadGroup +
                '}';
    }
}
