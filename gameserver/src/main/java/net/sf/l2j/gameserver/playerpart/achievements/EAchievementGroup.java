package net.sf.l2j.gameserver.playerpart.achievements;

import net.sf.l2j.gameserver.playerpart.achievements.impl.EAchieveCraft;

import java.util.stream.Stream;

/**
 * @author Johnson / 07.08.2017
 */
public enum EAchievementGroup {
    CRAFT("Производство", "«Не молот железо кует, а кузнец, что молотом бьет!»", "Icon.weapon_work_hammer_i00", EAchieveCraft.values()),
    SPOIL("Собирательство", "«Прекрасная вещь» — скажет коллекционер, а отнюдь не «прекрасная статуэтка».", "Icon.skill4270_1"),
    COMMON("Общее", "«Отличие упрямства от упорства в том, что упрямство оставляет после себя экскременты неудач, а упорство - плоды успеха.»", "L2UI_CH3.bloodhood_icon03");

    private final String title;
    private final String description;
    private final String icon;
    private final IAchieveElement[] enmValues;

    EAchievementGroup(String title, String description, String icon, IAchieveElement... enm) {
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.enmValues = enm;
    }

    public String getTitle() { return title; }

    public String getIcon() { return icon; }

    public String getDescription() { return description; }

    public static Stream<IAchieveElement> getAllAchievements() { return Stream.of(values()).flatMap(enm -> Stream.of(enm.enmValues)); }

    public static IAchieveElement getAchievement(String id) {
        for (EAchievementGroup group : values()) {
            if (group.enmValues != null) {
                for (IAchieveElement achieveElement : group.enmValues) {
                    if (achieveElement.getId().equalsIgnoreCase(id)) {
                        return achieveElement;
                    }
                }
            }
        }
        return null;
    }

    public Stream<IAchieveElement> getAchievements() {
        return Stream.of(enmValues);
    }

    public static EAchievementGroup getGroup(String key) {
        for (EAchievementGroup group : values()) {
            if (group.name().equals(key)) {
                return group;
            }
        }
        return null;
    }
}
