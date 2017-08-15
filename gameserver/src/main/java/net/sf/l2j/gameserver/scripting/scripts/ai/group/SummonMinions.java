package net.sf.l2j.gameserver.scripting.scripts.ai.group;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.scripting.EventType;
import net.sf.l2j.gameserver.scripting.scripts.ai.AbstractNpcAI;

import java.util.HashMap;
import java.util.Map;

public class SummonMinions extends AbstractNpcAI {
    private static final String[] ORCS_WORDS = {
            "Come out, you children of darkness!",
            "Destroy the enemy, my brothers!",
            "Show yourselves!",
            "Forces of darkness! Follow me!"
    };

    private static final Map<Integer, int[]> MINIONS = new HashMap<>();

    static {
        MINIONS.put(20767, new int[]{
                20768,
                20769,
                20770
        }); // Timak Orc Troop
        MINIONS.put(21524, new int[]{
                21525
        }); // Blade of Splendor
        MINIONS.put(21531, new int[]{
                21658
        }); // Punishment of Splendor
        MINIONS.put(21539, new int[]{
                21540
        }); // Wailing of Splendor
    }

    public SummonMinions() {
        super("ai/group");
        registerMobs(MINIONS.keySet(), EventType.ON_ATTACK, EventType.ON_KILL);
    }

    @Override
    public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet) {
        if (npc.isScriptValue(0)) {
            int npcId = npc.getNpcId();
            if (npcId == 20767) {
                for (int val : MINIONS.get(npcId)) {
                    addSpawn(val, npc, true, 0, false);
                }
                npc.broadcastNpcSay(ORCS_WORDS[Rnd.get(ORCS_WORDS.length)]);
            }
            else {
                for (int val : MINIONS.get(npcId)) {
                    L2Attackable newNpc = (L2Attackable) addSpawn(val, npc, true, 0, false);
                    attack(newNpc, attacker);
                }
            }
            npc.setScriptValue(1);
        }
        return super.onAttack(npc, attacker, damage, isPet);
    }
}