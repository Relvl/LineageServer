package net.sf.l2j.gameserver.model.skill.target.impl;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.skill.target.ISkillTargetCollector;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

import java.util.ArrayList;
import java.util.List;

/**
 * Цель скилла - все известные цели в радиусе действия скилла.
 * Центром поиска является кастовавший.
 * Если тип скилла DUMMY - целями являются сам, хозяин (игрок, если есть), окружающие NPC/Атакуемые,
 * иначе - игроки, питомцы, атакуемые NPC, но только если разрешена атака.
 *
 * @author Johnson / 24.07.2017
 */
public class TargetAuraCollector implements ISkillTargetCollector {
    @Override
    public L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill) {
        List<L2Character> targetList = new ArrayList<>();
        if (skill.getSkillType() == L2SkillType.DUMMY) {
            if (onlyFirst) { return new L2Character[]{activeChar}; }
            L2PcInstance actingPlayer = activeChar.getActingPlayer();
            targetList.add(activeChar);
            for (L2Character obj : activeChar.getKnownList().getKnownTypeInRadius(L2Character.class, skill.getSkillRadius())) {
                if (!(obj == activeChar || obj == actingPlayer || obj instanceof L2Npc || obj instanceof L2Attackable)) { continue; }
                targetList.add(obj);
            }
        }
        else {
            boolean srcInArena = activeChar.isInArena();
            for (L2Character obj : activeChar.getKnownList().getKnownTypeInRadius(L2Character.class, skill.getSkillRadius())) {
                if (obj instanceof L2Attackable || obj instanceof L2Playable) {
                    switch (skill.getTargetType()) {
                        case TARGET_FRONT_AURA:
                            if (!obj.isInFrontOf(activeChar)) { continue; }
                            break;
                        case TARGET_BEHIND_AURA:
                            if (!obj.isBehind(activeChar)) { continue; }
                            break;
                    }
                    if (!L2Skill.checkForAreaOffensiveSkills(activeChar, obj, skill, srcInArena)) { continue; }
                    if (onlyFirst) { return new L2Character[]{obj}; }
                    targetList.add(obj);
                }
            }
        }
        return targetList.toArray(new L2Character[targetList.size()]);
    }
}
