package net.sf.l2j.gameserver.taskmanager;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2CubicInstance;
import net.sf.l2j.gameserver.network.client.game_to_client.AutoAttackStop;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public final class AttackStanceTaskManager implements Runnable {
    private static final long ATTACK_STANCE_PERIOD = 15000; // 15 seconds

    private final Map<L2Character, Long> characters = new ConcurrentHashMap<>();

    private AttackStanceTaskManager() {
        ThreadPoolManager.getInstance().scheduleAiAtFixedRate(this, 1000, 1000);
    }

    public static AttackStanceTaskManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public void add(L2Character character) {
        if (character instanceof L2Playable) {
            for (L2CubicInstance cubic : character.getActingPlayer().getCubics().values()) {
                if (cubic.getId() != L2CubicInstance.LIFE_CUBIC) {
                    cubic.doAction();
                }
            }
        }

        characters.put(character, System.currentTimeMillis() + ATTACK_STANCE_PERIOD);
    }

    public void remove(L2Character character) {
        if (character.isSummon()) {
            character = character.getActingPlayer();
        }
        characters.remove(character);
    }

    public boolean isInAttackStance(L2Character character) {
        if (character.isSummon()) {
            character = character.getActingPlayer();
        }
        return characters.containsKey(character);
    }

    @Override
    public void run() {
        if (characters.isEmpty()) { return; }
        long time = System.currentTimeMillis();

        for (Entry<L2Character, Long> entry : characters.entrySet()) {
            if (time < entry.getValue()) { continue; }
            L2Character character = entry.getKey();

            character.broadcastPacket(new AutoAttackStop(character.getObjectId()));
            if (character.isPlayer() && character.getPet() != null) {
                character.getPet().broadcastPacket(new AutoAttackStop(character.getPet().getObjectId()));
            }

            character.getAI().setAutoAttacking(false);
            characters.remove(character);
        }
    }

    private static final class SingletonHolder {
        private static final AttackStanceTaskManager INSTANCE = new AttackStanceTaskManager();
    }
}