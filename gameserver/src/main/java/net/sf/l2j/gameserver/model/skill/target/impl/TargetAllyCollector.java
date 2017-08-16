package net.sf.l2j.gameserver.model.skill.target.impl;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.skill.target.ISkillTargetCollector;

import java.util.ArrayList;
import java.util.List;

/**
 * Цель скилла - сам, живые члены своего клана и живые члены своего альянса в радиусе действия скилла. А так же их питомцы.
 * Если кастующий на олимпиаде - только сам.
 * Игнорирует игроков в чужой команде дуэли.
 *
 * @author Johnson / 24.07.2017
 */
public class TargetAllyCollector implements ISkillTargetCollector {
    @Override
    public L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill) {
        L2PcInstance player = activeChar.getActingPlayer();
        if (player == null) { return EMPTY_TARGET_LIST; }

        if (onlyFirst || player.isInOlympiadMode()) { return new L2Character[]{ activeChar }; }

        List<L2Character> targetList = new ArrayList<>();
        targetList.add(player);

        if (canAddSummon(activeChar, player, skill.getSkillRadius(), false)) { targetList.add(player.getPet()); }

        if (player.getClan() != null) {
            for (L2PcInstance obj : activeChar.getKnownList().getKnownTypeInRadius(L2PcInstance.class, skill.getSkillRadius())) {
                if ((obj.getAllyId() == 0 || obj.getAllyId() != player.getAllyId()) && (obj.getClan() == null || obj.getClanId() != player.getClanId())) { continue; }
                if (player.isInDuel()) {
                    if (player.getDuelId() != obj.getDuelId()) { continue; }
                    if (player.isInParty() && obj.isInParty() && player.getParty().getPartyLeaderOID() != obj.getParty().getPartyLeaderOID()) { continue; }
                }
                if (!player.checkPvpSkill(obj, skill)) { continue; }
                L2Summon summon = obj.getPet();
                if (summon != null && !summon.isDead()) { targetList.add(summon); }
                if (!obj.isDead()) { targetList.add(obj); }
            }
        }
        return targetList.toArray(new L2Character[targetList.size()]);
    }
}
