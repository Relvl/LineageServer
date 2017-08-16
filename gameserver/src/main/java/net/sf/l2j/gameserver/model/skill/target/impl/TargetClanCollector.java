package net.sf.l2j.gameserver.model.skill.target.impl;

import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ClanMember;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.skill.target.ISkillTargetCollector;
import net.sf.l2j.gameserver.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Цель скилла - сам, члены своего клана в радиусе действия скилла. А так же их питомцы и хозяева.
 * Во время олимпиады - только сам.
 * Во время дуэли - только сам и члены клана в своей группе.
 * Для NPC - все члены своего "клана" в радиусе каста скилла.
 * Игнорируются мертвые цели.
 *
 * @author Johnson / 24.07.2017
 */
public class TargetClanCollector implements ISkillTargetCollector {
    @Override
    public L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill) {
        List<L2Character> targetList = new ArrayList<>();
        if (activeChar instanceof L2Playable) {
            L2PcInstance player = activeChar.getActingPlayer();
            if (player == null) { return EMPTY_TARGET_LIST; }
            if (onlyFirst || player.isInOlympiadMode()) { return new L2Character[]{ activeChar }; }
            targetList.add(player);
            if (canAddSummon(activeChar, player, skill.getSkillRadius(), false)) { targetList.add(player.getPet()); }
            L2Clan clan = player.getClan();
            if (clan != null) {
                for (L2ClanMember member : clan.getMembers()) {
                    L2PcInstance obj = member.getPlayerInstance();
                    if (obj == null || obj == player) { continue; }
                    if (player.isInDuel()) {
                        if (player.getDuelId() != obj.getDuelId()) { continue; }
                        if (player.isInParty() && obj.isInParty() && player.getParty().getPartyLeaderOID() != obj.getParty().getPartyLeaderOID()) { continue; }
                    }
                    if (!player.checkPvpSkill(obj, skill)) { continue; }
                    if (canAddSummon(activeChar, obj, skill.getSkillRadius(), false)) { targetList.add(obj.getPet()); }
                    if (!canAddCharacter(activeChar, obj, skill.getSkillRadius(), false)) { continue; }
                    targetList.add(obj);
                }
            }
        }
        else if (activeChar instanceof L2Npc) {
            targetList.add(activeChar);
            for (L2Npc newTarget : activeChar.getKnownList().getKnownTypeInRadius(L2Npc.class, skill.getCastRange())) {
                if (newTarget.isDead() || !Util.contains(((L2Npc) activeChar).getClans(), newTarget.getClans())) { continue; }
                targetList.add(newTarget);
            }
        }
        return targetList.toArray(new L2Character[targetList.size()]);
    }
}
