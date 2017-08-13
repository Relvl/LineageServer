package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.ShotType;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.func.Env;
import net.sf.l2j.gameserver.templates.StatsSet;

public class L2SkillElemental extends L2Skill {
    private final int[] seeds;
    private final boolean seedAny;

    public L2SkillElemental(StatsSet set) {
        super(set);

        seeds = new int[3];
        seeds[0] = set.getInteger("seed1", 0);
        seeds[1] = set.getInteger("seed2", 0);
        seeds[2] = set.getInteger("seed3", 0);

        seedAny = set.getInteger("seed_any", 0) == 1;
    }

    @Override
    public void useSkill(L2Character activeChar, L2Object[] targets) {
        if (activeChar.isAlikeDead()) { return; }

        boolean sps = activeChar.isChargedShot(ShotType.SPIRITSHOT);
        boolean bsps = activeChar.isChargedShot(ShotType.BLESSED_SPIRITSHOT);

        for (L2Object obj : targets) {
            if (!(obj instanceof L2Character)) { continue; }

            L2Character target = (L2Character) obj;
            if (target.isAlikeDead()) { continue; }

            boolean charged = true;
            if (!seedAny) {
                for (int _seed : seeds) {
                    if (_seed != 0) {
                        L2Effect e = target.getFirstEffect(_seed);
                        if (e == null || !e.getInUse()) {
                            charged = false;
                            break;
                        }
                    }
                }
            }
            else {
                charged = false;
                for (int _seed : seeds) {
                    if (_seed != 0) {
                        L2Effect e = target.getFirstEffect(_seed);
                        if (e != null && e.getInUse()) {
                            charged = true;
                            break;
                        }
                    }
                }
            }

            if (!charged) {
                activeChar.sendMessage("Target is not charged by elements.");
                continue;
            }

            boolean mcrit = Formulas.calcMCrit(activeChar.getMCriticalHit(target, this));
            byte shld = Formulas.calcShldUse(activeChar, target, this);

            int damage = (int) Formulas.calcMagicDam(activeChar, target, this, shld, sps, bsps, mcrit);
            if (damage > 0) {
                target.reduceCurrentHp(damage, activeChar, this);

                // Manage cast break of the target (calculating rate, sending message...)
                Formulas.calcCastBreak(target, damage);

                activeChar.sendDamageMessage(target, damage, false, false, false);
            }

            // activate attacked effects, if any
            target.stopSkillEffects(getId());
            getEffects(activeChar, target, new Env(shld, sps, false, bsps));
        }

        activeChar.setChargedShot(bsps ? ShotType.BLESSED_SPIRITSHOT : ShotType.SPIRITSHOT, isStaticReuse());
    }
}