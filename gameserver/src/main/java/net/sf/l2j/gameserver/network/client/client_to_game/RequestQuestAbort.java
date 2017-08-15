package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.scripting.EScript;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public final class RequestQuestAbort extends L2GameClientPacket {
    private int questId;

    @Override
    protected void readImpl() {
        questId = readD();
    }

    @Override
    protected void runImpl() {
        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null) { return; }

        Quest qe = EScript.getQuest(questId);
        if (qe == null) { return; }

        QuestState qs = activeChar.getQuestState(qe.getName());
        if (qs != null) { qs.exitQuest(true); }
    }
}