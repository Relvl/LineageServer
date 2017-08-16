package net.sf.l2j.gameserver.playerpart.achievements;

import java.io.Serializable;

/**
 * @author Johnson / 23.07.2017
 */
public interface IAchieveElement extends Serializable {
    /** «аголовок. */
    String title();

    /**  */
    String description();

    /**  оличество "раз", которое нужно совершить действие до достижени€. */
    default int getCount() { return 1; }

    /**  */
    String getId();

    EAchievementGroup getGroup();
}
