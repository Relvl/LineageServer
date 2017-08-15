package net.sf.l2j.gameserver.listenerbus.interfaces;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Johnson / 14.08.2017
 */
@FunctionalInterface
public interface OnPlayerExitListener extends IListener {
    void onPlayerExit(L2PcInstance player);
}
