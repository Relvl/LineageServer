package net.sf.l2j.gameserver.model.skill.target.impl;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.skill.target.ISkillTargetCollector;
import net.sf.l2j.gameserver.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Johnson / 24.07.2017
 */
public class TargetAreaCollector implements ISkillTargetCollector {
    @Override
    public L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill) {

        if (skill.getCastRange() >= 0 && (target == null || target == activeChar || target.isAlikeDead())) {
            return sendIncorrect(activeChar);
        }
        if (!(target instanceof L2Attackable || target instanceof L2Playable)) {
            return sendIncorrect(activeChar);
        }

        List<L2Character> targetList = new ArrayList<>();
        final L2Character origin;
        boolean srcInArena = activeChar.isInArena();
        if (skill.getCastRange() >= 0) {
            if (!L2Skill.checkForAreaOffensiveSkills(activeChar, target, skill, srcInArena)) {
                return sendIncorrect(activeChar);
            }
            if (onlyFirst) { return new L2Character[]{target}; }
            origin = target;
            targetList.add(origin);
        }
        else { origin = activeChar; }

        for (L2Character obj : activeChar.getKnownList().getKnownType(L2Character.class)) {
            if (!(obj instanceof L2Attackable || obj instanceof L2Playable)) { continue; }
            if (obj == origin) { continue; }
            if (Util.checkIfInRange(skill.getSkillRadius(), origin, obj, true)) {
                switch (skill.getTargetType()) {
                    case TARGET_FRONT_AREA:
                        if (!obj.isInFrontOf(activeChar)) { continue; }
                        break;
                    case TARGET_BEHIND_AREA:
                        if (!obj.isBehind(activeChar)) { continue; }
                        break;
                }
                if (!L2Skill.checkForAreaOffensiveSkills(activeChar, obj, skill, srcInArena)) { continue; }
                targetList.add(obj);
            }
        }
        if (targetList.isEmpty()) { return EMPTY_TARGET_LIST; }
        return targetList.toArray(new L2Character[targetList.size()]);
    }
}
