package net.sf.l2j.gameserver.model.actor.stat;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.skills.Calculator;
import net.sf.l2j.gameserver.skills.func.Env;
import net.sf.l2j.gameserver.skills.Stats;

public class CharStat {
    private final L2Character activeChar;

    private long exp;
    private int sp;
    private byte level = 1;

    public CharStat(L2Character activeChar) { this.activeChar = activeChar; }

    public final double calcStat(Stats stat, double init, L2Character target, L2Skill skill) {
        if (activeChar == null || stat == null) { return init; }

        int id = stat.ordinal();

        Calculator calculator = activeChar.getCalculators()[id];
        if (calculator == null || calculator.size() == 0) { return init; }

        Env env = new Env();
        env.setCharacter(activeChar);
        env.setTarget(target);
        env.setSkill(skill);
        env.setValue(init);
        calculator.calc(env);

        if (env.getValue() <= 0) {
            switch (stat) {
                case MAX_HP:
                case MAX_MP:
                case MAX_CP:
                case MAGIC_DEFENCE:
                case POWER_DEFENCE:
                case POWER_ATTACK:
                case MAGIC_ATTACK:
                case POWER_ATTACK_SPEED:
                case MAGIC_ATTACK_SPEED:
                case SHIELD_DEFENCE:
                case STAT_CON:
                case STAT_DEX:
                case STAT_INT:
                case STAT_MEN:
                case STAT_STR:
                case STAT_WIT:
                    env.setValue(1);
                default:
                    return env.getValue();
            }
        }
        return env.getValue();
    }

    public final int getSTR() { return (int) calcStat(Stats.STAT_STR, activeChar.getTemplate().getBaseSTR(), null, null); }

    public final int getDEX() { return (int) calcStat(Stats.STAT_DEX, activeChar.getTemplate().getBaseDEX(), null, null); }

    public final int getCON() { return (int) calcStat(Stats.STAT_CON, activeChar.getTemplate().getBaseCON(), null, null); }

    public int getINT() { return (int) calcStat(Stats.STAT_INT, activeChar.getTemplate().getBaseINT(), null, null); }

    public final int getMEN() { return (int) calcStat(Stats.STAT_MEN, activeChar.getTemplate().getBaseMEN(), null, null); }

    public final int getWIT() { return (int) calcStat(Stats.STAT_WIT, activeChar.getTemplate().getBaseWIT(), null, null); }

    public int getCriticalHit(L2Character target, L2Skill skill) { return Math.min((int) calcStat(Stats.CRITICAL_RATE, activeChar.getTemplate().getBaseCritRate(), target, skill), 500); }

    public final int getMCriticalHit(L2Character target, L2Skill skill) { return (int) calcStat(Stats.MCRITICAL_RATE, 8, target, skill); }

    public int getEvasionRate(L2Character target) { return (int) calcStat(Stats.EVASION_RATE, 0, target, null); }

    public int getAccuracy() { return (int) calcStat(Stats.ACCURACY_COMBAT, 0, null, null); }

    public int getMaxHp() { return (int) calcStat(Stats.MAX_HP, activeChar.getTemplate().getBaseHpMax(activeChar.getLevel()), null, null); }

    public int getMaxCp() { return 0; }

    public int getMaxMp() { return (int) calcStat(Stats.MAX_MP, activeChar.getTemplate().getBaseMpMax(activeChar.getLevel()), null, null); }

    public int getMAtk(L2Character target, L2Skill skill) {
        double attack = activeChar.getTemplate().getBaseMAtk() * ((activeChar.isChampion()) ? Config.CHAMPION_ATK : 1);
        if (skill != null) { attack += skill.getPower(); }
        return (int) calcStat(Stats.MAGIC_ATTACK, attack, target, skill);
    }

    public int getMAtkSpd() { return (int) calcStat(Stats.MAGIC_ATTACK_SPEED, 333.0 * ((activeChar.isChampion()) ? Config.CHAMPION_SPD_ATK : 1), null, null); }

    public int getMDef(L2Character target, L2Skill skill) { return (int) calcStat(Stats.MAGIC_DEFENCE, activeChar.getTemplate().getBaseMDef() * ((activeChar.isRaid()) ? Config.RAID_DEFENCE_MULTIPLIER : 1), target, skill); }

    public int getPAtk(L2Character target) { return (int) calcStat(Stats.POWER_ATTACK, activeChar.getTemplate().getBasePAtk() * ((activeChar.isChampion()) ? Config.CHAMPION_ATK : 1), target, null); }

    public int getPAtkSpd() { return (int) calcStat(Stats.POWER_ATTACK_SPEED, activeChar.getTemplate().getBasePAtkSpd() * ((activeChar.isChampion()) ? Config.CHAMPION_SPD_ATK : 1), null, null); }

    public int getPDef(L2Character target) { return (int) calcStat(Stats.POWER_DEFENCE, activeChar.getTemplate().getBasePDef() * ((activeChar.isRaid()) ? Config.RAID_DEFENCE_MULTIPLIER : 1), target, null); }

    public final double getPAtkAnimals(L2Character target) { return calcStat(Stats.PATK_ANIMALS, 1, target, null); }

    public final double getPAtkDragons(L2Character target) { return calcStat(Stats.PATK_DRAGONS, 1, target, null); }

    public final double getPAtkInsects(L2Character target) { return calcStat(Stats.PATK_INSECTS, 1, target, null); }

    public final double getPAtkMonsters(L2Character target) { return calcStat(Stats.PATK_MONSTERS, 1, target, null); }

