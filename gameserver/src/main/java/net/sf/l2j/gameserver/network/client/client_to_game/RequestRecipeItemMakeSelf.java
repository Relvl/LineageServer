package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.playerpart.PrivateStoreType;
import net.sf.l2j.gameserver.util.FloodProtectors;
import net.sf.l2j.gameserver.util.FloodProtectors.Action;

public final class RequestRecipeItemMakeSelf extends L2GameClientPacket {
    private int recipeId;

    @Override
    protected void readImpl() {
        recipeId = readD();
    }

    @Override
    protected void runImpl() {
        if (!FloodProtectors.performAction(getClient(), Action.MANUFACTURE)) { return; }

        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null) { return; }
        if (activeChar.getPrivateStoreType() == PrivateStoreType.MANUFACTURE || activeChar.isInCraftMode()) { return; }
        activeChar.getRecipeController().doManufacture(recipeId, false, activeChar);
    }
}