package net.sf.l2j.gameserver.model.actor.template;

import net.sf.l2j.gameserver.datatables.HerbDropTable;
import net.sf.l2j.gameserver.model.L2MinionData;
import net.sf.l2j.gameserver.model.L2NpcAIData;
import net.sf.l2j.gameserver.model.actor.NpcRace;
import net.sf.l2j.gameserver.model.actor.instance.L2XmassTreeInstance;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.item.DropCategory;
import net.sf.l2j.gameserver.model.item.DropData;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.scripting.EventType;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.templates.StatsSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class NpcTemplate extends CharTemplate {
    private static final Logger LOGGER = LoggerFactory.getLogger(NpcTemplate.class);

    private final int _npcId;
    private final int _idTemplate;
    private final String _type;
    private final String _name;
    private final String _title;
    private final byte _level;
    private final int _exp;
    private final int _sp;
    private final int _rHand;
    private final int _lHand;
    private final int _enchantEffect;
    private final int _corpseTime;
    private int _dropHerbGroup;
    private NpcRace _race;

    // used for champion option ; avoid to popup champion quest mob.
    private final boolean _cantBeChampionMonster;

    // Skills arrays
    private final List<L2Skill> _buffSkills = new ArrayList<>();
    private final List<L2Skill> _debuffSkills = new ArrayList<>();
    private final List<L2Skill> _healSkills = new ArrayList<>();
    private final List<L2Skill> _longRangeSkills = new ArrayList<>();
    private final List<L2Skill> _shortRangeSkills = new ArrayList<>();
    private final List<L2Skill> _suicideSkills = new ArrayList<>();

    private L2NpcAIData _AIdata = new L2NpcAIData();

    private final List<DropCategory> _categories = new LinkedList<>();
    private final List<L2MinionData> _minions = new ArrayList<>();
    private final List<ClassId> _teachInfo = new ArrayList<>();
    private final Map<Integer, L2Skill> _skills = new HashMap<>();
    private final Map<EventType, List<Quest>> _questEvents = new HashMap<>();

    /**
     * Constructor of L2NpcTemplate.
     *
     * @param set The StatsSet object to transfer data to the method.
     */
    public NpcTemplate(StatsSet set) {
        super(set);

        _npcId = set.getInteger("id");
        _idTemplate = set.getInteger("idTemplate", _npcId);

        _type = set.getString("type");

        _name = set.getString("name");
        _title = set.getString("title", "");

        _cantBeChampionMonster = (_title.equalsIgnoreCase("Quest Monster") || isType("L2Chest")) ? true : false;

        _level = set.getByte("level", (byte) 1);

        _exp = set.getInteger("exp", 0);
        _sp = set.getInteger("sp", 0);

        _rHand = set.getInteger("rHand", 0);
        _lHand = set.getInteger("lHand", 0);

        _enchantEffect = set.getInteger("enchant", 0);
        _corpseTime = set.getInteger("corpseTime", 7);
        _dropHerbGroup = set.getInteger("dropHerbGroup", 0);

        if (_dropHerbGroup > 0 && HerbDropTable.getInstance().getHerbDroplist(_dropHerbGroup) == null) {
            LOGGER.warn("Missing dropHerbGroup information for npcId: {}, dropHerbGroup: {}", _npcId, _dropHerbGroup);
            _dropHerbGroup = 0;
        }
    }

    public void addTeachInfo(ClassId classId) {
        _teachInfo.add(classId);
    }

    public List<ClassId> getTeachInfo() {
        return _teachInfo;
    }

    public boolean canTeach(ClassId classId) {
        // If the player is on a third class, fetch the class teacher information for its parent class.
        if (classId.level() == 3) { return _teachInfo.contains(classId.getParent()); }

        return _teachInfo.contains(classId);
    }

    // Add a drop to a given category. If the category does not exist, create it.
    public void addDropData(DropData drop, int categoryType) {
        // If the category doesn't already exist, create it first
        synchronized (_categories) {
            boolean catExists = false;
            for (DropCategory cat : _categories) {
                // If the category exists, add the drop to this category.
                if (cat.getCategoryType() == categoryType) {
                    cat.addDropData(drop, isType("L2RaidBoss") || isType("L2GrandBoss"));
                    catExists = true;
                    break;
                }
            }

            // If the category doesn't exit, create it and add the drop
            if (!catExists) {
                DropCategory cat = new DropCategory(categoryType);
                cat.addDropData(drop, isType("L2RaidBoss") || isType("L2GrandBoss"));
                _categories.add(cat);
            }
        }
    }

    public void addRaidData(L2MinionData minion) {
        _minions.add(minion);
    }

    public void addSkill(L2Skill skill) {
        if (!skill.isPassive()) {
            if (skill.isSuicideAttack()) { _suicideSkills.add(skill); }
            else {
                switch (skill.getSkillType()) {
                    case BUFF:
                    case CONT:
                    case REFLECT:
                        _buffSkills.add(skill);
                        break;

                    case HEAL:
                    case HOT:
                    case HEAL_PERCENT:
                    case HEAL_STATIC:
                    case BALANCE_LIFE:
                    case MANARECHARGE:
                    case MANAHEAL_PERCENT:
                        _healSkills.add(skill);
                        break;

                    case DEBUFF:
                    case ROOT:
                    case SLEEP:
                    case STUN:
                    case PARALYZE:
                    case POISON:
                    case DOT:
                    case MDOT:
                    case BLEED:
                    case MUTE:
                    case FEAR:
                    case CANCEL:
                    case NEGATE:
                    case WEAKNESS:
                    case AGGDEBUFF:
                        _debuffSkills.add(skill);
                        break;

                    case PDAM:
                    case MDAM:
                    case BLOW:
                    case DRAIN:
                    case CHARGEDAM:
                    case FATAL:
                    case DEATHLINK:
                    case MANADAM:
                    case CPDAMPERCENT:
                    case GET_PLAYER:
                    case INSTANT_JUMP:
                    case AGGDAMAGE:
                        addShortOrLongRangeSkill(skill);
                        break;
                }
            }
        }
        _skills.put(skill.getId(), skill);
    }

    /**
     * @return the list of all possible UNCATEGORIZED drops of this L2NpcTemplate.
     */
    public List<DropCategory> getDropData() {
        return _categories;
    }

    /**
     * @return the list of all Minions that must be spawn with the L2Npc using this L2NpcTemplate.
     */
    public List<L2MinionData> getMinionData() {
        return _minions;
    }

    public Map<Integer, L2Skill> getSkills() {
        return _skills;
    }

    public L2Skill[] getSkillsArray() {
        return _skills.values().toArray(new L2Skill[_skills.values().size()]);
    }

    public void addQuestEvent(EventType eventType, Quest quest) {
        List<Quest> eventList = _questEvents.get(eventType);
        if (eventList == null) {
            eventList = new ArrayList<>();
            eventList.add(quest);
            _questEvents.put(eventType, eventList);
        }
        else {
            eventList.remove(quest);

            if (eventType.isMultipleRegistrationAllowed() || eventList.isEmpty()) { eventList.add(quest); }
            else {
                LOGGER.warn("Quest event not allow multiple quest registrations. Skipped addition of EventType \"{}\" for NPC \"{}\" and quest \"{}\".", eventType, _name, quest.getName());
            }
        }
    }

    public Map<EventType, List<Quest>> getEventQuests() {
        return _questEvents;
    }

    public List<Quest> getEventQuests(EventType eventType) {
        return _questEvents.get(eventType);
    }

    public void setRace(int raceId) {
        switch (raceId) {
            case 1:
                _race = NpcRace.UNDEAD;
                break;
            case 2:
                _race = NpcRace.MAGICCREATURE;
                break;
            case 3:
                _race = NpcRace.BEAST;
                break;
            case 4:
                _race = NpcRace.ANIMAL;
                break;
            case 5:
                _race = NpcRace.PLANT;
                break;
            case 6:
                _race = NpcRace.HUMANOID;
                break;
            case 7:
                _race = NpcRace.SPIRIT;
                break;
            case 8:
                _race = NpcRace.ANGEL;
                break;
            case 9:
                _race = NpcRace.DEMON;
                break;
            case 10:
                _race = NpcRace.DRAGON;
                break;
            case 11:
                _race = NpcRace.GIANT;
                break;
            case 12:
                _race = NpcRace.BUG;
                break;
            case 13:
                _race = NpcRace.FAIRIE;
                break;
            case 14:
                _race = NpcRace.HUMAN;
                break;
            case 15:
                _race = NpcRace.ELVE;
                break;
            case 16:
                _race = NpcRace.DARKELVE;
                break;
            case 17:
                _race = NpcRace.ORC;
                break;
            case 18:
                _race = NpcRace.DWARVE;
                break;
            case 19:
                _race = NpcRace.OTHER;
                break;
            case 20:
                _race = NpcRace.NONLIVING;
                break;
            case 21:
                _race = NpcRace.SIEGEWEAPON;
                break;
            case 22:
                _race = NpcRace.DEFENDINGARMY;
                break;
            case 23:
                _race = NpcRace.MERCENARIE;
                break;
            default:
                _race = NpcRace.UNKNOWN;
                break;
        }
    }

    public int getNpcId() {
        return _npcId;
    }

    public String getName() {
        return _name;
    }

    public String getTitle() {
        return _title;
    }

    public NpcRace getRace() {
        if (_race == null) { _race = NpcRace.UNKNOWN; }
        return _race;
    }

    public String getType() {
        return _type;
    }

    public int getRewardExp() {
        return _exp;
    }

    public int getRewardSp() {
        return _sp;
    }

    public int getRightHand() {
        return _rHand;
    }

    public int getLeftHand() {
        return _lHand;
    }

    public byte getLevel() {
        return _level;
    }

    public int getDropHerbGroup() {
        return _dropHerbGroup;
    }

    public int getEnchantEffect() {
        return _enchantEffect;
    }

    public int getIdTemplate() {
        return _idTemplate;
    }

    public int getCorpseTime() {
        return _corpseTime;
    }

    public void setAIData(L2NpcAIData aidata) {
        _AIdata = aidata;
    }

    public L2NpcAIData getAIData() {
        return _AIdata;
    }

    public void addShortOrLongRangeSkill(L2Skill skill) {
        if (skill.getCastRange() > 150) { _longRangeSkills.add(skill); }
        else if (skill.getCastRange() > 0) { _shortRangeSkills.add(skill); }
    }

    public List<L2Skill> getSuicideSkills() {
        return _suicideSkills;
    }

    public List<L2Skill> getHealSkills() {
        return _healSkills;
    }

    public List<L2Skill> getDebuffSkills() {
        return _debuffSkills;
    }

    public List<L2Skill> getBuffSkills() {
        return _buffSkills;
    }

    public List<L2Skill> getLongRangeSkills() {
        return _longRangeSkills;
    }

    public List<L2Skill> getShortRangeSkills() {
        return _shortRangeSkills;
    }

    public boolean isSpecialTree() {
        return _npcId == L2XmassTreeInstance.SPECIAL_TREE_ID;
    }

    public boolean isUndead() {
        return _race == NpcRace.UNDEAD;
    }

    public boolean cantBeChampion() {
        return _cantBeChampionMonster;
    }

    public boolean isCustomNpc() {
        return _npcId != _idTemplate;
    }

    /**
     * Checks types, ignore case.
     *
     * @param t the type to check.
     * @return true if the type are the same, false otherwise.
     */
    public boolean isType(String t) {
        return _type.equalsIgnoreCase(t);
    }
}