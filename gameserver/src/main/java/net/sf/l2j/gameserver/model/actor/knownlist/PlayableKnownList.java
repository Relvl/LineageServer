package net.sf.l2j.gameserver.model.actor.knownlist;

import net.sf.l2j.gameserver.model.actor.L2Playable;

public class PlayableKnownList extends CharKnownList {
    public PlayableKnownList(L2Playable activeChar) {
        super(activeChar);
    }
}