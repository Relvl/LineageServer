package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.network.client.game_to_client.SetSummonRemainTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Johnson / 25.07.2017
 */
class SummonLifetimeTask implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SummonLifetimeTask.class);

    private final L2PcInstance player;
    private final L2SummonInstance summon;

    SummonLifetimeTask(L2PcInstance player, L2SummonInstance summon) {
        this.player = player;
        this.summon = summon;
    }

    @Override
    public void run() {
        try {
            double oldTimeRemaining = summon.getTimeRemaining();
            int maxTime = summon.getTotalLifeTime();

            // if pet is attacking
            if (summon.isAttackingNow()) { summon.decTimeRemaining(summon.getTimeLostActive()); }
            else { summon.decTimeRemaining(summon.getTimeLostIdle()); }

            double newTimeRemaining = summon.getTimeRemaining();

            // check if the summon's lifetime has ran out
            if (newTimeRemaining < 0) { summon.unSummon(player); }
            else if ((newTimeRemaining <= summon.getNextItemConsumeTime()) && (oldTimeRemaining > summon.getNextItemConsumeTime())) {
                summon.decNextItemConsumeTime(maxTime / (summon.getItemConsumeSteps() + 1));

                // check if owner has enought itemConsume, if requested
                if (summon.getItemConsumeCount() > 0
                        && summon.getItemConsumeId() != 0
                        && !summon.isDead()
                        && summon.getOwner().getInventory().destroyItemByItemId(EItemProcessPurpose.CONSUME, summon.getItemConsumeId(), summon.getItemConsumeCount(), summon.getOwner(), player, true) == null) {
                    summon.unSummon(player);
                }
            }

            // prevent useless packet-sending when the difference isn't visible.
            if ((summon.getLastShowntimeRemaining() - newTimeRemaining) > maxTime / 352) {
                player.sendPacket(new SetSummonRemainTime(maxTime, (int) newTimeRemaining));
                summon.setLastShowntimeRemaining((int) newTimeRemaining);
                summon.updateEffectIcons();
            }
        }
        catch (Exception e) {
            LOGGER.error("Error on player [{}] summon item consume task.", player.getName(), e);
        }
    }
}
