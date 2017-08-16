package net.sf.l2j.gameserver.model.skill.target.impl;

import net.sf.l2j.gameserver.geoengine.PathFinding;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.skill.target.ISkillTargetCollector;

import java.util.ArrayList;
import java.util.List;

/**
 * Цель скилла - живая (ха-ха!) нежить (мобы и саммоны) в радиусе действия скилла от кастующего.
 *
 * @author Johnson / 24.07.2017
 */
public class TargetAuraRaceUndeadCollector implements ISkillTargetCollector {
    @Override
    public L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill) {
        List<L2Character> targetList = new ArrayList<>();
        for (L2Character obj : activeChar.getKnownList().getKnownTypeInRadius(L2Character.class, skill.getSkillRadius())) {
            if (obj instanceof L2Npc || obj instanceof L2SummonInstance) { target = obj; }
            else { continue; }
            if (target.isAlikeDead() || !target.isUndead()) { continue; }
            if (!PathFinding.getInstance().canSeeTarget(activeChar, target)) { continue; }
            if (onlyFirst) { return new L2Character[]{ obj }; }
            targetList.add(obj);
        }
        if (targetList.isEmpty()) { return EMPTY_TARGET_LIST; }
        return targetList.toArray(new L2Character[targetList.size()]);
    }
}
