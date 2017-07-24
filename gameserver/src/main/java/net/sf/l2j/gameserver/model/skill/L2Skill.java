package net.sf.l2j.gameserver.model.skill;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.geoengine.PathFinding;
import net.sf.l2j.gameserver.model.*;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2CubicInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeFlagInstance;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.kind.Armor;
import net.sf.l2j.gameserver.model.item.type.ArmorType;
import net.sf.l2j.gameserver.model.item.type.EWeaponType;
import net.sf.l2j.gameserver.model.skill.chance.ChanceCondition;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.basefuncs.Func;
import net.sf.l2j.gameserver.skills.basefuncs.FuncTemplate;
import net.sf.l2j.gameserver.skills.conditions.Condition;
import net.sf.l2j.gameserver.skills.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

public abstract class L2Skill implements IChanceSkillTrigger {
    protected static final Logger LOGGER = LoggerFactory.getLogger(L2Skill.class);

    // conditional values
    public static final int COND_BEHIND = 0x0008;
    public static final int COND_CRIT = 0x0010;

    private final int id;
    private final int level;
    private final String name;
    private final ESkillOpType opType;

    private final boolean _magic;

    private final int _mpConsume;
    private final int _mpInitialConsume;
    private final int _hpConsume;

    private final int _targetConsume;
    private final int _targetConsumeId;

    private final int _itemConsume;
    private final int _itemConsumeId;

    private final int _castRange;
    private final int _effectRange;

    private final int _abnormalLvl;
    private final int _effectAbnormalLvl;

    private final int _hitTime;
    private final int _coolTime;

    private final int _reuseDelay;
    private final int _equipDelay;

    private final int _buffDuration;

    private final ESkillTargetType targetType;

    private final double _power;

    private final int _magicLevel;

    private final int _negateLvl;
    private final int[] _negateId;
    private final L2SkillType[] _negateStats;
    private final int _maxNegatedEffects;

    private final int _levelDepend;

    private final int _skillRadius;

    private final L2SkillType skillType;
    private final L2SkillType _effectType;

    private final int _effectId;
    private final int _effectPower;
    private final int _effectLvl;

    private final boolean _ispotion;
    private final byte _element;

    private final boolean _ignoreResists;

    private final boolean _staticReuse;
    private final boolean _staticHitTime;

    private final int _reuseHashCode;

    private final Stats _stat;

    private final int _condition;
    private final int _conditionValue;

    private final boolean _overhit;
    private final boolean _killByDOT;
    private final boolean _isSuicideAttack;

    private final boolean _isDemonicSkill;
    private final boolean _isFlyingSkill;
    private final boolean _isStriderSkill;

    private final boolean _isSiegeSummonSkill;

    private final int _weaponsAllowed;

    private final boolean _nextActionIsAttack;

    private final int _minPledgeClass;

    private final boolean _isOffensive;
    private final int _maxCharges;
    private final int _numCharges;

    private final int _triggeredId;
    private final int _triggeredLevel;
    protected ChanceCondition _chanceCondition;
    private final String _chanceType;

    private final String _flyType;
    private final int _flyRadius;
    private final float _flyCourse;

    private final int _feed;

    private final boolean _isHeroSkill;

    private final int _baseCritRate; // percent of success for skill critical hit (especially for PDAM & BLOW - they're not affected by rCrit values or buffs). Default loads -1 for all other skills but 0 to PDAM & BLOW
    private final int _lethalEffect1; // percent of success for lethal 1st effect (hit cp to 1 or if mob hp to 50%) (only for PDAM skills)
    private final int _lethalEffect2; // percent of success for lethal 2nd effect (hit cp,hp to 1 or if mob hp to 1) (only for PDAM skills)
    private final boolean _directHpDmg; // If true then dmg is being make directly
    private final boolean _isDance; // If true then casting more dances will cost more MP
    private final int _nextDanceCost;
    private final float _sSBoost; // If true skill will have SoulShot boost (power*2)
    private final int _aggroPoints;

    protected List<Condition> _preCondition;
    protected List<Condition> _itemPreCondition;
    protected List<FuncTemplate> _funcTemplates;
    public List<EffectTemplate> _effectTemplates;
    protected List<EffectTemplate> _effectTemplatesSelf;

