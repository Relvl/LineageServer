package net.sf.l2j.gameserver.datatables;

import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.skills.DocumentSkill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SkillTable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SkillTable.class);

    private static final Map<Integer, L2Skill> _skills = new HashMap<>();
    private static final Map<Integer, Integer> _skillMaxLevel = new HashMap<>();

    private static final L2Skill[] _heroSkills = new L2Skill[5];
    private static final int[] _heroSkillsId = { 395, 396, 1374, 1375, 1376 };

    private static final L2Skill[] _nobleSkills = new L2Skill[8];
    private static final int[] _nobleSkillsId = { 325, 326, 327, 1323, 1324, 1325, 1326, 1327 };

    public static SkillTable getInstance() {
        return SingletonHolder.INSTANCE;
    }

    protected SkillTable() {
        load();
    }

    private static void load() {
        File dir = new File("./data/xml/skills");

        for (File file : dir.listFiles()) {
            DocumentSkill doc = new DocumentSkill(file);
            doc.parse();

            for (L2Skill skill : doc.getSkills()) { _skills.put(getSkillHashCode(skill), skill); }
        }

        LOGGER.info("SkillTable: Loaded {} skills.", _skills.size());

        // Stores max level of skills in a map for future uses.
        for (L2Skill skill : _skills.values()) {
            // Only non-enchanted skills
            int skillLvl = skill.getLevel();
            if (skillLvl < 99) {
                int skillId = skill.getId();
                int maxLvl = getMaxLevel(skillId);

                if (skillLvl > maxLvl) { _skillMaxLevel.put(skillId, skillLvl); }
            }
        }

        for (FrequentSkill sk : FrequentSkill.values()) { sk.skill = getInfo(sk.id, sk.level); }
        for (int i = 0; i < _heroSkillsId.length; i++) { _heroSkills[i] = getInfo(_heroSkillsId[i], 1); }
        for (int i = 0; i < _nobleSkills.length; i++) { _nobleSkills[i] = getInfo(_nobleSkillsId[i], 1); }
    }

    public static void reload() {
        _skills.clear();
        _skillMaxLevel.clear();

        load();
    }

    public static int getSkillHashCode(L2Skill skill) {
        return getSkillHashCode(skill.getId(), skill.getLevel());
    }

    public static int getSkillHashCode(int skillId, int skillLevel) {
        return skillId * 256 + skillLevel;
    }

    public static L2Skill getInfo(int skillId, int level) {
        return _skills.get(getSkillHashCode(skillId, level));
    }

    public static int getMaxLevel(int skillId) {
        Integer maxLevel = _skillMaxLevel.get(skillId);
        return (maxLevel != null) ? maxLevel : 0;
    }

    public L2Skill[] getSiegeSkills(boolean addNoble) {
        L2Skill[] temp = new L2Skill[2 + (addNoble ? 1 : 0)];
        int i = 0;
        temp[i++] = _skills.get(getSkillHashCode(246, 1));
        temp[i++] = _skills.get(getSkillHashCode(247, 1));
        if (addNoble) { temp[i++] = _skills.get(getSkillHashCode(326, 1)); }
        return temp;
    }

    public static L2Skill[] getHeroSkills() {
        return _heroSkills;
    }

    public static boolean isHeroSkill(int skillid) {
        for (int id : _heroSkillsId) { if (id == skillid) { return true; } }
        return false;
    }

    public static L2Skill[] getNobleSkills() {
        return _nobleSkills;
    }

    /** Enum to hold some important references to frequently used (hardcoded) skills in core */
    @SuppressWarnings("MagicNumber")
    public enum FrequentSkill {
        LUCKY(194, 1),
        SEAL_OF_RULER(246, 1),
        BUILD_HEADQUARTERS(247, 1),
        STRIDER_SIEGE_ASSAULT(325, 1),
        DWARVEN_CRAFT(1321, 1),
        COMMON_CRAFT(1322, 1),
        LARGE_FIREWORK(2025, 1),
        SPECIAL_TREE_RECOVERY_BONUS(2139, 1),

        ANTHARAS_JUMP(4106, 1),
        ANTHARAS_TAIL(4107, 1),
        ANTHARAS_FEAR(4108, 1),
        ANTHARAS_DEBUFF(4109, 1),
        ANTHARAS_MOUTH(4110, 1),
        ANTHARAS_BREATH(4111, 1),
        ANTHARAS_NORMAL_ATTACK(4112, 1),
        ANTHARAS_NORMAL_ATTACK_EX(4113, 1),
        ANTHARAS_SHORT_FEAR(5092, 1),
        ANTHARAS_METEOR(5093, 1),

        RAID_CURSE(4215, 1),
        WYVERN_BREATH(4289, 1),
        ARENA_CP_RECOVERY(4380, 1),
        RAID_CURSE2(4515, 1),
        VARKA_KETRA_PETRIFICATION(4578, 1),
        FAKE_PETRIFICATION(4616, 1),
        THE_VICTOR_OF_WAR(5074, 1),
        THE_VANQUISHED_OF_WAR(5075, 1),
        BLESSING_OF_PROTECTION(5182, 1),
        FIREWORK(5965, 1);

        protected final int id;
        protected final int level;
        protected L2Skill skill;

        FrequentSkill(int id, int level) {
            this.id = id;
            this.level = level;
        }

        public L2Skill getSkill() {
            return skill;
        }
    }

    private static final class SingletonHolder {
        private static final SkillTable INSTANCE = new SkillTable();
    }
}