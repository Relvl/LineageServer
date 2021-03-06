package net.sf.l2j.gameserver.database;

import net.sf.l2j.commons.DefaultConstructor;
import net.sf.l2j.commons.database.AUserDefinedType;
import net.sf.l2j.commons.database.annotation.OrmTypeParam;

/**
 * @author Johnson / 02.08.2017
 */
public class UDT_Level extends AUserDefinedType {
    @OrmTypeParam(0)
    private Integer level;
    @OrmTypeParam(1)
    private Long exp;
    @OrmTypeParam(2)
    private Integer sp;

    @DefaultConstructor
    public UDT_Level() { }

    public UDT_Level(Integer level, Long exp, Integer sp) {
        this.level = level;
        this.exp = exp;
        this.sp = sp;
    }

    @Override
    public String toString() {
        return "UDT_Level{" +
                "level=" + level +
                ", exp=" + exp +
                ", sp=" + sp +
                '}';
    }
}
