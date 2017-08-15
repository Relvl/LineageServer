package net.sf.l2j.gameserver.scripting.scripts.ai;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.scripting.EventType;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract NPC AI class for datapack based AIs.
 *
 * @author UnAfraid, Zoey76
 * @deprecated Наркоманы блять... Есть нормальный ИИ движок, нахуй для ИИ использовать квесты?!
 */
@Deprecated
public abstract class AbstractNpcAI extends Quest {
    protected AbstractNpcAI(String descr) {
        super(-1, descr);
    }

    public void registerMob(int mob, EventType... types) {
        for (EventType type : types) {
            addEventId(mob, type);
        }
    }

    public void registerMobs(int... mobs) {
        for (int id : mobs) {
            addEventId(id, EventType.ON_ATTACK);
            addEventId(id, EventType.ON_KILL);
            addEventId(id, EventType.ON_SPAWN);
            addEventId(id, EventType.ON_SPELL_FINISHED);
            addEventId(id, EventType.ON_SKILL_SEE);
            addEventId(id, EventType.ON_FACTION_CALL);
            addEventId(id, EventType.ON_AGGRO);
        }
    }

    public void registerMobs(int[] mobs, EventType... types) {
        for (int id : mobs) {
            for (EventType type : types) {
                addEventId(id, type);
            }
        }
    }

    public void registerMobs(Iterable<Integer> mobs, EventType... types) {
        for (int id : mobs) {
            for (EventType type : types) {
                addEventId(id, type);
            }
        }
    }

    public static void attack(L2Attackable npc, L2Playable playable, int aggro) {
        npc.setIsRunning(true);
        npc.addDamageHate(playable, 0, (aggro <= 0) ? 999 : aggro);
        npc.getAI().setIntention(EIntention.ATTACK, playable);
    }

    public static L2PcInstance getRandomPlayer(L2Npc npc) {
        List<L2PcInstance> result = new ArrayList<>();
        for (L2PcInstance player : npc.getKnownList().getKnownType(L2PcInstance.class)) {
            if (player.isDead()) { continue; }
            if (player.isGM() && player.isInvisible()) { continue; }
            result.add(player);
        }
        return (result.isEmpty()) ? null : Rnd.get(result);
    }

    public static int getPlayersCountInRadius(int range, L2Character npc, boolean invisible) {
        int count = 0;
        for (L2PcInstance player : npc.getKnownList().getKnownType(L2PcInstance.class)) {
            if (player.isDead()) { continue; }
            if (!invisible && player.isInvisible()) { continue; }
            if (Util.checkIfInRange(range, npc, player, true)) { count++; }
        }
        return count;
    }

    /**
     * Under that barbarian name, return the number of players in front, back and sides of the npc.<br>
     * Dead players aren't counted, invisible ones is the boolean parameter.
     *
     * @param range     : the radius.
     * @param npc       : the object to make the test on.
     * @param invisible : true counts invisible characters.
     * @return an array composed of front, back and side targets number.
     */
    public static int[] getPlayersCountInPositions(int range, L2Character npc, boolean invisible) {
        int frontCount = 0;
        int backCount = 0;
        int sideCount = 0;

        for (L2PcInstance player : npc.getKnownList().getKnownType(L2PcInstance.class)) {
            if (player.isDead()) { continue; }
            if (!invisible && player.isInvisible()) { continue; }
            if (!Util.checkIfInRange(range, npc, player, true)) { continue; }
            if (player.isInFrontOf(npc)) { frontCount++; }
            else if (player.isBehind(npc)) { backCount++; }
            else { sideCount++; }
        }

        return new int[]{
                frontCount,
                backCount,
                sideCount
        };
    }

    public static void attack(L2Attackable npc, L2Playable playable) {
        attack(npc, playable, 0);
    }
}