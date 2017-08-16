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
 * Цель скилла - всё атакуемое вокруг не мертвого питомца (или не мертвый питомец, если скилл требует одиночную цель), игнорируя текущую цель.
 *
 * @author Johnson / 24.07.2017
 */
public class TargetAreaSummonCollector implements ISkillTargetCollector {
    @Override
    public L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill) {
        target = activeChar.getPet();
        if (target == null || !target.isSummon() || target.isDead()) { return EMPTY_TARGET_LIST; }
        if (onlyFirst) { return new L2Character[]{ target }; }
        boolean srcInArena = activeChar.isInArena();
        List<L2Character> targetList = new ArrayList<>();
        for (L2Character obj : target.getKnownList().getKnownType(L2Character.class)) {
            if (obj == null || obj == target || obj == activeChar) { continue; }
            if (!Util.checkIfInRange(skill.getSkillRadius(), target, obj, true)) { continue; }
            if (!(obj instanceof L2Attackable || obj instanceof L2Playable)) { continue; }
            if (!L2Skill.checkForAreaOffensiveSkills(activeChar, obj, skill, srcInArena)) { continue; }
            targetList.add(obj);
        }
        if (targetList.isEmpty()) { return EMPTY_TARGET_LIST; }
        return targetList.toArray(new L2Character[targetList.size()]);
    }
}
