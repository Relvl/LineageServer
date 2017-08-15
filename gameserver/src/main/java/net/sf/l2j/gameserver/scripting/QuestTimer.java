package net.sf.l2j.gameserver.scripting;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.util.threading.ThreadPoolManager;

import java.util.concurrent.ScheduledFuture;

@SuppressWarnings("FieldNotUsedInToString")
public final class QuestTimer implements Runnable {
    private final Quest quest;
    private final String event;
    private final L2Npc npc;
    private final L2PcInstance player;
    private final boolean repeating;

    private ScheduledFuture<?> scheduler;

    public QuestTimer(Quest quest, String event, L2Npc npc, L2PcInstance player, long time, boolean repeating) {
        this.quest = quest;
        this.event = event;
        this.npc = npc;
        this.player = player;
        this.repeating = repeating;
        this.scheduler = repeating ?
                ThreadPoolManager.getInstance().weakScheduleAtFixedRate(this, time, time) :
                ThreadPoolManager.getInstance().weakSchedule(this, time);
    }

    @Override
    public String toString() { return event; }

    @Override
    public void run() {
        if (scheduler == null) { return; }
        if (!repeating) { cancel(); }
        quest.notifyEvent(event, npc, player);
    }

    public void cancel() {
        if (scheduler != null) {
            scheduler.cancel(false);
            scheduler = null;
        }
        quest.removeQuestTimer(this);
    }

    @SuppressWarnings({"ObjectEquality", "SimplifiableIfStatement"})
    public boolean equals(Quest quest, String event, L2Npc npc, L2PcInstance player) {
        if (quest == null || quest != this.quest) { return false; }
        if (event == null || !event.equals(this.event)) { return false; }
        return (npc == this.npc) && (player == this.player);
    }
}