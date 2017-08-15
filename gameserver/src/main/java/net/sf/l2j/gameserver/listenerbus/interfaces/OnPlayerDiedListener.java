package net.sf.l2j.gameserver.listenerbus.interfaces;

import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Johnson / 14.08.2017
 */
@FunctionalInterface
public interface OnPlayerDiedListener extends IListener {
    void onPlayerDied(L2PcInstance player, L2Character killer);
}
