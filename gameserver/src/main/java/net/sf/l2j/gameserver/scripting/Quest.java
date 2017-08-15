package net.sf.l2j.gameserver.scripting;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.cache.HtmlCacheNew;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.location.HeadedLocation;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.network.client.game_to_client.ActionFailed;
import net.sf.l2j.gameserver.network.client.game_to_client.NpcHtmlMessage;
import net.sf.l2j.gameserver.util.threading.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings({"BooleanMethodNameMustStartWithQuestion", "EqualsAndHashcode"})
public abstract class Quest {
    protected static final Logger LOGGER = LoggerFactory.getLogger(Quest.class);

    private static final String LOAD_QUEST_STATES = "SELECT name,value FROM character_quests WHERE charId=? AND var='<state>'";
    private static final String LOAD_QUEST_VARIABLES = "SELECT name,var,value FROM character_quests WHERE charId=? AND var<>'<state>'";

    private final Map<Integer, List<QuestTimer>> eventTimers = new ConcurrentHashMap<>();

    private final int id;
    private final String descr;
    private int[] itemsIds;

    protected Quest(int questId, String descr) {
        id = questId;
        this.descr = descr;
    }

    public final String getName() { return getClass().getSimpleName(); }

    public final int getQuestId() { return id; }

    public final boolean isRealQuest() { return id > 0; }

    public final String getDescr() { return descr; }

    public final int[] getItemsIds() { return itemsIds; }

    public final void setItemsIds(int... itemIds) { itemsIds = itemIds; }

    public QuestState newQuestState(L2PcInstance player) { return new QuestState(player, this, QuestState.STATE_CREATED); }

