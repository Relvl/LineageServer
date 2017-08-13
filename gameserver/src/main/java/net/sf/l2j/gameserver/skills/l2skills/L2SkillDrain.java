package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.ShotType;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2CubicInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.skill.ESkillTargetType;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.StatusUpdate;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.templates.StatsSet;

public class L2SkillDrain extends L2Skill {
    private final float absorbPart;
    private final int absorbAbs;

    public L2SkillDrain(StatsSet set) {
        super(set);
        absorbPart = set.getFloat("absorbPart", 0.0f);
        absorbAbs = set.getInteger("absorbAbs", 0);
    }

    @Override
    public void useSkill(L2Character activeChar, L2Object[] targets) {
        if (activeChar.isAlikeDead()) { return; }

        boolean sps = activeChar.isChargedShot(ShotType.SPIRITSHOT);
        boolean bsps = activeChar.isChargedShot(ShotType.BLESSED_SPIRITSHOT);
        boolean isPlayable = activeChar instanceof L2Playable;

        for (L2Object obj : targets) {
            if (!(obj instanceof L2Character)) { continue; }

            L2Character target = (L2Character) obj;
            if (target.isAlikeDead() && getTargetType() != ESkillTargetType.TARGET_CORPSE_MOB) { continue; }

            if (activeChar != target && target.isInvul()) {
                continue; // No effect on invulnerable chars unless they cast it themselves.
            }

            boolean mcrit = Formulas.calcMCrit(activeChar.getMCriticalHit(target, this));
            byte shld = Formulas.calcShldUse(activeChar, target, this);
            int damage = (int) Formulas.calcMagicDam(activeChar, target, this, shld, sps, bsps, mcrit);

            if (damage > 0) {
                int _drain = 0;
                int _cp = (int) target.getCurrentCp();
                int _hp = (int) target.getCurrentHp();

                // Drain system is different for L2Playable and monsters.
                // When playables attack CP of enemies, monsters don't bother about it.
                if (isPlayable && _cp > 0) {
                    if (damage < _cp) { _drain = 0; }
                    else { _drain = damage - _cp; }
                }
                else if (damage > _hp) { _drain = _hp; }
                else { _drain = damage; }

                double hpAdd = absorbAbs + absorbPart * _drain;
                if (hpAdd > 0) {
                    double hp = (activeChar.getCurrentHp() + hpAdd) > activeChar.getMaxHp() ? activeChar.getMaxHp() : activeChar.getCurrentHp() + hpAdd;

                    activeChar.setCurrentHp(hp);

                    StatusUpdate suhp = new StatusUpdate(activeChar);
                    suhp.addAttribute(StatusUpdate.CUR_HP, (int) hp);
                    activeChar.sendPacket(suhp);
                }

                // That section is launched for drain skills made on ALIVE targets.
                if (!target.isDead() || getTargetType() != ESkillTargetType.TARGET_CORPSE_MOB) {
                    // Manage cast break of the target (calculating rate, sending message...)
                    Formulas.calcCastBreak(target, damage);

                    activeChar.sendDamageMessage(target, damage, mcrit, false, false);

                    if (hasEffects() && getTargetType() != ESkillTargetType.TARGET_CORPSE_MOB) {
                        // ignoring vengance-like reflections
                        if ((Formulas.calcSkillReflect(target, this) & Formulas.SKILL_REFLECT_SUCCEED) > 0) {
                            activeChar.stopSkillEffects(getId());
                            getEffects(target, activeChar);
                            activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(getId()));
                        }
                        else {
                            // activate attacked effects, if any
                            target.stopSkillEffects(getId());
                            if (Formulas.calcSkillSuccess(activeChar, target, this, shld, bsps)) { getEffects(activeChar, target); }
                            else { activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(getId())); }
                        }
                    }
                    target.reduceCurrentHp(damage, activeChar, this);
                }
            }
        }

        if (hasSelfEffects()) {
            L2Effect effect = activeChar.getFirstEffect(getId());
            if (effect != null && effect.isSelfEffect()) { effect.exit(); }

            getEffectsSelf(activeChar);
        }

        activeChar.setChargedShot(bsps ? ShotType.BLESSED_SPIRITSHOT : ShotType.SPIRITSHOT, isStaticReuse());
    }

    public void useCubicSkill(L2CubicInstance activeCubic, L2Object... targets) {
        for (L2Object obj : targets) {
            if (!(obj instanceof L2Character)) { continue; }

            L2Character target = (L2Character) obj;
            if (target.isAlikeDead() && getTargetType() != ESkillTargetType.TARGET_CORPSE_MOB) { continue; }

            boolean mcrit = Formulas.calcMCrit(activeCubic.getMCriticalHit(target, this));
            byte shld = Formulas.calcShldUse(activeCubic.getOwner(), target, this);
            int damage = (int) Formulas.calcMagicDam(activeCubic, target, this, mcrit, shld);

            // Check to see if we should damage the target
            if (damage > 0) {
                L2PcInstance owner = activeCubic.getOwner();
                double hpAdd = absorbAbs + absorbPart * damage;
                if (hpAdd > 0) {
                    double hp = (owner.getCurrentHp() + hpAdd) > owner.getMaxHp() ? owner.getMaxHp() : owner.getCurrentHp() + hpAdd;

                    owner.setCurrentHp(hp);

                    StatusUpdate suhp = new StatusUpdate(owner);
                    suhp.addAttribute(StatusUpdate.CUR_HP, (int) hp);
                    owner.sendPacket(suhp);
                }

                // That section is launched for drain skills made on ALIVE targets.
                if (!target.isDead() || getTargetType() != ESkillTargetType.TARGET_CORPSE_MOB) {
                    target.reduceCurrentHp(damage, activeCubic.getOwner(), this);

                    // Manage cast break of the target (calculating rate, sending message...)
                    Formulas.calcCastBreak(target, damage);

                    owner.sendDamageMessage(target, damage, mcrit, false, false);
                }
            }
        }
    }
}