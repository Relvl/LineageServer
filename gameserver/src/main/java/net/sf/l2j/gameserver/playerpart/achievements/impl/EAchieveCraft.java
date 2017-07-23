package net.sf.l2j.gameserver.playerpart.achievements.impl;

import net.sf.l2j.gameserver.playerpart.achievements.IAchieveElement;

/**
 * @author Johnson / 23.07.2017
 */
public enum EAchieveCraft implements IAchieveElement {
    CRAFT_NOT_CONSUMABLES_100_TIMES("Я, крафтер", 100, "Скрафтить %s любых предметов, кроме расходников"),
    CRAFT_CONSUMABLES_1000_TIMES("Расходники в массы", 1000, "Скрафтить любые расходники %s раз"),
    CRAFT_100_A_WEAPONS("Will Will Smith smith?", 100, "Скрафтить %s экземпляров оружия [A] грейда или выше");

    private final String title;
    private final int count;
    private final String desc;

    EAchieveCraft(String title, int count, String desc) {
        this.title = title;
        this.count = count;
        this.desc = desc;
    }

    @Override
    public String title() { return title; }

    @Override
    public String description() { return desc; }

    @Override
    public int getCount() { return count; }

    @Override
    public String getId() { return name(); }
}
