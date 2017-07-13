package net.sf.l2j.gameserver.ai;

public class NextAction {
    /** After which ECtrlEvent is this action supposed to run. */
    private final ECtrlEvent nextEvent;
    /** What is the intention of the action, e.g. if AI gets this EIntention set, NextAction is canceled. */
    private final EIntention nextIntention;
    /** Wrapper for NextAction content. */
    private final Runnable nextTask;

    public NextAction(ECtrlEvent event, EIntention intention, Runnable runnable) {
        nextEvent = event;
        nextIntention = intention;
        nextTask = runnable;
    }

    public ECtrlEvent getEvent() {
        return nextEvent;
    }

    public EIntention getIntention() {
        return nextIntention;
    }

    public void run() {
        nextTask.run();
    }

    @Override
    public String toString() {
        return "NextAction{" +
                "nextEvent=" + nextEvent +
                ", nextIntention=" + nextIntention +
                ", nextTask=" + nextTask +
                '}';
    }
}