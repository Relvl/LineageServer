package net.sf.l2j.gameserver.model.actor.status;

import net.sf.l2j.gameserver.model.actor.L2Playable;

public class PlayableStatus extends CharStatus {
    public PlayableStatus(L2Playable activeChar) {
        super(activeChar);
    }

    @Override
    public L2Playable getActiveChar() {
        return (L2Playable) super.getActiveChar();
    }
}