    private final String _attribute;

    private final boolean _isDebuff;
    private final boolean _stayAfterDeath; // skill should stay after death

    private final boolean _removedOnAnyActionExceptMove;
    private final boolean _removedOnDamage;

    private final boolean _canBeReflected;
    private final boolean _canBeDispeled;

    private final boolean _isClanSkill;

    private final boolean _ignoreShield;

    private final boolean _simultaneousCast;

    private L2ExtractableSkill _extractableItems;

    protected L2Skill(StatsSet set) {
        id = set.getInteger("skill_id");
        level = set.getInteger("level");

        name = set.getString("name");
        opType = set.getEnum("operateType", ESkillOpType.class);

        _magic = set.getBool("isMagic", false);
        _ispotion = set.getBool("isPotion", false);

        _mpConsume = set.getInteger("mpConsume", 0);
        _mpInitialConsume = set.getInteger("mpInitialConsume", 0);
        _hpConsume = set.getInteger("hpConsume", 0);

        _targetConsume = set.getInteger("targetConsumeCount", 0);
        _targetConsumeId = set.getInteger("targetConsumeId", 0);

        _itemConsume = set.getInteger("itemConsumeCount", 0);
        _itemConsumeId = set.getInteger("itemConsumeId", 0);

        _castRange = set.getInteger("castRange", 0);
        _effectRange = set.getInteger("effectRange", -1);

        _abnormalLvl = set.getInteger("abnormalLvl", -1);
        _effectAbnormalLvl = set.getInteger("effectAbnormalLvl", -1); // support for a separate effect abnormal lvl, e.g. poison inside a different skill
        _negateLvl = set.getInteger("negateLvl", -1);

        _hitTime = set.getInteger("hitTime", 0);
        _coolTime = set.getInteger("coolTime", 0);

        _reuseDelay = set.getInteger("reuseDelay", 0);
        _equipDelay = set.getInteger("equipDelay", 0);

        _buffDuration = set.getInteger("buffDuration", 0);

        _skillRadius = set.getInteger("skillRadius", 80);

        targetType = set.getEnum("target", ESkillTargetType.class);

        _power = set.getFloat("power", 0.f);

        _attribute = set.getString("attribute", "");
        String str = set.getString("negateStats", "");

        if (str.isEmpty()) { _negateStats = new L2SkillType[0]; }
        else {
            String[] stats = str.split(" ");
            L2SkillType[] array = new L2SkillType[stats.length];

            for (int i = 0; i < stats.length; i++) {
                L2SkillType type;
                try {
                    type = Enum.valueOf(L2SkillType.class, stats[i]);
                }
                catch (Exception e) {
                    throw new IllegalArgumentException("SkillId: " + id + "Enum value of type " + L2SkillType.class.getName() + " required, but found: " + stats[i]);
                }

                array[i] = type;
            }
            _negateStats = array;
        }

        String negateId = set.getString("negateId", null);
        if (negateId != null) {
            String[] valuesSplit = negateId.split(",");
            _negateId = new int[valuesSplit.length];
            for (int i = 0; i < valuesSplit.length; i++) {
                _negateId[i] = Integer.parseInt(valuesSplit[i]);
            }
        }
        else { _negateId = new int[0]; }

        _maxNegatedEffects = set.getInteger("maxNegated", 0);

        _magicLevel = set.getInteger("magicLvl", SkillTreeTable.getInstance().getMinSkillLevel(id, level));
        _levelDepend = set.getInteger("lvlDepend", 0);
        _ignoreResists = set.getBool("ignoreResists", false);

        _staticReuse = set.getBool("staticReuse", false);
        _staticHitTime = set.getBool("staticHitTime", false);

        String reuseHash = set.getString("sharedReuse", null);
        if (reuseHash != null) {
            try {
                String[] valuesSplit = reuseHash.split("-");
                _reuseHashCode = SkillTable.getSkillHashCode(Integer.parseInt(valuesSplit[0]), Integer.parseInt(valuesSplit[1]));
            }
            catch (Exception e) {
                throw new IllegalArgumentException("SkillId: " + id + " invalid sharedReuse value: " + reuseHash + ", \"skillId-skillLvl\" required");
            }
        }
        else { _reuseHashCode = SkillTable.getSkillHashCode(id, level); }

        _stat = set.getEnum("stat", Stats.class, null);
        _ignoreShield = set.getBool("ignoreShld", false);

        skillType = set.getEnum("skillType", L2SkillType.class);
        _effectType = set.getEnum("effectType", L2SkillType.class, null);

        _effectId = set.getInteger("effectId", 0);
        _effectPower = set.getInteger("effectPower", 0);
        _effectLvl = set.getInteger("effectLevel", 0);

        _element = set.getByte("element", (byte) -1);

        _condition = set.getInteger("condition", 0);
        _conditionValue = set.getInteger("conditionValue", 0);

        _overhit = set.getBool("overHit", false);
        _killByDOT = set.getBool("killByDOT", false);
        _isSuicideAttack = set.getBool("isSuicideAttack", false);

        _isDemonicSkill = set.getBool("isDemonicSkill", false);
        _isFlyingSkill = set.getBool("isFlyingSkill", false);
        _isStriderSkill = set.getBool("isStriderSkill", false);

        _isSiegeSummonSkill = set.getBool("isSiegeSummonSkill", false);

        String weaponsAllowedString = set.getString("weaponsAllowed", null);
        if (weaponsAllowedString != null) {
            int mask = 0;
            StringTokenizer st = new StringTokenizer(weaponsAllowedString, ",");
            while (st.hasMoreTokens()) {
                int old = mask;
                String item = st.nextToken();
                for (EWeaponType wt : EWeaponType.values()) {
                    if (wt.name().equals(item)) {
                        mask |= wt.mask();
                        break;
                    }
                }

                for (ArmorType at : ArmorType.values()) {
                    if (at.name().equals(item)) {
                        mask |= at.mask();
                        break;
                    }
                }

                if (old == mask) { LOGGER.info("[weaponsAllowed] Unknown item type name: " + item); }
            }
            _weaponsAllowed = mask;
        }
        else { _weaponsAllowed = 0; }

        _nextActionIsAttack = set.getBool("nextActionAttack", false);

        _minPledgeClass = set.getInteger("minPledgeClass", 0);

        _triggeredId = set.getInteger("triggeredId", 0);
        _triggeredLevel = set.getInteger("triggeredLevel", 0);
        _chanceType = set.getString("chanceType", "");
        if (!_chanceType.isEmpty() && !_chanceType.isEmpty()) { _chanceCondition = ChanceCondition.parse(set); }

        _isOffensive = set.getBool("offensive", isSkillTypeOffensive());
        _maxCharges = set.getInteger("maxCharges", 0);
        _numCharges = set.getInteger("numCharges", 0);

        _isHeroSkill = SkillTable.isHeroSkill(id);

        _baseCritRate = set.getInteger("baseCritRate", (skillType == L2SkillType.PDAM || skillType == L2SkillType.BLOW) ? 0 : -1);
        _lethalEffect1 = set.getInteger("lethal1", 0);
        _lethalEffect2 = set.getInteger("lethal2", 0);

        _directHpDmg = set.getBool("dmgDirectlyToHp", false);
        _isDance = set.getBool("isDance", false);
        _nextDanceCost = set.getInteger("nextDanceCost", 0);
        _sSBoost = set.getFloat("SSBoost", 0.f);
        _aggroPoints = set.getInteger("aggroPoints", 0);

        _isDebuff = set.getBool("isDebuff", false);
        _stayAfterDeath = set.getBool("stayAfterDeath", false);

        _removedOnAnyActionExceptMove = set.getBool("removedOnAnyActionExceptMove", false);
        _removedOnDamage = set.getBool("removedOnDamage", skillType == L2SkillType.SLEEP);

        _flyType = set.getString("flyType", null);
        _flyRadius = set.getInteger("flyRadius", 0);
        _flyCourse = set.getFloat("flyCourse", 0);

        _feed = set.getInteger("feed", 0);

        _canBeReflected = set.getBool("canBeReflected", true);
        _canBeDispeled = set.getBool("canBeDispeled", true);

        _isClanSkill = set.getBool("isClanSkill", false);

        _simultaneousCast = set.getBool("simultaneousCast", false);

        String capsuled_items = set.getString("capsuled_items_skill", null);
        if (capsuled_items != null) {
            if (capsuled_items.isEmpty()) { LOGGER.warn("Empty extractable data for skill: {}", id); }

            _extractableItems = parseExtractableSkill(id, level, capsuled_items);
        }
    }

