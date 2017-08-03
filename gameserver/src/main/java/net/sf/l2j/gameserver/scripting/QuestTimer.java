package net.sf.l2j.gameserver.scripting;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

import java.util.concurrent.ScheduledFuture;

public class QuestTimer {

    protected final Quest _quest;
    protected final String _name;
    protected final L2Npc _npc;
    protected final L2PcInstance _player;
    protected final boolean _isRepeating;

    protected ScheduledFuture<?> _schedular;

    public QuestTimer(Quest quest, String name, L2Npc npc, L2PcInstance player, long time, boolean repeating) {
        _quest = quest;
        _name = name;
        _npc = npc;
        _player = player;
        _isRepeating = repeating;

        if (repeating) { _schedular = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new ScheduleTimerTask(), time, time); }
        else { _schedular = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleTimerTask(), time); }
    }

    @Override
    public final String toString() {
        return _name;
    }

    protected final class ScheduleTimerTask implements Runnable {
        @Override
        public void run() {
            if (_schedular == null) { return; }

            if (!_isRepeating) { cancel(); }

            _quest.notifyEvent(_name, _npc, _player);
        }
    }

    public final void cancel() {
        if (_schedular != null) {
            _schedular.cancel(false);
            _schedular = null;
        }

        _quest.removeQuestTimer(this);
    }

    /**
     * public method to compare if this timer matches with the key attributes passed.
     *
     * @param quest  : Quest instance to which the timer is attached
     * @param name   : Name of the timer
     * @param npc    : Npc instance attached to the desired timer (null if no npc attached)
     * @param player : Player instance attached to the desired timer (null if no player attached)
     * @return boolean
     */
    public final boolean equals(Quest quest, String name, L2Npc npc, L2PcInstance player) {
        if (quest == null || quest != _quest) { return false; }

        if (name == null || !name.equals(_name)) { return false; }

        return ((npc == _npc) && (player == _player));
    }
}