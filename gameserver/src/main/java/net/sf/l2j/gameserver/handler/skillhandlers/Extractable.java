package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2ExtractableProductItem;
import net.sf.l2j.gameserver.model.L2ExtractableSkill;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

public class Extractable implements ISkillHandler {
    private static final L2SkillType[] SKILL_IDS =
            {
                    L2SkillType.EXTRACTABLE,
                    L2SkillType.EXTRACTABLE_FISH
            };

    @Override
    public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets) {
        if (!(activeChar instanceof L2PcInstance)) { return; }

        L2ExtractableSkill exItem = skill.getExtractableSkill();
        if (exItem == null || exItem.getProductItemsArray().isEmpty()) {
            LOGGER.warn("Missing informations for extractable skill id: {}.", skill.getId());
            return;
        }

        L2PcInstance player = activeChar.getActingPlayer();
        int chance = Rnd.get(100000);

        boolean created = false;
        int chanceIndex = 0;

        for (L2ExtractableProductItem expi : exItem.getProductItemsArray()) {
            chanceIndex += (int) (expi.getChance() * 1000);
            if (chance <= chanceIndex) {
                for (IntIntHolder item : expi.getItems()) { player.addItem(EItemProcessPurpose.EXTRACT, item.getId(), item.getValue(), targets[0], true); }

                created = true;
                break;
            }
        }

        if (!created) {
            player.sendPacket(SystemMessageId.NOTHING_INSIDE_THAT);
            return;
        }
    }

    @Override
    public L2SkillType[] getSkillIds() {
        return SKILL_IDS;
    }
}