    public final double getPAtkPlants(L2Character target) { return calcStat(Stats.PATK_PLANTS, 1, target, null); }

    public final double getPAtkGiants(L2Character target) { return calcStat(Stats.PATK_GIANTS, 1, target, null); }

    public final double getPAtkMagicCreatures(L2Character target) { return calcStat(Stats.PATK_MCREATURES, 1, target, null); }

    public final double getPDefAnimals(L2Character target) { return calcStat(Stats.PDEF_ANIMALS, 1, target, null); }

    public final double getPDefDragons(L2Character target) { return calcStat(Stats.PDEF_DRAGONS, 1, target, null); }

    public final double getPDefInsects(L2Character target) { return calcStat(Stats.PDEF_INSECTS, 1, target, null); }

    public final double getPDefMonsters(L2Character target) { return calcStat(Stats.PDEF_MONSTERS, 1, target, null); }

    public final double getPDefPlants(L2Character target) { return calcStat(Stats.PDEF_PLANTS, 1, target, null); }

    public final double getPDefGiants(L2Character target) { return calcStat(Stats.PDEF_GIANTS, 1, target, null); }

    public final double getPDefMagicCreatures(L2Character target) { return calcStat(Stats.PDEF_MCREATURES, 1, target, null); }

    public int getPhysicalAttackRange() { return activeChar.getTemplate().getBaseAtkRange(); }

    public int getWalkSpeed() {
        double baseWalkSpd = activeChar.getTemplate().getBaseWalkSpd();
        if (baseWalkSpd < 1) { return 0; }
        return (int) calcStat(Stats.WALK_SPEED, baseWalkSpd, null, null);
    }

    public int getRunSpeed() { return (int) calcStat(Stats.RUN_SPEED, activeChar.getTemplate().getBaseRunSpd(), null, null); }

    public final int getShldDef() { return (int) calcStat(Stats.SHIELD_DEFENCE, 0, null, null); }

    public final int getMpConsume(L2Skill skill) {
        if (skill == null) { return 1; }
        double mpConsume = skill.getMpConsume();
        if (skill.isDance()) {
            if (activeChar != null && activeChar.getDanceCount() > 0) { mpConsume += activeChar.getDanceCount() * skill.getNextDanceMpCost(); }
        }
        if (skill.isDance()) { return (int) calcStat(Stats.DANCE_MP_CONSUME_RATE, mpConsume, null, null); }
        if (skill.isMagic()) { return (int) calcStat(Stats.MAGICAL_MP_CONSUME_RATE, mpConsume, null, null); }
        return (int) calcStat(Stats.PHYSICAL_MP_CONSUME_RATE, mpConsume, null, null);
    }

    public final int getMpInitialConsume(L2Skill skill) {
        if (skill == null) { return 1; }
        double mpConsume = skill.getMpInitialConsume();
        if (skill.isDance()) { return (int) calcStat(Stats.DANCE_MP_CONSUME_RATE, mpConsume, null, null); }
        if (skill.isMagic()) { return (int) calcStat(Stats.MAGICAL_MP_CONSUME_RATE, mpConsume, null, null); }
        return (int) calcStat(Stats.PHYSICAL_MP_CONSUME_RATE, mpConsume, null, null);
    }

    public int getAttackElementValue(byte attackAttribute) {
        switch (attackAttribute) {
            case 1: // wind
                return (int) calcStat(Stats.WIND_POWER, 0, null, null);
            case 2: // fire
                return (int) calcStat(Stats.FIRE_POWER, 0, null, null);
            case 3: // water
                return (int) calcStat(Stats.WATER_POWER, 0, null, null);
            case 4: // earth
                return (int) calcStat(Stats.EARTH_POWER, 0, null, null);
            case 5: // holy
                return (int) calcStat(Stats.HOLY_POWER, 0, null, null);
            case 6: // dark
                return (int) calcStat(Stats.DARK_POWER, 0, null, null);
            default:
                return 0;
        }
    }

    public int getDefenseElementValue(byte defenseAttribute) {
        switch (defenseAttribute) {
            case 1: // wind
                return (int) calcStat(Stats.WIND_RES, 0, null, null);
            case 2: // fire
                return (int) calcStat(Stats.FIRE_RES, 0, null, null);
            case 3: // water
                return (int) calcStat(Stats.WATER_RES, 0, null, null);
            case 4: // earth
                return (int) calcStat(Stats.EARTH_RES, 0, null, null);
            case 5: // holy
                return (int) calcStat(Stats.HOLY_RES, 0, null, null);
            case 6: // dark
                return (int) calcStat(Stats.DARK_RES, 0, null, null);
            default:
                return 0;
        }
    }

    public float getMovementSpeedMultiplier() { return getRunSpeed() / (float) activeChar.getTemplate().getBaseRunSpd(); }

    public final float getAttackSpeedMultiplier() { return (float) (1.1 * getPAtkSpd() / activeChar.getTemplate().getBasePAtkSpd()); }

    public int getMoveSpeed() { return (activeChar.isRunning()) ? getRunSpeed() : getWalkSpeed(); }

    @Deprecated
    public long getExp() { return exp; }

    @Deprecated
    public void setExp(long value) { exp = value; }

    @Deprecated
    public int getSp() { return sp; }

    @Deprecated
    public void setSp(int value) { sp = value; }

    @Deprecated
    public byte getLevel() { return level; }

    @Deprecated
    public void setLevel(byte value) { level = value; }

    public L2Character getActiveChar() { return activeChar; }
}