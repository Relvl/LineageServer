package net.sf.l2j.gameserver.database;

import net.sf.l2j.commons.DefaultConstructor;
import net.sf.l2j.commons.database.AUserDefinedType;
import net.sf.l2j.commons.database.annotation.OrmTypeParam;

/**
 * @author Johnson / 12.08.2017
 */
public class UDT_Hitpoint extends AUserDefinedType {

    @OrmTypeParam(0)
    private Integer hp;
    @OrmTypeParam(1)
    private Integer mp;
    @OrmTypeParam(2)
    private Integer cp;

    @DefaultConstructor
    public UDT_Hitpoint() {
    }

    public UDT_Hitpoint(Integer hp, Integer mp, Integer cp) {
        this.hp = hp;
        this.mp = mp;
        this.cp = cp;
    }

    public UDT_Hitpoint(double hp, double mp, double cp) {
        this.hp = (int) hp;
        this.mp = (int) mp;
        this.cp = (int) cp;
    }

    public Integer getHp() { return hp; }

    public Integer getMp() { return mp; }

    public Integer getCp() { return cp; }

    public void setHp(Integer hp) { this.hp = hp; }

    public void setMp(Integer mp) { this.mp = mp; }

    public void setCp(Integer cp) { this.cp = cp; }

    @Override
    public String toString() {
        return "UDT_Hitpoint{" +
                "hp=" + hp +
                ", mp=" + mp +
                ", cp=" + cp +
                '}';
    }
}
