package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.instancemanager.SevenSignsFestival;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;

public class L2FestivalMonsterInstance extends L2MonsterInstance {
    protected int _bonusMultiplier = 1;

    public L2FestivalMonsterInstance(int objectId, NpcTemplate template) {
        super(objectId, template);
    }

    public void setOfferingBonus(int bonusMultiplier) {
        _bonusMultiplier = bonusMultiplier;
    }

    @Override
    public boolean isAutoAttackable(L2Character attacker) {
        return !(attacker instanceof L2FestivalMonsterInstance);

    }

    @Override
    public boolean isAggressive() {
        return true;
    }

    @Override
    public boolean hasRandomAnimation() {
        return false;
    }

    @Override
    public void doItemDrop(L2Character attacker) {
        L2PcInstance player = attacker.getActingPlayer();
        if (player == null || !player.isInParty()) { return; }

        player.getParty().getLeader().addItem(EItemProcessPurpose.SEVEN_SIGNS, SevenSignsFestival.FESTIVAL_OFFERING_ID, _bonusMultiplier, attacker, true);

        super.doItemDrop(attacker);
    }
}