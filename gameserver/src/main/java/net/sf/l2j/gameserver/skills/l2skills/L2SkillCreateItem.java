package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.PetItemList;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.templates.StatsSet;

public class L2SkillCreateItem extends L2Skill {
    private final int[] createItemId;
    private final int createItemCount;
    private final int randomCount;

    public L2SkillCreateItem(StatsSet set) {
        super(set);
        createItemId = set.getIntegerArray("create_item_id");
        createItemCount = set.getInteger("create_item_count", 0);
        randomCount = set.getInteger("random_count", 1);
    }

    @Override
    public void useSkill(L2Character activeChar, L2Object[] targets) {
        L2PcInstance player = activeChar.getActingPlayer();
        if (activeChar.isAlikeDead()) { return; }

        if (activeChar instanceof L2Playable) {
            if (createItemId == null || createItemCount == 0) {
                SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE);
                sm.addSkillName(this);
                activeChar.sendPacket(sm);
                return;
            }

            int count = createItemCount + Rnd.get(randomCount);
            int rndid = Rnd.get(createItemId.length);

            if (activeChar.isPlayer()) {
                player.addItem(EItemProcessPurpose.SKILL, createItemId[rndid], count, activeChar, true);
            }
            else if (activeChar instanceof L2PetInstance) {
                activeChar.getInventory().addItem(EItemProcessPurpose.SKILL, createItemId[rndid], count, player, activeChar);
                player.sendPacket(new PetItemList((L2PetInstance) activeChar));
            }
        }
    }
}