package net.sf.l2j.gameserver.playerpart.achievements;

/**
 * @author Johnson / 23.07.2017
 */
public enum EAchieveGroup {
    UNKNOWN(null),
    CRAFT(EAchieveCraft.class);

    private final Class<? extends IAchieveElement> element;

    EAchieveGroup(Class<? extends IAchieveElement> element) {
        this.element = element;
    }

    public static EAchieveGroup getByClass(IAchieveElement element) {
        Class<? extends IAchieveElement> achieveClass = element.getClass();
        for (EAchieveGroup group : values()) {
            if (achieveClass.isAssignableFrom(group.element)) {
                return group;
            }
        }
        return UNKNOWN;
    }
}
