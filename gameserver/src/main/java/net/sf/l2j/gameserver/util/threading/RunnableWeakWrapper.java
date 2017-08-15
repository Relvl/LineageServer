package net.sf.l2j.gameserver.util.threading;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.ref.WeakReference;

/**
 * Особая обертка раннейбла, которая держит слабую ссылку, и не будет исполнена, если ссылка уже померла.
 * TODO Надо бы сделать наследника ReferenceQueue и помониторить убийство ссылок...
 *
 * @author Johnson / 15.08.2017
 */
@SuppressWarnings("ClassHasNoToStringMethod")
final class RunnableWeakWrapper implements Runnable {
    private final WeakReference<Runnable> reference;

    RunnableWeakWrapper(Runnable runnable) {
        this.reference = new WeakReference<>(runnable);
    }

    @Override
    public void run() {
        try {
            Runnable runnable = reference.get();
            if (runnable != null) { runnable.run(); }
        }
        catch (Throwable e) {
            UncaughtExceptionHandler h = Thread.currentThread().getUncaughtExceptionHandler();
            if (h != null) {
                h.uncaughtException(Thread.currentThread(), e);
            }
        }
    }
}