    public abstract void useSkill(L2Character caster, L2Object[] targets);

    public final boolean isPotion() { return _ispotion; }

    public final L2SkillType getSkillType() { return skillType; }

    public final byte getElement() { return _element; }

    public final ESkillTargetType getTargetType() { return targetType; }

    public final int getCondition() { return _condition; }

    public final boolean isOverhit() { return _overhit; }

    public final boolean isKillingByDOT() { return _killByDOT; }

    public final boolean isSuicideAttack() { return _isSuicideAttack; }

    public final boolean isDemonicSkill() { return _isDemonicSkill; }

    public final boolean isFlyingSkill() { return _isFlyingSkill; }

    public final boolean isStriderSkill() { return _isStriderSkill; }

    public final boolean isSiegeSummonSkill() { return _isSiegeSummonSkill; }

    public final double getPower(L2Character activeChar) {
        if (activeChar == null) { return _power; }
        switch (skillType) {
            case DEATHLINK:
                return _power * Math.pow(1.7165 - activeChar.getCurrentHp() / activeChar.getMaxHp(), 2) * 0.577;
            case FATAL:
                return _power + (_power * Math.pow(1.7165 - activeChar.getCurrentHp() / activeChar.getMaxHp(), 3.5) * 0.577);
            default:
                return _power;
        }
    }

