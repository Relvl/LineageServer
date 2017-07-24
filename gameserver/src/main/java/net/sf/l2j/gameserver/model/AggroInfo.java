package net.sf.l2j.gameserver.model;

import net.sf.l2j.gameserver.model.actor.L2Character;

public final class AggroInfo {
    private final L2Character attacker;

    private int hate;
    private int damage;

    public AggroInfo(L2Character attacker) {
        this.attacker = attacker;
    }

    public L2Character getAttacker() {
        return attacker;
    }

    public int getHate() {
        return hate;
    }

    public int checkHate(L2Character owner) {
        if (attacker.isAlikeDead() || !attacker.isVisible() || !owner.getKnownList().isObjectKnown(attacker)) {
            hate = 0;
        }
        return hate;
    }

    public void addHate(int value) {
        hate = (int) Math.min(hate + (long) value, 999999999);
    }

    public void stopHate() {
        hate = 0;
    }

    public int getDamage() {
        return damage;
    }

    public void addDamage(int value) {
        damage = (int) Math.min(damage + (long) value, 999999999);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) { return true; }
        if (obj instanceof AggroInfo) {
            return ((AggroInfo) obj).getAttacker() == attacker;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return attacker.getObjectId();
    }
}