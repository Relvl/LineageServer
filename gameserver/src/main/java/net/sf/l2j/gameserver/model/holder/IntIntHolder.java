package net.sf.l2j.gameserver.model.holder;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.skill.L2Skill;

public class IntIntHolder {
    private int id;
    private int value;

    public IntIntHolder(int id, int value) {
        this.id = id;
        this.value = value;
    }

    public int getId() {
        return id;
    }

    public int getValue() {
        return value;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public final L2Skill getSkill() {
        return SkillTable.getInfo(id, value);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": Id: " + id + ", Value: " + value;
    }
}