    public final double getPower() { return _power; }

    public final L2SkillType[] getNegateStats() { return _negateStats; }

    public final int getAbnormalLvl() { return _abnormalLvl; }

    public final int getNegateLvl() { return _negateLvl; }

    public final int[] getNegateId() { return _negateId; }

    public final int getMagicLevel() { return _magicLevel; }

    public final int getMaxNegatedEffects() { return _maxNegatedEffects; }

    public final int getLevelDepend() { return _levelDepend; }

    public final boolean isIgnoreResists() { return _ignoreResists; }

    public int getTriggeredId() { return _triggeredId; }

    public int getTriggeredLevel() { return _triggeredLevel; }

    public boolean triggerAnotherSkill() { return _triggeredId > 1; }

    public final boolean isRemovedOnAnyActionExceptMove() { return _removedOnAnyActionExceptMove; }

    public final boolean isRemovedOnDamage() { return _removedOnDamage; }

    public final double getEffectPower() {
        if (_effectTemplates != null) {
            for (EffectTemplate et : _effectTemplates) {
                if (et.effectPower > 0) { return et.effectPower; }
            }
        }

        if (_effectPower > 0) { return _effectPower; }

        switch (skillType) {
            case PDAM:
            case MDAM:
                return 20;
            default:
                return (_power <= 0 || _power > 100) ? 20 : _power;
        }
    }

    public final int getEffectId() { return _effectId; }

    public final int getEffectLvl() { return _effectLvl; }

    public final int getEffectAbnormalLvl() { return _effectAbnormalLvl; }

    public final L2SkillType getEffectType() {
        if (_effectTemplates != null) {
            for (EffectTemplate et : _effectTemplates) {
                if (et.effectType != null) { return et.effectType; }
            }
        }

        if (_effectType != null) { return _effectType; }

        switch (skillType) {
            case PDAM:
                return L2SkillType.STUN;
            case MDAM:
                return L2SkillType.PARALYZE;
            default:
                return skillType;
        }
    }

    public final boolean nextActionIsAttack() { return _nextActionIsAttack; }

    public final int getBuffDuration() { return _buffDuration; }

    public final int getCastRange() { return _castRange; }

    public final int getEffectRange() { return _effectRange; }

    public final int getHpConsume() { return _hpConsume; }

    public final boolean isDebuff() { return _isDebuff; }

    public final int getId() { return id; }

    public final Stats getStat() { return _stat; }

    public final int getTargetConsumeId() { return _targetConsumeId; }

    public final int getTargetConsume() { return _targetConsume; }

