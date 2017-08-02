package net.sf.l2j.gameserver.database;

import net.sf.l2j.commons.DefaultConstructor;
import net.sf.l2j.commons.database.AUserDefinedType;
import net.sf.l2j.commons.database.annotation.OrmTypeParam;
import net.sf.l2j.gameserver.model.location.HeadedLocation;

/**
 * @author Johnson / 02.08.2017
 */
public class UDT_Player extends AUserDefinedType {
    @OrmTypeParam(0)
    private Integer playerId;
    @OrmTypeParam(1)
    private String login;
    @OrmTypeParam(2)
    private String name;
    @OrmTypeParam(3)
    private String title;
    @OrmTypeParam(4)
    private HeadedLocation location;
    @OrmTypeParam(5)
    private UDT_Level level;

    @DefaultConstructor
    public UDT_Player() { }

    @Override
    public String toString() {
        return "UDT_Player{" +
                "playerId=" + playerId +
                ", login='" + login + '\'' +
                ", name='" + name + '\'' +
                ", title='" + title + '\'' +
                ", location=" + location +
                ", level=" + level +
                '}';
    }
}
