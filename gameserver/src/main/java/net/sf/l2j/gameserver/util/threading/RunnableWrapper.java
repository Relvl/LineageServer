package net.sf.l2j.gameserver.util.threading;

import java.lang.Thread.UncaughtExceptionHandler;

/**
 * Обычная обертка раннейбла, которая препятствует пробрасыванию исключения до дна стэка.
 *
 * @author Johnson / 15.08.2017
 */
@SuppressWarnings("ClassHasNoToStringMethod")
final class RunnableWrapper implements Runnable {
    private final Runnable runnable;

    RunnableWrapper(Runnable r) {
        runnable = r;
    }

    @Override
    public void run() {
        try {
            runnable.run();
        }
        catch (Throwable e) {
            Thread t = Thread.currentThread();
            UncaughtExceptionHandler h = t.getUncaughtExceptionHandler();
            if (h != null) {
                h.uncaughtException(t, e);
            }
        }
    }
}
