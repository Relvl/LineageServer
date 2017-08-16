package net.sf.l2j.gameserver.scripting.scripts.teleports;

import net.sf.l2j.gameserver.instancemanager.SevenSigns;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.scripting.Quest;

public class GatekeeperSpirit extends Quest {
    private static final int ENTER_GK = 31111;
    private static final int EXIT_GK = 31112;
    private static final int LILITH = 25283;
    private static final int ANAKIM = 25286;

    public GatekeeperSpirit() {
        super(-1, "teleports");
        addStartNpc(ENTER_GK);
        addFirstTalkId(ENTER_GK);
        addTalkId(ENTER_GK);
        addKillId(LILITH, ANAKIM);
    }

    @Override
    public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
        switch (event) {
            case "spawn_exitgk_lilith":
                // exit_necropolis_boss_lilith
                addSpawn(EXIT_GK, 184446, -10112, -5488, 0, false, 900000, false);
                break;
            case "spawn_exitgk_anakim":
                // exit_necropolis_boss_anakim
                addSpawn(EXIT_GK, 184466, -13106, -5488, 0, false, 900000, false);
                break;
        }
        return null;
    }

    @Override
    public String onFirstTalk(L2Npc npc, L2PcInstance player) {
        int playerCabal = SevenSigns.getInstance().getPlayerCabal(player.getObjectId());
        int sealAvariceOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_AVARICE);
        int compWinner = SevenSigns.getInstance().getCabalHighestScore();
        if (playerCabal == sealAvariceOwner && playerCabal == compWinner) {
            switch (sealAvariceOwner) {
                case SevenSigns.CABAL_DAWN:
                    return "dawn.htm";
                case SevenSigns.CABAL_DUSK:
                    return "dusk.htm";
            }
        }
        npc.showChatWindow(player);
        return null;
    }

    @Override
    public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet) {
        // Запускаем таймер, который через 10 сек после убийства босса заспавнит ГК.
        switch (npc.getNpcId()) {
            case LILITH:
                startQuestTimer("spawn_exitgk_lilith", 10000, null, null, false);
                break;
            case ANAKIM:
                startQuestTimer("spawn_exitgk_anakim", 10000, null, null, false);
                break;
        }
        return super.onKill(npc, killer, isPet);
    }
}