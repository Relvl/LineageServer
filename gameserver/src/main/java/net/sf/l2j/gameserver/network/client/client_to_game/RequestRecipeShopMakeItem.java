package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.playerpart.PrivateStoreType;
import net.sf.l2j.gameserver.util.FloodProtectors;
import net.sf.l2j.gameserver.util.FloodProtectors.Action;
import net.sf.l2j.gameserver.util.Util;

public final class RequestRecipeShopMakeItem extends L2GameClientPacket {
    private int manufacturerId;
    private int recipeId;
    private int unk1;

    @Override
    protected void readImpl() {
        manufacturerId = readD();
        recipeId = readD();
        unk1 = readD();
    }

    @Override
    protected void runImpl() {
        if (!FloodProtectors.performAction(getClient(), Action.MANUFACTURE)) { return; }

        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null) { return; }

        L2PcInstance manufacturer = L2World.getInstance().getPlayer(manufacturerId);
        if (manufacturer == null) { return; }

        if (activeChar.isInStoreMode()) { return; }

        if (manufacturer.getPrivateStoreType() != PrivateStoreType.MANUFACTURE) { return; }

        if (activeChar.isInCraftMode() || manufacturer.isInCraftMode()) { return; }

        if (manufacturer.isInDuel() || activeChar.isInDuel()) {
            activeChar.sendPacket(SystemMessageId.CANT_OPERATE_PRIVATE_STORE_DURING_COMBAT);
            return;
        }

        if (Util.checkIfInRange(150, activeChar, manufacturer, true)) {
            manufacturer.getRecipeController().doManufacture(recipeId, true, activeChar);
        }
    }
}