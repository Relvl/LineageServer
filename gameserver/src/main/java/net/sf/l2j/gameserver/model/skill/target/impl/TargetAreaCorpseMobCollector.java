package net.sf.l2j.gameserver.model.skill.target.impl;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.skill.target.ISkillTargetCollector;

import java.util.ArrayList;
import java.util.List;

/**
 * Цель скилла - мертвый моб в цели и мертвые мобы в радиусе действия скилла от игрока.
 *
 * @author Johnson / 24.07.2017
 */
public class TargetAreaCorpseMobCollector implements ISkillTargetCollector {
    @Override
    public L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill) {
        // TODO На самом деле, наверное нужно позволить кастовать и с живой целью в таргете, только не трогать её. Возможно, после достижения.
        if (!(target instanceof L2Attackable) || !target.isDead()) {
            return sendIncorrect(activeChar);
        }
        if (onlyFirst) { return new L2Character[]{target}; }
        List<L2Character> targetList = new ArrayList<>();
        targetList.add(target);
        boolean srcInArena = activeChar.isInArena();
        for (L2Character obj : activeChar.getKnownList().getKnownTypeInRadius(L2Character.class, skill.getSkillRadius())) {
            // FIXME либо я туплю, либо действию подвергнутся все атакуемые или игроки. А где проверка на живость? Нужно проанализировать сами скиллы и переделать явно.
            if (!(obj instanceof L2Attackable || obj instanceof L2Playable)) { continue; }
            if (!L2Skill.checkForAreaOffensiveSkills(activeChar, obj, skill, srcInArena)) { continue; }
            targetList.add(obj);
        }
        if (targetList.isEmpty()) { return EMPTY_TARGET_LIST; }
        return targetList.toArray(new L2Character[targetList.size()]);
    }
}
