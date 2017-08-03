/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.scripting.quests;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.instancemanager.RaidBossSpawnManager;
import net.sf.l2j.gameserver.instancemanager.RaidBossSpawnManager.StatusEnum;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q604_DaimonTheWhiteEyed_Part2 extends Quest {
    private static final String qn = "Q604_DaimonTheWhiteEyed_Part2";

    // Monster
    private static final int DAIMON_THE_WHITE_EYED = 25290;

    // NPCs
    private static final int EYE_OF_ARGOS = 31683;
    private static final int DAIMON_ALTAR = 31541;

    // Items
    private static final int UNFINISHED_SUMMON_CRYSTAL = 7192;
    private static final int SUMMON_CRYSTAL = 7193;
    private static final int ESSENCE_OF_DAIMON = 7194;
    private static final int REWARD_DYE[] =
            {
                    4595,
                    4596,
                    4597,
                    4598,
                    4599,
                    4600
            };

    // Other
    private static final int CHECK_INTERVAL = 600000; // 10 minutes
    private static final int IDLE_INTERVAL = 3; // (X * CHECK_INTERVAL) = 30 minutes
    private static L2Npc _npc = null;
    private static int _status = -1;

    public Q604_DaimonTheWhiteEyed_Part2() {
        super(604, "Daimon The White-Eyed - Part 2");

        setItemsIds(SUMMON_CRYSTAL, ESSENCE_OF_DAIMON);

        addStartNpc(EYE_OF_ARGOS);
        addTalkId(EYE_OF_ARGOS, DAIMON_ALTAR);

        addAttackId(DAIMON_THE_WHITE_EYED);
        addKillId(DAIMON_THE_WHITE_EYED);

        switch (RaidBossSpawnManager.getInstance().getRaidBossStatusId(DAIMON_THE_WHITE_EYED)) {
            case UNDEFINED:
                LOGGER.warn(qn + ": can not find spawned L2RaidBoss id=" + DAIMON_THE_WHITE_EYED);
                break;

            case ALIVE:
                spawnNpc();
            case DEAD:
                startQuestTimer("check", CHECK_INTERVAL, null, null, true);
                break;
        }
    }

    @Override
    public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
        // global quest timer has player==null -> cannot get QuestState
        if (event.equals("check")) {
            L2RaidBossInstance raid = RaidBossSpawnManager.getInstance().getBosses().get(DAIMON_THE_WHITE_EYED);
            if (raid != null && raid.getRaidStatus() == StatusEnum.ALIVE) {
                if (_status >= 0 && _status-- == 0) { despawnRaid(raid); }

                spawnNpc();
            }

            return null;
        }

        String htmltext = event;
        QuestState st = player.getQuestState(qn);
        if (st == null) { return htmltext; }

        // Eye of Argos
        if (event.equalsIgnoreCase("31683-03.htm")) {
            if (st.hasQuestItems(UNFINISHED_SUMMON_CRYSTAL)) {
                st.setState(STATE_STARTED);
                st.set("cond", "1");
                st.playSound(QuestState.SOUND_ACCEPT);
                st.takeItems(UNFINISHED_SUMMON_CRYSTAL, 1);
                st.giveItems(SUMMON_CRYSTAL, 1);
            }
            else { htmltext = "31683-04.htm"; }
        }
        else if (event.equalsIgnoreCase("31683-08.htm")) {
            if (st.hasQuestItems(ESSENCE_OF_DAIMON)) {
                st.takeItems(ESSENCE_OF_DAIMON, 1);
                st.rewardItems(REWARD_DYE[Rnd.get(REWARD_DYE.length)], 5);
                st.playSound(QuestState.SOUND_FINISH);
                st.exitQuest(true);
            }
            else { htmltext = "31683-09.htm"; }
        }
        // Diamon's Altar
        else if (event.equalsIgnoreCase("31541-02.htm")) {
            if (st.hasQuestItems(SUMMON_CRYSTAL)) {
                if (_status < 0) {
                    if (spawnRaid()) {
                        st.set("cond", "2");
                        st.playSound(QuestState.SOUND_MIDDLE);
                        st.takeItems(SUMMON_CRYSTAL, 1);
                    }
                }
                else { htmltext = "31541-04.htm"; }
            }
            else { htmltext = "31541-03.htm"; }
        }

        return htmltext;
    }

    @Override
    public String onTalk(L2Npc npc, L2PcInstance player) {
        String htmltext = getNoQuestMsg();
        QuestState st = player.getQuestState(qn);
        if (st == null) { return htmltext; }

        switch (st.getState()) {
            case STATE_CREATED:
                if (player.getLevel() < 73) {
                    htmltext = "31683-02.htm";
                    st.exitQuest(true);
                }
                else { htmltext = "31683-01.htm"; }
                break;

            case STATE_STARTED:
                int cond = st.getInt("cond");
                switch (npc.getNpcId()) {
                    case EYE_OF_ARGOS:
                        if (cond == 1) { htmltext = "31683-05.htm"; }
                        else if (cond == 2) { htmltext = "31683-06.htm"; }
                        else { htmltext = "31683-07.htm"; }
                        break;

                    case DAIMON_ALTAR:
                        if (cond == 1) { htmltext = "31541-01.htm"; }
                        else if (cond == 2) { htmltext = "31541-05.htm"; }
                        break;
                }
                break;
        }

        return htmltext;
    }

    @Override
    public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet) {
        _status = IDLE_INTERVAL;
        return null;
    }

    @Override
    public String onKill(L2Npc npc, L2PcInstance player, boolean isPet) {
        for (L2PcInstance partyMember : getPartyMembers(player, npc, "cond", "2")) {
            QuestState st = partyMember.getQuestState(qn);
            st.set("cond", "3");
            st.playSound(QuestState.SOUND_MIDDLE);
            st.giveItems(ESSENCE_OF_DAIMON, 1);
        }

        // despawn raid (reset info)
        despawnRaid(npc);

        // despawn npc
        if (_npc != null) {
            _npc.deleteMe();
            _npc = null;
        }

        return null;
    }

    private void spawnNpc() {
        // spawn npc, if not spawned
        if (_npc == null) { _npc = addSpawn(DAIMON_ALTAR, 186304, -43744, -3193, 57000, false, 0, false); }
    }

    private static boolean spawnRaid() {
        L2RaidBossInstance raid = RaidBossSpawnManager.getInstance().getBosses().get(DAIMON_THE_WHITE_EYED);
        if (raid.getRaidStatus() == StatusEnum.ALIVE) {
            // set temporarily spawn location (to provide correct behavior of L2RaidBossInstance.checkAndReturnToSpawn())
            raid.getSpawn().setLocx(185900);
            raid.getSpawn().setLocy(-44000);
            raid.getSpawn().setLocz(-3160);

            // teleport raid from secret place
            raid.teleToLocation(185900, -44000, -3160, 100);
            raid.broadcastNpcSay("Who called me?");

            // set raid status
            _status = IDLE_INTERVAL;

            return true;
        }

        return false;
    }

    private static void despawnRaid(L2Npc raid) {
        // reset spawn location
        raid.getSpawn().setLocx(-106500);
        raid.getSpawn().setLocy(-252700);
        raid.getSpawn().setLocz(-15542);

        // teleport raid back to secret place
        if (!raid.isDead()) { raid.teleToLocation(-106500, -252700, -15542, 0); }

        // reset raid status
        _status = -1;
    }
}