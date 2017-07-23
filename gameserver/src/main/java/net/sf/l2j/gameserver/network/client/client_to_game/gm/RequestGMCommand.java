package net.sf.l2j.gameserver.network.client.client_to_game.gm;

import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.network.client.client_to_game.L2GameClientPacket;
import net.sf.l2j.gameserver.network.client.game_to_client.gm.*;

public final class RequestGMCommand extends L2GameClientPacket {
    private String targetName;
    private int command;

    @Override
    protected void readImpl() {
        targetName = readS();
        command = readD();
    }

    @Override
    protected void runImpl() {
        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null) { return; }

        // prevent non gm or low level GMs from viewing player stuff
        if (!activeChar.isGM() || !activeChar.getAccessLevel().allowAltG()) { return; }

        L2PcInstance target = L2World.getInstance().getPlayer(targetName);
        L2Clan clan = ClanTable.getInstance().getClanByName(targetName);

        if (target == null && (clan == null || command != 6)) { return; }

        switch (command) {
            case 1: // target status
                sendPacket(new GMViewCharacterInfo(target));
                sendPacket(new GMViewHennaInfo(target));
                break;

            case 2: // target clan
                if (target != null && target.getClan() != null) { sendPacket(new GMViewPledgeInfo(target.getClan(), target)); }
                break;

            case 3: // target skills
                sendPacket(new GMViewSkillInfo(target));
                break;

            case 4: // target quests
                sendPacket(new GMViewQuestList(target));
                break;

            case 5: // target inventory
                sendPacket(new GMViewItemList(target));
                sendPacket(new GMViewHennaInfo(target));
                break;

            case 6: // player or clan warehouse
                if (target != null) { sendPacket(new GMViewWarehouseWithdrawList(target)); }
                else { sendPacket(new GMViewWarehouseWithdrawList(clan)); }
                break;
        }
    }
}