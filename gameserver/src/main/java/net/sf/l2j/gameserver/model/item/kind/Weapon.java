package net.sf.l2j.gameserver.model.item.kind;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.handler.SkillHandler;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.EItemType1;
import net.sf.l2j.gameserver.model.item.EItemType2;
import net.sf.l2j.gameserver.model.item.type.EWeaponType;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.scripting.EventType;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.skills.func.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.conditions.ACondition;
import net.sf.l2j.gameserver.skills.conditions.ConditionGameChance;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Weapon extends Item {
    private final EWeaponType weaponType;

    private final int randomDamage;

    private final int soulShotCount;
    private final int reducedSoulshot;
    private final int reducedSoulshotChance;

    private final int spiritShotCount;

    private final int mpConsume;
    private final int mpConsumeReduce;
    private final int mpConsumeReduceChance;

    private final boolean isMagical;

    private IntIntHolder enchant4Skill; // skill that activates when item is enchanted +4 (for duals)

    private IntIntHolder skillsOnCast;
    private ACondition skillsOnCastCondition;

    private IntIntHolder skillsOnCrit;
    private ACondition skillsOnCritCondition;

    private final int reuseDelay;

    public Weapon(StatsSet set) {
        super(set);
        itemType1 = EItemType1.WEAPON_RING_EARRING_NECKLACE;
        itemType2 = EItemType2.TYPE2_WEAPON;

        weaponType = EWeaponType.valueOf(set.getString("weapon_type", "none").toUpperCase());
        soulShotCount = set.getInteger("soulshots", 0);
        spiritShotCount = set.getInteger("spiritshots", 0);
        randomDamage = set.getInteger("random_damage", 0);
        mpConsume = set.getInteger("mp_consume", 0);
        String[] reduce = set.getString("mp_consume_reduce", "0,0").split(",");
        mpConsumeReduceChance = Integer.parseInt(reduce[0]);
        mpConsumeReduce = Integer.parseInt(reduce[1]);
        reuseDelay = set.getInteger("reuse_delay", 0);
        isMagical = set.getBool("is_magical", false);

        String[] soulshots = set.getString("reduced_soulshot", "").split(",");
        reducedSoulshotChance = (soulshots.length == 2) ? Integer.parseInt(soulshots[0]) : 0;
        reducedSoulshot = (soulshots.length == 2) ? Integer.parseInt(soulshots[1]) : 0;

        String skill = set.getString("enchant4_skill", null);
        if (skill != null) {
            String[] info = skill.split("-");

            if (info.length == 2) {
                int id = 0;
                int level = 0;
                try {
                    id = Integer.parseInt(info[0]);
                    level = Integer.parseInt(info[1]);
                }
                catch (Exception ignored) {
                    LOGGER.info("> Couldnt parse {} in weapon enchant skills! item {}", skill, toString());
                }
                if (id > 0 && level > 0) { enchant4Skill = new IntIntHolder(id, level); }
            }
        }

        skill = set.getString("oncast_skill", null);
        if (skill != null) {
            String[] info = skill.split("-");
            String infochance = set.getString("oncast_chance", null);
            if (info.length == 2) {
                int id = 0;
                int level = 0;
                int chance = 0;
                try {
                    id = Integer.parseInt(info[0]);
                    level = Integer.parseInt(info[1]);
                    if (infochance != null) { chance = Integer.parseInt(infochance); }
                }
                catch (Exception ignored) {
                    LOGGER.info("> Couldnt parse {} in weapon oncast skills! item {}", skill, toString());
                }
                if (id > 0 && level > 0 && chance > 0) {
                    skillsOnCast = new IntIntHolder(id, level);
                    skillsOnCastCondition = new ConditionGameChance(chance);
                }
            }
        }

        skill = set.getString("oncrit_skill", null);
        if (skill != null) {
            String[] info = skill.split("-");
            String infochance = set.getString("oncrit_chance", null);
            if (info.length == 2) {
                int id = 0;
                int level = 0;
                int chance = 0;
                try {
                    id = Integer.parseInt(info[0]);
                    level = Integer.parseInt(info[1]);
                    if (infochance != null) { chance = Integer.parseInt(infochance); }
                }
                catch (Exception ignored) {
                    LOGGER.info("> Couldnt parse {} in weapon oncrit skills! item {}", skill, toString());
                }
                if (id > 0 && level > 0 && chance > 0) {
                    skillsOnCrit = new IntIntHolder(id, level);
                    skillsOnCritCondition = new ConditionGameChance(chance);
                }
            }
        }
    }

    @Override
    public EWeaponType getItemType() {
        return weaponType;
    }

    @Override
    public int getItemMask() {
        return weaponType.mask();
    }

    public int getSoulShotCount() {
        return soulShotCount;
    }

    public int getSpiritShotCount() {
        return spiritShotCount;
    }

    public int getReducedSoulShot() {
        return reducedSoulshot;
    }

    public int getReducedSoulShotChance() {
        return reducedSoulshotChance;
    }

    public int getRandomDamage() {
        return randomDamage;
    }

    public int getReuseDelay() {
        return reuseDelay;
    }

    public boolean isMagical() {
        return isMagical;
    }

    @Override
    public boolean isWeapon() { return true; }

    public int getMpConsume() {
        if (mpConsumeReduceChance > 0 && Rnd.get(100) < mpConsumeReduceChance) { return mpConsumeReduce; }
        return mpConsume;
    }

    public L2Skill getEnchant4Skill() {
        if (enchant4Skill == null) { return null; }
        return enchant4Skill.getSkill();
    }

    public List<L2Effect> getSkillEffects(L2Character caster, L2Character target, boolean crit) {
        if (skillsOnCrit == null || !crit) { return Collections.emptyList(); }

        List<L2Effect> effects = new ArrayList<>();

        if (skillsOnCritCondition != null) {
            Env env = new Env();
            env.setCharacter(caster);
            env.setTarget(target);
            env.setSkill(skillsOnCrit.getSkill());

            if (!skillsOnCritCondition.test(env)) { return Collections.emptyList(); }
        }

        byte shld = Formulas.calcShldUse(caster, target, skillsOnCrit.getSkill());
        if (!Formulas.calcSkillSuccess(caster, target, skillsOnCrit.getSkill(), shld, false)) { return Collections.emptyList(); }

        if (target.getFirstEffect(skillsOnCrit.getSkill().getId()) != null) { target.getFirstEffect(skillsOnCrit.getSkill().getId()).exit(); }

        for (L2Effect e : skillsOnCrit.getSkill().getEffects(caster, target, new Env(shld, false, false, false))) { effects.add(e); }

        return effects;
    }

    public List<L2Effect> getSkillEffects(L2Character caster, L2Character target, L2Skill trigger) {
        if (skillsOnCast == null) { return Collections.emptyList(); }

        // Trigger only same type of skill.
        if (trigger.isOffensive() != skillsOnCast.getSkill().isOffensive()) { return Collections.emptyList(); }

        // No buffing with toggle or not magic skills.
        if ((trigger.isToggle() || !trigger.isMagic()) && skillsOnCast.getSkill().getSkillType() == L2SkillType.BUFF) { return Collections.emptyList(); }

        if (skillsOnCastCondition != null) {
            Env env = new Env();
            env.setCharacter(caster);
            env.setTarget(target);
            env.setSkill(skillsOnCast.getSkill());

            if (!skillsOnCastCondition.test(env)) { return Collections.emptyList(); }
        }

        byte shld = Formulas.calcShldUse(caster, target, skillsOnCast.getSkill());
        if (skillsOnCast.getSkill().isOffensive() && !Formulas.calcSkillSuccess(caster, target, skillsOnCast.getSkill(), shld, false)) { return Collections.emptyList(); }

        // Get the skill handler corresponding to the skill type
        ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(skillsOnCast.getSkill().getSkillType());

        L2Character[] targets = new L2Character[1];
        targets[0] = target;

        // Launch the magic skill and calculate its effects
        if (handler != null) { handler.useSkill(caster, skillsOnCast.getSkill(), targets); }
        else { skillsOnCast.getSkill().useSkill(caster, targets); }

        // notify quests of a skill use
        if (caster instanceof L2PcInstance) {
            // Mobs in range 1000 see spell
            for (L2Npc npcMob : caster.getKnownList().getKnownTypeInRadius(L2Npc.class, 1000)) {
                List<Quest> quests = npcMob.getTemplate().getEventQuests(EventType.ON_SKILL_SEE);
                if (quests != null) { for (Quest quest : quests) { quest.notifySkillSee(npcMob, (L2PcInstance) caster, skillsOnCast.getSkill(), targets, false); } }
            }
        }
        return Collections.emptyList();
    }
}