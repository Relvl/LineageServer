package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;

public final class RequestRecipeBookOpen extends L2GameClientPacket {
    private boolean isDwarvenCraft;

    @Override
    protected void readImpl() {
        isDwarvenCraft = readD() == 0;
    }

    @Override
    protected void runImpl() {
        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null) { return; }

        if (activeChar.isCastingNow() || activeChar.isAllSkillsDisabled()) {
            activeChar.sendPacket(SystemMessageId.NO_RECIPE_BOOK_WHILE_CASTING);
            return;
        }

        activeChar.getRecipeController().requestBookOpen(isDwarvenCraft);
    }
}