package net.sf.l2j.gameserver.model.skill.target.impl;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.skill.target.ISkillTargetCollector;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

/**
 * Цель скилла - мертвый игрок или пэт.
 *
 * @author Johnson / 24.07.2017
 */
public class TargetCorpsePlayerCollector implements ISkillTargetCollector {
    @Override
    public L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill) {
        if (!activeChar.isPlayer()) { return EMPTY_TARGET_LIST; }
        if (target == null || !target.isDead()) { return sendIncorrect(activeChar); }

        L2PcInstance targetPlayer = target.isPlayer() ? (L2PcInstance) target : null;
        L2PetInstance targetPet = target instanceof L2PetInstance ? (L2PetInstance) target : null;

        if (targetPlayer != null || targetPet != null) {
            boolean condGood = true;
            if (skill.getSkillType() == L2SkillType.RESURRECT) {
                L2PcInstance player = (L2PcInstance) activeChar;
                if (targetPlayer != null) {
                    if (targetPlayer.isInsideZone(ZoneId.SIEGE) && !targetPlayer.isInSiege()) {
                        condGood = false;
                        activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE));
                    }
                    if (targetPlayer.isFestivalParticipant()) {
                        condGood = false;
                        activeChar.sendMessage("You may not resurrect participants in a festival.");
                    }
                    if (targetPlayer.isReviveRequested()) {
                        if (targetPlayer.isRevivingPet()) { player.sendPacket(SystemMessageId.MASTER_CANNOT_RES); }
                        else { player.sendPacket(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED); }
                        condGood = false;
                    }
                }
                else {
                    if (targetPet.getOwner() != player) {
                        if (targetPet.getOwner().isReviveRequested()) {
                            if (targetPet.getOwner().isRevivingPet()) { player.sendPacket(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED); }
                            else { player.sendPacket(SystemMessageId.CANNOT_RES_PET2); }
                            condGood = false;
                        }
                    }
                }
            }
            if (condGood) {
                return new L2Character[]{ target };
            }
        }
        return sendIncorrect(activeChar);
    }
}
