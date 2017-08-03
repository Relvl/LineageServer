package net.sf.l2j.gameserver.handler;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ISkillHandler {
    Logger LOGGER = LoggerFactory.getLogger(ISkillHandler.class);

    /**
     * this is the worker method that is called when using a skill.
     *
     * @param activeChar The L2Character who uses that skill.
     * @param skill      The skill object itself.
     * @param targets    Eventual targets.
     */
    void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets);

    /**
     * this method is called at initialization to register all the skill ids automatically
     *
     * @return all known itemIds
     */
    L2SkillType[] getSkillIds();
}