    public static void playerEnterWorld(L2PcInstance player) {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection();) {
            PreparedStatement loadStatement = con.prepareStatement(LOAD_QUEST_STATES);
            loadStatement.setInt(1, player.getObjectId());
            ResultSet rs = loadStatement.executeQuery();
            while (rs.next()) {
                String questId = rs.getString("name");
                Quest q = EScript.getQuest(questId);
                if (q == null) {
                    LOGGER.warn("Unknown  quest {} for player {}", questId, player.getName());
                    continue;
                }
                new QuestState(player, q, rs.getByte("value"));
            }
            rs.close();
            loadStatement.close();

            loadStatement = con.prepareStatement(LOAD_QUEST_VARIABLES);
            loadStatement.setInt(1, player.getObjectId());
            rs = loadStatement.executeQuery();
            while (rs.next()) {
                String questId = rs.getString("name");

                QuestState qs = player.getQuestState(questId);
                if (qs == null) {
                    LOGGER.warn("Unknown quest {} for player {}", questId, player.getName());
                    continue;
                }

                qs.setInternal(rs.getString("var"), rs.getString("value"));
            }
            rs.close();
            loadStatement.close();
        }
        catch (Exception e) {
            LOGGER.error("could not insert char quest:", e);
        }
    }

    public L2PcInstance getRandomPartyMember(L2PcInstance player, L2Object object) {
        if (player == null) { return null; }
        if (object == null || !player.isInParty()) { return player; }
        List<L2PcInstance> members = new ArrayList<>();
        for (L2PcInstance member : player.getParty().getPartyMembers()) {
            if (member.isInsideRadius(object, Config.ALT_PARTY_RANGE, true, false)) {
                members.add(member);
            }
        }
        if (members.isEmpty()) { return null; }
        return members.get(Rnd.get(members.size()));
    }

    public QuestState checkPlayerCondition(L2PcInstance player, L2Npc npc, String varName, String value) {
        if (player == null) { return null; }
        QuestState st = player.getQuestState(getName());
        if (st == null) { return null; }
        if (st.get(varName) == null || !value.equalsIgnoreCase(st.get(varName))) { return null; }
        if (npc == null) { return null; }
        if (!player.isInsideRadius(npc, Config.ALT_PARTY_RANGE, true, false)) { return null; }
        return st;
    }

    public List<L2PcInstance> getPartyMembers(L2PcInstance player, L2Npc npc, String varName, String value) {
        List<L2PcInstance> candidates = new ArrayList<>();
        if (player != null && player.isInParty()) {
            for (L2PcInstance partyMember : player.getParty().getPartyMembers()) {
                if (partyMember == null) { continue; }
                if (checkPlayerCondition(partyMember, npc, varName, value) != null) {
                    candidates.add(partyMember);
                }
            }
        }
        else if (checkPlayerCondition(player, npc, varName, value) != null) {
            candidates.add(player);
        }
        return candidates;
    }

    public L2PcInstance getRandomPartyMember(L2PcInstance player, L2Npc npc, String varName, String value) {
        if (player == null) { return null; }
        List<L2PcInstance> candidates = getPartyMembers(player, npc, varName, value);
        if (candidates.isEmpty()) { return null; }
        return candidates.get(Rnd.get(candidates.size()));
    }

    public L2PcInstance getRandomPartyMember(L2PcInstance player, L2Npc npc, String value) {
        return getRandomPartyMember(player, npc, "cond", value);
    }

    public QuestState checkPlayerState(L2PcInstance player, L2Npc npc, byte state) {
        if (player == null) { return null; }
        QuestState st = player.getQuestState(getName());
        if (st == null) { return null; }
        if (st.getState() != state) { return null; }
        if (npc == null) { return null; }
        if (!player.isInsideRadius(npc, Config.ALT_PARTY_RANGE, true, false)) { return null; }
        return st;
    }

    public List<L2PcInstance> getPartyMembersState(L2PcInstance player, L2Npc npc, byte state) {
        List<L2PcInstance> candidates = new ArrayList<>();
        if (player != null && player.isInParty()) {
            for (L2PcInstance partyMember : player.getParty().getPartyMembers()) {
                if (partyMember == null) { continue; }
                if (checkPlayerState(partyMember, npc, state) != null) {
                    candidates.add(partyMember);
                }
            }
        }
        else if (checkPlayerState(player, npc, state) != null) { candidates.add(player); }
        return candidates;
    }

    public L2PcInstance getRandomPartyMemberState(L2PcInstance player, L2Npc npc, byte state) {
        if (player == null) { return null; }
        List<L2PcInstance> candidates = getPartyMembersState(player, npc, state);
        if (candidates.isEmpty()) { return null; }
        return candidates.get(Rnd.get(candidates.size()));
    }

    public QuestState getClanLeaderQuestState(L2PcInstance player, L2Npc npc) {
        if (player.isClanLeader() && player.isInsideRadius(npc, Config.ALT_PARTY_RANGE, true, false)) {
            return player.getQuestState(getName());
        }
        L2Clan clan = player.getClan();
        if (clan == null) { return null; }
        L2PcInstance leader = clan.getLeader().getPlayerInstance();
        if (leader == null) { return null; }
        if (leader.isInsideRadius(npc, Config.ALT_PARTY_RANGE, true, false)) {
            return leader.getQuestState(getName());
        }
        return null;
    }

    public L2Npc addSpawn(int npcId, L2Character cha, boolean randomOffset, long despawnDelay, boolean isSummonSpawn) {
        return addSpawn(npcId, cha.getX(), cha.getY(), cha.getZ(), cha.getHeading(), randomOffset, despawnDelay, isSummonSpawn);
    }

    public L2Npc addSpawn(int npcId, HeadedLocation loc, boolean randomOffset, long despawnDelay, boolean isSummonSpawn) {
        return addSpawn(npcId, loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), randomOffset, despawnDelay, isSummonSpawn);
    }

    public L2Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffset, long despawnDelay, boolean isSummonSpawn) {
        try {
            NpcTemplate template = NpcTable.getInstance().getTemplate(npcId);
            if (template != null) {
                // TODO Sometimes, even if the quest script specifies some xyz (for example npc.getX() etc) by the time the code reaches here, xyz have become 0!
                // Also, a questdev might have purposely set xy to 0,0...however, the spawn code is coded such that if x=y=0, it looks into location for the spawn loc!
                // This will NOT work with quest spawns!
                // For both of the above cases, we need a fail-safe spawn.
                // For this, we use the default spawn location, which is at the player's loc.
                if ((x == 0) && (y == 0)) {
                    LOGGER.error("Failed to adjust bad locks for quest spawn!  Spawn aborted!");
                    return null;
                }

                if (randomOffset) {
                    x += Rnd.get(-100, 100);
                    y += Rnd.get(-100, 100);
                }

                L2Spawn spawn = new L2Spawn(template);
                spawn.setHeading(heading);
                spawn.setLocx(x);
                spawn.setLocy(y);
                spawn.setLocz(z + 20);
                spawn.stopRespawn();
                L2Npc result = spawn.doSpawn(isSummonSpawn);

                if (despawnDelay > 0) { result.scheduleDespawn(despawnDelay); }

                return result;
            }
        }
        catch (ClassNotFoundException | NoSuchMethodException e1) {
            LOGGER.error("Could not spawn Npc {}", npcId, e1);
        }

        return null;
    }

    public static String getNoQuestMsg() { return HtmlCacheNew.getInstance().getHtml("data/html/scripts/quests/NoQuestAvailable.htm"); }

    public static String getAlreadyCompletedMsg() { return HtmlCacheNew.getInstance().getHtml("data/html/scripts/quests/QuestAllreadyComplete.htm"); }

    public boolean showResult(L2Npc npc, L2PcInstance player, String result) {
        if (player == null || result == null || result.isEmpty()) { return false; }
        // Показать указанный файл.
        if (result.endsWith(".htm") || result.endsWith(".html")) {
            NpcHtmlMessage npcReply = new NpcHtmlMessage(npc == null ? 0 : npc.getNpcId());
            if (isRealQuest()) {
                npcReply.setFile("./data/html/scripts/quests/" + getName() + "/" + result);
            }
            else {
                npcReply.setFile("./data/html/scripts/" + descr + "/" + getName() + "/" + result);
            }

            if (npc != null) { npcReply.replace("%objectId%", npc.getObjectId()); }

            player.sendPacket(npcReply);
            player.sendPacket(ActionFailed.STATIC_PACKET);
        }
        // Показать HTML.
        else if (result.startsWith("<html>")) {
            NpcHtmlMessage npcReply = new NpcHtmlMessage(npc == null ? 0 : npc.getNpcId());
            npcReply.setHtml(result);

            if (npc != null) { npcReply.replace("%objectId%", npc.getObjectId()); }

            player.sendPacket(npcReply);
            player.sendPacket(ActionFailed.STATIC_PACKET);
        }
        // Показать сообщение в сером чате.
        else { player.sendMessage(result); }

        return true;
    }

    public boolean showError(L2PcInstance player, Throwable e) {
        LOGGER.warn(getClass().getName(), e);
        if (player != null && player.isGM()) {
            NpcHtmlMessage npcReply = new NpcHtmlMessage(0);
            npcReply.setHtml("<html><body><title>Script error</title>" + e.getMessage() + "</body></html>");
            player.sendPacket(npcReply);
            player.sendPacket(ActionFailed.STATIC_PACKET);
            return true;
        }
        return false;
    }

    public String getHtmlText(String fileName) {
        if (isRealQuest()) { return HtmCache.getInstance().getHtmForce("./data/html/scripts/quests/" + getName() + "/" + fileName); }
        return HtmCache.getInstance().getHtmForce("./data/html/scripts/" + descr + "/" + getName() + "/" + fileName);
    }

    public void addEventId(int npcId, EventType eventType) {
        try {
            NpcTemplate t = NpcTable.getInstance().getTemplate(npcId);
            if (t != null) {
                t.addQuestEvent(eventType, this);
            }
        }
        catch (RuntimeException e) {
            LOGGER.error("Exception on addEventId(): {}", e.getMessage(), e);
        }
    }

    public void addStartNpc(int... npcIds) {
        for (int npcId : npcIds) {
            addEventId(npcId, EventType.QUEST_START);
        }
    }

    public void addAttackId(int... npcIds) {
        for (int npcId : npcIds) {
            addEventId(npcId, EventType.ON_ATTACK);
        }
    }

    public final boolean notifyAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet) {
        String res;
        try {
            res = onAttack(npc, attacker, damage, isPet);
        }
        catch (RuntimeException e) {
            return showError(attacker, e);
        }
        return showResult(npc, attacker, res);
    }

    public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet) { return null; }

    public void addAttackActId(int... npcIds) {
        for (int npcId : npcIds) {
            addEventId(npcId, EventType.ON_ATTACK_ACT);
        }
    }

    public final boolean notifyAttackAct(L2Npc npc, L2PcInstance victim) {
        String res;
        try {
            res = onAttackAct(npc, victim);
        }
        catch (RuntimeException e) {
            return showError(victim, e);
        }
        return showResult(npc, victim, res);
    }

    public String onAttackAct(L2Npc npc, L2PcInstance victim) { return null; }

    public void addAggroRangeEnterId(int... npcIds) {
        for (int npcId : npcIds) {
            addEventId(npcId, EventType.ON_AGGRO);
        }
    }

    public final boolean notifyAggro(L2Npc npc, L2PcInstance player, boolean isPet) {
        ThreadPoolManager.getInstance().executeAi(new OnAggroEnter(npc, player, isPet));
        return true;
    }

    public String onAggro(L2Npc npc, L2PcInstance player, boolean isPet) { return null; }

    public final boolean notifyDeath(L2Character killer, L2PcInstance player) {
        String res;
        try {
            res = onDeath(killer, player);
        }
        catch (RuntimeException e) {
            return showError(player, e);
        }
        if (killer instanceof L2Npc) {
            return showResult((L2Npc) killer, player, res);
        }
        return showResult(null, player, res);
    }

    public String onDeath(L2Character killer, L2PcInstance player) {
        if (killer instanceof L2Npc) {
            return onAdvEvent("", (L2Npc) killer, player);
        }
        return onAdvEvent("", null, player);
    }

    public final boolean notifyEvent(String event, L2Npc npc, L2PcInstance player) {
        String res;
        try {
            res = onAdvEvent(event, npc, player);
        }
        catch (RuntimeException e) {
            return showError(player, e);
        }
        return showResult(npc, player, res);
    }

    public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) { return null; }

    public void addEnterZoneId(int... zoneIds) {
        for (int zoneId : zoneIds) {
            L2ZoneType zone = ZoneManager.getInstance().getZoneById(zoneId);
            if (zone != null) {
                zone.addQuestEvent(EventType.ON_ENTER_ZONE, this);
            }
        }
    }

    public final boolean notifyEnterZone(L2Character character, L2ZoneType zone) {
        L2PcInstance player = character.getActingPlayer();
        String res = null;
        try {
            res = onEnterZone(character, zone);
        }
        catch (RuntimeException e) {
            if (player != null) { return showError(player, e); }
        }
        return player == null || showResult(null, player, res);
    }

    public String onEnterZone(L2Character character, L2ZoneType zone) { return null; }

    public void addExitZoneId(int... zoneIds) {
        for (int zoneId : zoneIds) {
            L2ZoneType zone = ZoneManager.getInstance().getZoneById(zoneId);
            if (zone != null) {
                zone.addQuestEvent(EventType.ON_EXIT_ZONE, this);
            }
        }
    }

    public final boolean notifyExitZone(L2Character character, L2ZoneType zone) {
        L2PcInstance player = character.getActingPlayer();
        String res = null;
        try {
            res = onExitZone(character, zone);
        }
        catch (RuntimeException e) {
            if (player != null) { return showError(player, e); }
        }
        return player == null || showResult(null, player, res);
    }

    public String onExitZone(L2Character character, L2ZoneType zone) { return null; }

    public void addFactionCallId(int... npcIds) {
        for (int npcId : npcIds) {
            addEventId(npcId, EventType.ON_FACTION_CALL);
        }
    }

    public final boolean notifyFactionCall(L2Npc npc, L2Npc caller, L2PcInstance attacker, boolean isPet) {
        String res;
        try {
            res = onFactionCall(npc, caller, attacker, isPet);
        }
        catch (RuntimeException e) {
            return showError(attacker, e);
        }
        return showResult(npc, attacker, res);
    }

    public String onFactionCall(L2Npc npc, L2Npc caller, L2PcInstance attacker, boolean isPet) { return null; }

    public void addFirstTalkId(int... npcIds) {
        for (int npcId : npcIds) {
            addEventId(npcId, EventType.ON_FIRST_TALK);
        }
    }

    public final boolean notifyFirstTalk(L2Npc npc, L2PcInstance player) {
        String res;
        try {
            res = onFirstTalk(npc, player);
        }
        catch (RuntimeException e) {
            return showError(player, e);
        }
        if (res != null && !res.isEmpty()) { return showResult(npc, player, res); }
        player.sendPacket(ActionFailed.STATIC_PACKET);
        return true;
    }

    public String onFirstTalk(L2Npc npc, L2PcInstance player) { return null; }

    public void addItemUse(int... itemIds) {
        for (int itemId : itemIds) {
            Item t = ItemTable.getInstance().getTemplate(itemId);
            if (t != null) {
                t.addQuestEvent(this);
            }
        }
    }

    public final boolean notifyItemUse(L2ItemInstance item, L2PcInstance player, L2Object target) {
        String res;
        try {
            res = onItemUse(item, player, target);
        }
        catch (RuntimeException e) {
            return showError(player, e);
        }
        return showResult(null, player, res);
    }

    public String onItemUse(L2ItemInstance item, L2PcInstance player, L2Object target) { return null; }

    public void addKillId(int... killIds) {
        for (int killId : killIds) {
            addEventId(killId, EventType.ON_KILL);
        }
    }

    public final boolean notifyKill(L2Npc npc, L2PcInstance killer, boolean isPet) {
        String res;
        try {
            res = onKill(npc, killer, isPet);
        }
        catch (RuntimeException e) {
            return showError(killer, e);
        }
        return showResult(npc, killer, res);
    }

    public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet) { return null; }

    public void addSpawnId(int... npcIds) {
        for (int npcId : npcIds) {
            addEventId(npcId, EventType.ON_SPAWN);
        }
    }

    public final boolean notifySpawn(L2Npc npc) {
        try {
            onSpawn(npc);
        }
        catch (RuntimeException e) {
            LOGGER.error("Exception on onSpawn() in notifySpawn(): {}", e.getMessage(), e);
            return true;
        }
        return false;
    }

    public String onSpawn(L2Npc npc) { return null; }

    public void addSkillSeeId(int... npcIds) {
        for (int npcId : npcIds) {
            addEventId(npcId, EventType.ON_SKILL_SEE);
        }
    }

    public final boolean notifySkillSee(L2Npc npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet) {
        ThreadPoolManager.getInstance().executeAi(new OnSkillSee(npc, caster, skill, targets, isPet));
        return true;
    }

    public String onSkillSee(L2Npc npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet) { return null; }

    public void addSpellFinishedId(int... npcIds) {
        for (int npcId : npcIds) {
            addEventId(npcId, EventType.ON_SPELL_FINISHED);
        }
    }

    public final boolean notifySpellFinished(L2Npc npc, L2PcInstance player, L2Skill skill) {
        String res;
        try {
            res = onSpellFinished(npc, player, skill);
        }
        catch (RuntimeException e) {
            return showError(player, e);
        }
        return showResult(npc, player, res);
    }

    public String onSpellFinished(L2Npc npc, L2PcInstance player, L2Skill skill) { return null; }

    public void addTalkId(int... talkIds) {
        for (int talkId : talkIds) {
            addEventId(talkId, EventType.ON_TALK);
        }
    }

    public final boolean notifyTalk(L2Npc npc, L2PcInstance player) {
        String res;
        try {
            res = onTalk(npc, player);
        }
        catch (RuntimeException e) {
            return showError(player, e);
        }
        player.setLastQuestNpcObject(npc.getObjectId());
        return showResult(npc, player, res);
    }

    public String onTalk(L2Npc npc, L2PcInstance talker) { return null; }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Quest) {
            Quest q = (Quest) obj;
            return id == q.id && getName().equals(q.getName());
        }
        return false;
    }

    // region QUEST TIMERS ============================================================================================
    public void startQuestTimer(String event, long time, L2Npc npc, L2PcInstance player, boolean repeating) {
        List<QuestTimer> timers = eventTimers.get(event.hashCode());
        if (timers == null) {
            timers = new CopyOnWriteArrayList<>();
            timers.add(new QuestTimer(this, event, npc, player, time, repeating));
            eventTimers.put(event.hashCode(), timers);
        }
        else {
            // Если есть таймер для этого ивента, нпс и игрока - не добавляем, просто уходим.
            for (QuestTimer timer : timers) {
                if (timer != null && timer.equals(this, event, npc, player)) {
                    return;
                }
            }
            timers.add(new QuestTimer(this, event, npc, player, time, repeating));
        }
    }

    public void cancelQuestTimer(String event, L2Npc npc, L2PcInstance player) {
        List<QuestTimer> timers = eventTimers.get(event.hashCode());
        if (timers == null || timers.isEmpty()) { return; }
        for (QuestTimer timer : timers) {
            if (timer != null && timer.equals(this, event, npc, player)) {
                timer.cancel();
            }
        }
    }

    public void cancelQuestTimers(String event) {
        List<QuestTimer> timers = eventTimers.get(event.hashCode());
        if (timers == null || timers.isEmpty()) { return; }
        for (QuestTimer timer : timers) {
            if (timer != null) {
                timer.cancel();
            }
        }
    }

    void removeQuestTimer(QuestTimer timer) {
        if (timer == null) { return; }
        List<QuestTimer> timers = eventTimers.get(timer.toString().hashCode());
        if (timers == null || timers.isEmpty()) { return; }
        timers.remove(timer);
    }

    // endregion QUEST TIMERS =========================================================================================

    private class OnAggroEnter implements Runnable {
        private final L2Npc npc;
        private final L2PcInstance player;
        private final boolean isPet;

        private OnAggroEnter(L2Npc npc, L2PcInstance player, boolean isPet) {
            this.npc = npc;
            this.player = player;
            this.isPet = isPet;
        }

        @Override
        public void run() {
            String res = null;
            try {
                res = onAggro(npc, player, isPet);
            }
            catch (RuntimeException e) {
                showError(player, e);
            }
            showResult(npc, player, res);
        }
    }

    public class OnSkillSee implements Runnable {
        private final L2Npc npc;
        private final L2PcInstance player;
        private final L2Skill skill;
        private final L2Object[] targets;
        private final boolean isPet;

        public OnSkillSee(L2Npc npc, L2PcInstance player, L2Skill skill, L2Object[] targets, boolean isPet) {
            this.npc = npc;
            this.player = player;
            this.skill = skill;
            this.targets = targets;
            this.isPet = isPet;
        }

        @Override
        public void run() {
            String res = null;
            try {
                res = onSkillSee(npc, player, skill, targets, isPet);
            }
            catch (RuntimeException e) {
                showError(player, e);
            }
            showResult(npc, player, res);
        }
    }

}