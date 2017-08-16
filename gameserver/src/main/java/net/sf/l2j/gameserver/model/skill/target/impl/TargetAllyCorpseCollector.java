package net.sf.l2j.gameserver.model.skill.target.impl;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.skill.target.ISkillTargetCollector;
import net.sf.l2j.gameserver.model.zone.ZoneId;

import java.util.ArrayList;
import java.util.List;

/**
 * Цель скилла - все мертвые члены своего клана и своего альянса в радиусе действия скилла.
 * Игнорируются игроки в осадной зоне, но не участвующие в осаде.
 * Игнорируются игроки в чужой команде дуэли.
 *
 * @author Johnson / 24.07.2017
 */
public class TargetAllyCorpseCollector implements ISkillTargetCollector {
    @Override
    public L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill) {
        L2PcInstance player = activeChar.getActingPlayer();
        if (player == null) { return EMPTY_TARGET_LIST; }
        if (onlyFirst || player.isInOlympiadMode()) {
            return new L2Character[]{ activeChar };
        }
        List<L2Character> targetList = new ArrayList<>();
        targetList.add(activeChar);
        if (player.getClan() != null) {
            for (L2PcInstance obj : activeChar.getKnownList().getKnownTypeInRadius(L2PcInstance.class, skill.getSkillRadius())) {
                if (!obj.isDead()) { continue; }
                if ((obj.getAllyId() == 0 || obj.getAllyId() != player.getAllyId()) && (obj.getClan() == null || obj.getClanId() != player.getClanId())) { continue; }
                if (player.isInDuel()) {
                    if (player.getDuelId() != obj.getDuelId()) { continue; }
                    if (player.isInParty() && obj.isInParty() && player.getParty().getPartyLeaderOID() != obj.getParty().getPartyLeaderOID()) { continue; }
                }
                if (obj.isInsideZone(ZoneId.SIEGE) && !obj.isInSiege()) { continue; }
                targetList.add(obj);
            }
        }
        return targetList.toArray(new L2Character[targetList.size()]);
    }
}
