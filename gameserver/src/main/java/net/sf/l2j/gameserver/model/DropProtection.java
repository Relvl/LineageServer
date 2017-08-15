package net.sf.l2j.gameserver.model;

import net.sf.l2j.gameserver.util.threading.ThreadPoolManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;

import java.util.concurrent.ScheduledFuture;

public class DropProtection implements Runnable {
    private volatile boolean _isProtected;
    private L2PcInstance _owner;
    private ScheduledFuture<?> _task;

    private static final long PROTECTED_MILLIS_TIME = 15000;

    @Override
    public synchronized void run() {
        _isProtected = false;
        _owner = null;
        _task = null;
    }

    public boolean isProtected() {
        return _isProtected;
    }

    public L2PcInstance getOwner() {
        return _owner;
    }

    public synchronized boolean tryPickUp(L2PcInstance actor) {
        if (!_isProtected) { return true; }

        if (_owner == actor) { return true; }

        return _owner.getParty() != null && _owner.getParty() == actor.getParty();

    }

    public boolean tryPickUp(L2PetInstance pet) {
        return tryPickUp(pet.getOwner());
    }

    public synchronized void unprotect() {
        if (_task != null) { _task.cancel(false); }

        _isProtected = false;
        _owner = null;
        _task = null;
    }

    public synchronized void protect(L2PcInstance player) {
        unprotect();

        _isProtected = true;

        if ((_owner = player) == null) { throw new NullPointerException("Trying to protect dropped item to null owner"); }

        _task = ThreadPoolManager.getInstance().schedule(this, PROTECTED_MILLIS_TIME);
    }
}
