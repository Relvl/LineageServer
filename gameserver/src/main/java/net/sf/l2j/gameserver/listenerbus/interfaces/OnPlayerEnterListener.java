package net.sf.l2j.gameserver.listenerbus.interfaces;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Johnson / 14.08.2017
 */
@FunctionalInterface
public interface OnPlayerEnterListener extends IListener {
    void onPlayerEnter(L2PcInstance player);
}
