package net.sf.l2j.gameserver.model.skill;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.skill.target.ISkillTargetCollector;
import net.sf.l2j.gameserver.model.skill.target.impl.*;

public enum ESkillTargetType {
    TARGET_NONE(false, null),
    TARGET_ONE(false, new TargetOneCollector()),
    TARGET_SELF(false, new TargetSelfCollector()),
    TARGET_GROUND(false, TARGET_SELF.collector),
    TARGET_HOLY(false, new TargetHolyArtefactCollector()),
    TARGET_SUMMON(false, new TargetSummonCollector()),
    TARGET_PET(false, new TargetPetCollector()),
    TARGET_OWNER_PET(false, new TargetPetOwnerCollector()),
    TARGET_CORPSE_PET(false, new TargetPetCorpseCollector()),
    TARGET_AURA(true, new TargetAuraCollector()),
    TARGET_FRONT_AURA(true, TARGET_AURA.collector),
    TARGET_BEHIND_AURA(true, TARGET_AURA.collector),
    TARGET_AREA_SUMMON(false, new TargetAreaSummonCollector()),
    TARGET_AREA(true, new TargetAreaCollector()),
    TARGET_FRONT_AREA(true, TARGET_AREA.collector),
    TARGET_BEHIND_AREA(true, TARGET_AREA.collector),
    TARGET_PARTY(false, new TargetPartyCollector()),
    TARGET_PARTY_MEMBER(false, new TargetPartyMemberCollector()),
    TARGET_PARTY_OTHER(false, new TargetPartyOtherMemberCollector()),
    TARGET_ALLY(false, new TargetAllyCollector()),
    TARGET_CORPSE_ALLY(false, new TargetAllyCorpseCollector()),
    TARGET_CLAN(false, new TargetClanCollector()),
    TARGET_CORPSE_PLAYER(false, new TargetCorpsePlayerCollector()),
    TARGET_CORPSE_MOB(false, new TargetCorpseMobCollector()),
    TARGET_UNDEAD(false, new TargetRaceUndeadCollector()),
    TARGET_AURA_UNDEAD(false, new TargetAuraRaceUndeadCollector()),
    TARGET_AREA_CORPSE_MOB(false, new TargetAreaCorpseMobCollector()),
    TARGET_UNLOCKABLE(false, new TargetUnlockableCollector()),
    TARGET_ENEMY_SUMMON(false, new TargetSummonEnemyCollector());

    private final boolean isAoeSkill;
    private final ISkillTargetCollector collector;

    ESkillTargetType(boolean isAoeSkill, ISkillTargetCollector collector) {
        this.isAoeSkill = isAoeSkill;
        this.collector = collector;
    }

    public boolean isAoeSkill() { return isAoeSkill; }

    public L2Object[] getTargetList(L2Character activeChar, L2Skill skill) {
        return getTargetList(activeChar, false, skill);
    }

    public L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst, L2Skill skill) {
        L2Character target = null;
        L2Object objTarget = activeChar.getTarget();
        if (objTarget instanceof L2Character) {
            target = (L2Character) objTarget;
        }
        return getTargetList(activeChar, onlyFirst, target, skill);
    }

    public L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill) {
        if (collector == null) {
            // TODO log
            return ISkillTargetCollector.EMPTY_TARGET_LIST;
        }
        return collector.getTargetList(activeChar, onlyFirst, target, skill);
    }
}