    public final int getItemConsume() { return _itemConsume; }

    public final int getItemConsumeId() { return _itemConsumeId; }

    public final int getLevel() { return level; }

    public final boolean isMagic() { return _magic; }

    public final boolean isStaticReuse() { return _staticReuse; }

    public final boolean isStaticHitTime() { return _staticHitTime; }

    public final int getMpConsume() { return _mpConsume; }

    public final int getMpInitialConsume() { return _mpInitialConsume; }

    public final String getName() { return name; }

    public final int getReuseDelay() { return _reuseDelay; }

    public final int getEquipDelay() { return _equipDelay; }

    public final int getReuseHashCode() { return _reuseHashCode; }

    public final int getHitTime() { return _hitTime; }

    public final int getCoolTime() { return _coolTime; }

    public final int getSkillRadius() { return _skillRadius; }

    public final boolean isActive() { return opType == ESkillOpType.OP_ACTIVE; }

    public final boolean isPassive() { return opType == ESkillOpType.OP_PASSIVE; }

    public final boolean isToggle() { return opType == ESkillOpType.OP_TOGGLE; }

    public boolean isChance() { return _chanceCondition != null && isPassive(); }

    public final boolean isDance() { return _isDance; }

    public final int getNextDanceMpCost() { return _nextDanceCost; }

    public final float getSSBoost() { return _sSBoost; }

    public final int getAggroPoints() { return _aggroPoints; }

    public final boolean isUsingSoulshot() {
        switch (skillType) {
            case BLOW:
            case PDAM:
            case STUN:
            case CHARGEDAM:
                return true;
            default:
                return false;
        }
    }

    public final boolean isUsingSpiritshot() { return isMagic(); }

    public final int getWeaponsAllowed() { return _weaponsAllowed; }

    public boolean isSimultaneousCast() { return _simultaneousCast; }

    public int getMinPledgeClass() { return _minPledgeClass; }

    public String getAttributeName() { return _attribute; }

    public boolean isIgnoresShield() { return _ignoreShield; }

    public boolean canBeReflected() { return _canBeReflected; }

    public boolean canBeDispeled() { return _canBeDispeled; }

    public boolean isClanSkill() { return _isClanSkill; }

    public final String getFlyType() { return _flyType; }

    public final int getFlyRadius() { return _flyRadius; }

    public int getFeed() { return _feed; }

    public final float getFlyCourse() { return _flyCourse; }

    public final int getMaxCharges() { return _maxCharges; }

    @Override
    public boolean triggersChanceSkill() { return _triggeredId > 0 && isChance(); }

    @Override
    public int getTriggeredChanceId() { return _triggeredId; }

    @Override
    public int getTriggeredChanceLevel() { return _triggeredLevel; }

    @Override
    public ChanceCondition getTriggeredChanceCondition() { return _chanceCondition; }

    public final boolean isPvpSkill() {
        switch (skillType) {
            case DOT:
            case BLEED:
            case POISON:
            case DEBUFF:
            case AGGDEBUFF:
            case STUN:
            case ROOT:
            case FEAR:
            case SLEEP:
            case MDOT:
            case MUTE:
            case WEAKNESS:
            case PARALYZE:
            case CANCEL:
            case MAGE_BANE:
            case WARRIOR_BANE:
            case BETRAY:
            case AGGDAMAGE:
            case AGGREDUCE_CHAR:
            case MANADAM:
                return true;
            default:
                return false;
        }
    }

    /** @deprecated Просто потому что некрасивый хардкодище. */
    @Deprecated
    public final boolean is7Signs() { return id > 4360 && id < 4367; }

    public final boolean isStayAfterDeath() { return _stayAfterDeath; }

    public final boolean isOffensive() { return _isOffensive; }

    public final boolean isHeroSkill() { return _isHeroSkill; }

    public final int getNumCharges() { return _numCharges; }

    public final int getBaseCritRate() { return _baseCritRate; }

    public final int getLethalChance1() { return _lethalEffect1; }

    public final int getLethalChance2() { return _lethalEffect2; }

    public final boolean isDamagedHPDirectly() { return _directHpDmg; }

