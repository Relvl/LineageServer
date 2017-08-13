package net.sf.l2j.gameserver.skills.func;

import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2CubicInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.skill.L2Skill;

public final class Env {
    private L2Character character;
    private L2CubicInstance cubic;
    private L2Character target;
    private L2ItemInstance item;
    private L2Skill skill;

    private double baseValue;
    private double value;

    private boolean skillMastery;
    private byte shield;

    private boolean soulShot;
    private boolean spiritShot;
    private boolean blessedSpiritShot;

    public Env() {}

    public Env(byte shield, boolean soulShot, boolean spiritShot, boolean blessedSpiritShot) {
        this.shield = shield;
        this.soulShot = soulShot;
        this.spiritShot = spiritShot;
        this.blessedSpiritShot = blessedSpiritShot;
    }

    public L2Character getCharacter() { return character; }

    public L2CubicInstance getCubic() { return cubic; }

    public L2Character getTarget() { return target; }

    public L2ItemInstance getItem() { return item; }

    public L2Skill getSkill() { return skill; }

    public L2PcInstance getPlayer() { return character == null ? null : character.getActingPlayer(); }

    public double getValue() { return value; }

    public double getBaseValue() { return baseValue; }

    public boolean isSkillMastery() { return skillMastery; }

    public byte getShield() { return shield; }

    public boolean isSoulShot() { return soulShot; }

    public boolean isSpiritShot() { return spiritShot; }

    public boolean isBlessedSpiritShot() { return blessedSpiritShot; }

    public void setCharacter(L2Character character) { this.character = character; }

    public void setCubic(L2CubicInstance cubic) { this.cubic = cubic; }

    public void setTarget(L2Character target) { this.target = target; }

    public void setItem(L2ItemInstance item) { this.item = item; }

    public void setSkill(L2Skill skill) { this.skill = skill; }

    public void setValue(double value) { this.value = value; }

    public void setBaseValue(double baseValue) { this.baseValue = baseValue; }

    public void setSkillMastery(boolean skillMastery) { this.skillMastery = skillMastery; }

    public void setShield(byte shield) { this.shield = shield; }

    public void setSoulShot(boolean soulShot) { this.soulShot = soulShot; }

    public void setSpiritShot(boolean spiritShot) { this.spiritShot = spiritShot; }

    public void setBlessedSpiritShot(boolean blessedSpiritShot) { this.blessedSpiritShot = blessedSpiritShot; }

    public void addValue(double value) { this.value += value; }

    public void subValue(double value) { this.value -= value; }

    public void mulValue(double value) { this.value *= value; }

    public void divValue(double value) { this.value /= value; }
}