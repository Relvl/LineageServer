package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

public class Craft implements ISkillHandler {
    private static final L2SkillType[] SKILL_IDS = { L2SkillType.COMMON_CRAFT, L2SkillType.DWARVEN_CRAFT };

    @Override
    public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets) {
        if (!(activeChar instanceof L2PcInstance)) { return; }
        L2PcInstance player = (L2PcInstance) activeChar;
        if (player.isInStoreMode()) {
            player.sendPacket(SystemMessageId.CANNOT_CREATED_WHILE_ENGAGED_IN_TRADING);
            return;
        }
        player.getRecipeController().requestBookOpen(skill.getSkillType() == L2SkillType.DWARVEN_CRAFT);
    }

    @Override
    public L2SkillType[] getSkillIds() {
        return SKILL_IDS;
    }
}