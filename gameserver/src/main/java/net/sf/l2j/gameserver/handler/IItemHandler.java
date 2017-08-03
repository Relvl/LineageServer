package net.sf.l2j.gameserver.handler;

import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mother class of all itemHandlers.
 */
public interface IItemHandler {
    Logger LOGGER = LoggerFactory.getLogger(IItemHandler.class);

    /**
     * Launch task associated to the item.
     *
     * @param playable L2Playable designating the player
     * @param item     L2ItemInstance designating the item to use
     * @param forceUse ctrl hold on item use
     */
    void useItem(L2Playable playable, L2ItemInstance item, boolean forceUse);
}