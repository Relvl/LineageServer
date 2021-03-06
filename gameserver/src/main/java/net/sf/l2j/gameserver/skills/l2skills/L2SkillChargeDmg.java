package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.ShotType;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.func.Env;
import net.sf.l2j.gameserver.templates.StatsSet;

public class L2SkillChargeDmg extends L2Skill {
    public L2SkillChargeDmg(StatsSet set) {
        super(set);
    }

    @Override
    public void useSkill(L2Character caster, L2Object[] targets) {
        if (caster.isAlikeDead()) { return; }

        double modifier = 0;

        if (caster instanceof L2PcInstance) {
            modifier = 0.7 + 0.3 * (((L2PcInstance) caster).getCharges() + getNumCharges());
        }

        boolean ss = caster.isChargedShot(ShotType.SOULSHOT);

        for (L2Object obj : targets) {
            if (!(obj instanceof L2Character)) { continue; }

            L2Character target = (L2Character) obj;
            if (target.isAlikeDead()) { continue; }

            boolean skillIsEvaded = Formulas.calcPhysicalSkillEvasion(target, this);
            if (skillIsEvaded) {
                if (caster.isPlayer()) { caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DODGES_ATTACK).addCharName(target)); }
                if (target.isPlayer()) { target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.AVOIDED_S1_ATTACK).addCharName(caster)); }
                continue;
            }

            byte shld = Formulas.calcShldUse(caster, target, this);
            boolean crit = false;

            if (getBaseCritRate() > 0) {
                crit = Formulas.calcCrit(getBaseCritRate() * 10 * Formulas.getSTRBonus(caster));
            }

            // damage calculation, crit is static 2x
            double damage = Formulas.calcPhysDam(caster, target, this, shld, false, ss);
            if (crit) { damage *= 2; }

            if (damage > 0) {
                byte reflect = Formulas.calcSkillReflect(target, this);
                if (hasEffects()) {
                    if ((reflect & Formulas.SKILL_REFLECT_SUCCEED) == 0) {
                        target.stopSkillEffects(getId());
                        if (Formulas.calcSkillSuccess(caster, target, this, shld, true)) {
                            getEffects(caster, target, new Env(shld, false, false, false));
                            target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(this));
                        }
                        else {
                            caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(this));
                        }
                    }
                    else {
                        caster.stopSkillEffects(getId());
                        getEffects(target, caster);
                        caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(this));
                    }
                }

                double finalDamage = damage * modifier;
                target.reduceCurrentHp(finalDamage, caster, this);

                if ((reflect & Formulas.SKILL_REFLECT_VENGEANCE) != 0) {
                    caster.reduceCurrentHp(damage, target, this);
                }
                caster.sendDamageMessage(target, (int) finalDamage, false, crit, false);
            }
            else {
                caster.sendDamageMessage(target, 0, false, false, true);
            }
        }

        if (hasSelfEffects()) {
            L2Effect effect = caster.getFirstEffect(getId());
            if (effect != null && effect.isSelfEffect()) {
                effect.exit();
            }
            getEffectsSelf(caster);
        }

        caster.setChargedShot(ShotType.SOULSHOT, isStaticReuse());
    }
}