    public final boolean isSkillTypeOffensive() {
        switch (skillType) {
            case PDAM:
            case MDAM:
            case CPDAMPERCENT:
            case DOT:
            case BLEED:
            case POISON:
            case AGGDAMAGE:
            case DEBUFF:
            case AGGDEBUFF:
            case STUN:
            case ROOT:
            case CONFUSION:
            case ERASE:
            case BLOW:
            case FATAL:
            case FEAR:
            case DRAIN:
            case SLEEP:
            case CHARGEDAM:
            case DEATHLINK:
            case DETECT_WEAKNESS:
            case MANADAM:
            case MDOT:
            case MUTE:
            case SOULSHOT:
            case SPIRITSHOT:
            case SPOIL:
            case WEAKNESS:
            case SWEEP:
            case PARALYZE:
            case DRAIN_SOUL:
            case AGGREDUCE:
            case CANCEL:
            case MAGE_BANE:
            case WARRIOR_BANE:
            case AGGREMOVE:
            case AGGREDUCE_CHAR:
            case BETRAY:
            case DELUXE_KEY_UNLOCK:
            case SOW:
            case HARVEST:
            case INSTANT_JUMP:
                return true;
            default:
                return isDebuff();
        }
    }

    public final boolean getWeaponDependancy(L2Character activeChar) {
        int weaponsAllowed = getWeaponsAllowed();
        if (weaponsAllowed == 0) { return true; }
        int mask = 0;
        if (activeChar.getActiveWeaponItem() != null) { mask |= activeChar.getActiveWeaponItem().getItemType().mask(); }
        if (activeChar.getSecondaryWeaponItem() != null && activeChar.getSecondaryWeaponItem() instanceof Armor) { mask |= activeChar.getSecondaryWeaponItem().getItemType().mask(); }
        if ((mask & weaponsAllowed) != 0) { return true; }
        activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(this));
        return false;
    }

    public boolean checkCondition(L2Character activeChar, L2Object target, boolean itemOrWeapon) {
        List<Condition> preCondition = (itemOrWeapon) ? _itemPreCondition : _preCondition;
        if (preCondition == null || preCondition.isEmpty()) { return true; }

        Env env = new Env();
        env.setCharacter(activeChar);
        if (target instanceof L2Character) { env.setTarget((L2Character) target); }

        env.setSkill(this);

        for (Condition cond : preCondition) {
            if (!cond.test(env)) {
                int msgId = cond.getMessageId();
                if (msgId == 0) {
                    String msg = cond.getMessage();
                    if (msg != null) { activeChar.sendMessage(msg); }
                }
                else {
                    SystemMessage sm = SystemMessage.getSystemMessage(msgId);
                    if (cond.isAddName()) { sm.addSkillName(id); }
                    activeChar.sendPacket(sm);
                }
                return false;
            }
        }
        return true;
    }

    public boolean isSelfLegit() {
        switch (skillType) {
            case BUFF:
            case HEAL:
            case HOT:
            case HEAL_PERCENT:
            case MANARECHARGE:
            case MANAHEAL:
            case NEGATE:
            case CANCEL_DEBUFF:
            case REFLECT:
            case COMBATPOINTHEAL:
            case SEED:
            case BALANCE_LIFE:
                return true;
            default:
                return false;
        }
    }

    public final L2Object getFirstOfTargetList(L2Character activeChar) {
        L2Object[] targets = targetType.getTargetList(activeChar, true, this);
        if (targets.length == 0) { return null; }
        return targets[0];
    }

    public static boolean checkForAreaOffensiveSkills(L2Character caster, L2Character target, L2Skill skill, boolean sourceInArena) {
        if (target == null || target.isDead() || target == caster) { return false; }
        L2PcInstance player = caster.getActingPlayer();
        L2PcInstance targetPlayer = target.getActingPlayer();
        if (player != null && targetPlayer != null) {
            if (targetPlayer == caster || targetPlayer == player) { return false; }
            if (targetPlayer.inObserverMode()) { return false; }
            if (skill.isOffensive() && player.getSiegeState() > 0 && player.isInsideZone(ZoneId.SIEGE) && player.getSiegeState() == targetPlayer.getSiegeState()) { return false; }
            if (target.isInsideZone(ZoneId.PEACE)) { return false; }
            if (player.isInParty() && targetPlayer.isInParty()) {
                if (player.getParty().getPartyLeaderOID() == targetPlayer.getParty().getPartyLeaderOID()) { return false; }
                if (player.getParty().getCommandChannel() != null && player.getParty().getCommandChannel() == targetPlayer.getParty().getCommandChannel()) { return false; }
            }
            if (!sourceInArena && !(targetPlayer.isInsideZone(ZoneId.PVP) && !targetPlayer.isInsideZone(ZoneId.SIEGE))) {
                if (player.getAllyId() != 0 && player.getAllyId() == targetPlayer.getAllyId()) { return false; }
                if (player.getClanId() != 0 && player.getClanId() == targetPlayer.getClanId()) { return false; }
                if (!player.checkPvpSkill(targetPlayer, skill)) { return false; }
            }
        }
        else if (target instanceof L2Attackable) {
            if (caster instanceof L2Attackable && !caster.isConfused()) { return false; }
            if (skill.isOffensive() && !target.isAutoAttackable(caster)) { return false; }
        }
        return PathFinding.getInstance().canSeeTarget(caster, target);
    }

    public final List<Func> getStatFuncs(L2Character player) {
        if (_funcTemplates == null) { return Collections.emptyList(); }
        if (!(player instanceof L2Playable) && !(player instanceof L2Attackable)) { return Collections.emptyList(); }
        List<Func> funcs = new ArrayList<>(_funcTemplates.size());
        Env env = new Env();
        env.setCharacter(player);
        env.setSkill(this);
        for (FuncTemplate t : _funcTemplates) {
            Func func = t.getFunc(env, this); // skill is owner
            if (func != null) { funcs.add(func); }
        }
        return funcs;
    }

    public boolean hasEffects() { return _effectTemplates != null && !_effectTemplates.isEmpty(); }

    public List<EffectTemplate> getEffectTemplates() { return _effectTemplates; }

    public boolean hasSelfEffects() { return _effectTemplatesSelf != null && !_effectTemplatesSelf.isEmpty(); }

    public final List<L2Effect> getEffects(L2Character effector, L2Character effected, Env env) {
        if (!hasEffects() || isPassive()) { return Collections.emptyList(); }
        if (effected instanceof L2DoorInstance || effected instanceof L2SiegeFlagInstance) { return Collections.emptyList(); }
        if (effector != effected) {
            if (isOffensive() || isDebuff()) {
                if (effected.isInvul()) { return Collections.emptyList(); }
                if (effector instanceof L2PcInstance && effector.isGM()) {
                    if (!((L2PcInstance) effector).getAccessLevel().canGiveDamage()) { return Collections.emptyList(); }
                }
            }
        }
        List<L2Effect> effects = new ArrayList<>(_effectTemplates.size());
        if (env == null) { env = new Env(); }
        env.setSkillMastery(Formulas.calcSkillMastery(effector, this));
        env.setCharacter(effector);
        env.setTarget(effected);
        env.setSkill(this);
        for (EffectTemplate et : _effectTemplates) {
            boolean success = true;
            if (et.effectPower > -1) { success = Formulas.calcEffectSuccess(effector, effected, et, this, env.getShield(), env.isBlessedSpiritShot()); }
            if (success) {
                L2Effect e = et.getEffect(env);
                if (e != null) {
                    e.scheduleEffect();
                    effects.add(e);
                }
            }
            else if (et.icon && effector instanceof L2PcInstance) {
                effector.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(effected).addSkillName(this));
            }
        }
        return effects;
    }

    public final List<L2Effect> getEffects(L2Character effector, L2Character effected) {
        return getEffects(effector, effected, null);
    }

    public final List<L2Effect> getEffects(L2CubicInstance effector, L2Character effected, Env env) {
        if (!hasEffects() || isPassive()) { return Collections.emptyList(); }
        if (effector.getOwner() != effected) {
            if (isDebuff() || isOffensive()) {
                if (effected.isInvul()) { return Collections.emptyList(); }
                if (effector.getOwner().isGM() && !effector.getOwner().getAccessLevel().canGiveDamage()) { return Collections.emptyList(); }
            }
        }
        List<L2Effect> effects = new ArrayList<>(_effectTemplates.size());
        if (env == null) { env = new Env(); }
        env.setCharacter(effector.getOwner());
        env.setCubic(effector);
        env.setTarget(effected);
        env.setSkill(this);
        for (EffectTemplate et : _effectTemplates) {
            boolean success = true;
            if (et.effectPower > -1) { success = Formulas.calcEffectSuccess(effector.getOwner(), effected, et, this, env.getShield(), env.isBlessedSpiritShot()); }
            if (success) {
                L2Effect e = et.getEffect(env);
                if (e != null) {
                    e.scheduleEffect();
                    effects.add(e);
                }
            }
        }
        return effects;
    }

    public final List<L2Effect> getEffectsSelf(L2Character effector) {
        if (!hasSelfEffects() || isPassive()) { return Collections.emptyList(); }
        List<L2Effect> effects = new ArrayList<>(_effectTemplatesSelf.size());
        Env env = new Env();
        env.setCharacter(effector);
        env.setTarget(effector);
        env.setSkill(this);
        for (EffectTemplate et : _effectTemplatesSelf) {
            L2Effect e = et.getEffect(env);
            if (e != null) {
                e.setSelfEffect();
                e.scheduleEffect();
                effects.add(e);
            }
        }
        return effects;
    }

    public final void attach(FuncTemplate funcTemplate) {
        if (_funcTemplates == null) { _funcTemplates = new ArrayList<>(1); }
        _funcTemplates.add(funcTemplate);
    }

    public final void attach(EffectTemplate effect) {
        if (_effectTemplates == null) { _effectTemplates = new ArrayList<>(1); }
        _effectTemplates.add(effect);
    }

    public final void attachSelf(EffectTemplate effect) {
        if (_effectTemplatesSelf == null) { _effectTemplatesSelf = new ArrayList<>(1); }
        _effectTemplatesSelf.add(effect);
    }

    public final void attach(Condition condition, boolean itemOrWeapon) {
        if (itemOrWeapon) {
            if (_itemPreCondition == null) { _itemPreCondition = new ArrayList<>(); }
            _itemPreCondition.add(condition);
        }
        else {
            if (_preCondition == null) { _preCondition = new ArrayList<>(); }
            _preCondition.add(condition);
        }
    }

    private L2ExtractableSkill parseExtractableSkill(int skillId, int skillLvl, String values) {
        String[] prodLists = values.split(";");
        List<L2ExtractableProductItem> products = new ArrayList<>();
        for (String prodList : prodLists) {
            String[] prodData = prodList.split(",");
            if (prodData.length < 3) { LOGGER.warn("Extractable skills data: Error in Skill Id: {} Level: {} -> wrong seperator!", skillId, skillLvl); }
            int lenght = prodData.length - 1;
            List<IntIntHolder> items = null;
            double chance = 0;
            try {
                items = new ArrayList<>(lenght / 2);
                for (int j = 0; j < lenght; j++) {
                    int prodId = Integer.parseInt(prodData[j]);
                    int quantity = Integer.parseInt(prodData[j += 1]);
                    if (prodId <= 0 || quantity <= 0) { LOGGER.warn("Extractable skills data: Error in Skill Id: {} Level: {} wrong production Id: {} or wrond quantity: {}!", skillId, skillLvl, prodId, quantity); }
                    items.add(new IntIntHolder(prodId, quantity));
                }
                chance = Double.parseDouble(prodData[lenght]);
            }
            catch (NumberFormatException e) {
                LOGGER.warn("Extractable skills data: Error in Skill Id: {} Level: {} -> incomplete/invalid production data or wrong seperator!", skillId, skillLvl, e);
            }
            products.add(new L2ExtractableProductItem(items, chance));
        }
        if (products.isEmpty()) {
            LOGGER.warn("Extractable skills data: Error in Skill Id: {} Level: {} -> There are no production items!", skillId, skillLvl);
        }
        return new L2ExtractableSkill(SkillTable.getSkillHashCode(this), products);
    }

    public L2ExtractableSkill getExtractableSkill() { return _extractableItems; }

    @Override
    public String toString() { return name + "[id=" + id + ",lvl=" + level + "]"; }

    @Override
    public boolean isSkill() { return true; }
}