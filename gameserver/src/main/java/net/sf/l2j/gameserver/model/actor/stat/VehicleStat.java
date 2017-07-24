package net.sf.l2j.gameserver.model.actor.stat;

import net.sf.l2j.gameserver.model.actor.L2Vehicle;

public class VehicleStat extends CharStat {
    private int moveSpeed;
    private int rotationSpeed;

    public VehicleStat(L2Vehicle activeChar) {
        super(activeChar);
    }

    @Override
    public int getMoveSpeed() {
        return moveSpeed;
    }

    public final void setMoveSpeed(int speed) {
        moveSpeed = speed;
    }

    public final int getRotationSpeed() {
        return rotationSpeed;
    }

    public final void setRotationSpeed(int speed) {
        rotationSpeed = speed;
    }
}