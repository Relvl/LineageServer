package net.sf.l2j.gameserver.model.skill.target.impl;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.skill.target.ISkillTargetCollector;

import java.util.ArrayList;
import java.util.List;

/**
 * Цель скилла - сам, свой питомец, все члены пати в радиусе каста и их питомцы в радиусе каста.
 *
 * @author Johnson / 24.07.2017
 */
public class TargetPartyCollector implements ISkillTargetCollector {
    @Override
    public L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill) {
        if (onlyFirst) { return new L2Character[]{activeChar}; }

        List<L2Character> targetList = new ArrayList<>();
        targetList.add(activeChar);
        L2PcInstance player = activeChar.getActingPlayer();

        if (activeChar.isSummon() && canAddCharacter(activeChar, player, skill.getSkillRadius(), false)) {
            targetList.add(player);
        }
        else if (activeChar.isPlayer() && canAddSummon(activeChar, player, skill.getSkillRadius(), false)) {
            targetList.add(player.getPet());
        }

        if (activeChar.isInParty()) {
            for (L2PcInstance partyMember : activeChar.getParty().getPartyMembers()) {
                if (partyMember == null || partyMember == player) { continue; }
                if (canAddCharacter(activeChar, partyMember, skill.getSkillRadius(), false)) { targetList.add(partyMember); }
                if (canAddSummon(activeChar, partyMember, skill.getSkillRadius(), false)) { targetList.add(partyMember.getPet()); }
            }
        }
        return targetList.toArray(new L2Character[targetList.size()]);
    }
}
