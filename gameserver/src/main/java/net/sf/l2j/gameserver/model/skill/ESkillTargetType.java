package net.sf.l2j.gameserver.model.skill;

/**
 * @author Johnson / 18.07.2017
 */
public enum ESkillTargetType {
    TARGET_NONE(false),
    TARGET_SELF(false),
    TARGET_ONE(false),
    TARGET_PARTY(false),
    TARGET_ALLY(false),
    TARGET_CLAN(false),
    TARGET_PET(false),
    TARGET_AREA(true),
    TARGET_FRONT_AREA(true),
    TARGET_BEHIND_AREA(true),
    TARGET_AURA(true),
    TARGET_FRONT_AURA(true),
    TARGET_BEHIND_AURA(true),
    TARGET_CORPSE(false),
    TARGET_UNDEAD(false),
    TARGET_AURA_UNDEAD(false),
    TARGET_CORPSE_ALLY(false),
    TARGET_CORPSE_PLAYER(false),
    TARGET_CORPSE_PET(false),
    TARGET_AREA_CORPSE_MOB(false),
    TARGET_CORPSE_MOB(false),
    TARGET_UNLOCKABLE(false),
    TARGET_HOLY(false),
    TARGET_PARTY_MEMBER(false),
    TARGET_PARTY_OTHER(false),
    TARGET_SUMMON(false),
    TARGET_AREA_SUMMON(false),
    TARGET_ENEMY_SUMMON(false),
    TARGET_OWNER_PET(false),
    TARGET_GROUND(false);

    private final boolean isAoeSkill;

    ESkillTargetType(boolean isAoeSkill) {this.isAoeSkill = isAoeSkill;}

    public boolean isAoeSkill() {
        return isAoeSkill;
    }
}
