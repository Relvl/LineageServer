package net.sf.l2j.gameserver.model;

import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.ObjectPoly;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.knownlist.ObjectKnownList;
import net.sf.l2j.gameserver.model.actor.position.ObjectPosition;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.model.world.L2WorldRegion;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.client.game_to_client.ActionFailed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class L2Object {
    protected static final Logger LOGGER = LoggerFactory.getLogger(L2Object.class);

    private String name;
    private int objectId;
    private ObjectPoly poly;
    private ObjectPosition position;
    private boolean isVisible;

    protected L2Object(int objectId) {
        this.objectId = objectId;
        initPosition();
    }

    public void initPosition() { position = new ObjectPosition(this); }

    public void onAction(L2PcInstance player) { player.sendPacket(ActionFailed.STATIC_PACKET); }

    public void onActionShift(L2PcInstance player) { player.sendPacket(ActionFailed.STATIC_PACKET); }

    public void onForcedAttack(L2PcInstance player) { player.sendPacket(ActionFailed.STATIC_PACKET); }

    public void onSpawn() { }

    public final int getX() {
        assert getPosition().getWorldRegion() != null || isVisible;
        return getPosition().getX();
    }

    public final int getY() {
        assert getPosition().getWorldRegion() != null || isVisible;
        return getPosition().getY();
    }

    public final int getZ() {
        assert getPosition().getWorldRegion() != null || isVisible;
        return getPosition().getZ();
    }

    public void decayMe() {
        assert getPosition().getWorldRegion() != null;
        L2WorldRegion reg = getPosition().getWorldRegion();
        synchronized (this) {
            isVisible = false;
            getPosition().setWorldRegion(null);
        }
        L2World.getInstance().removeVisibleObject(this, reg);
        L2World.getInstance().removeObject(this);
    }

    public void refreshID() {
        L2World.getInstance().removeObject(this);
        IdFactory.getInstance().releaseId(objectId);
        objectId = IdFactory.getInstance().getNextId();
    }

    public final void spawnMe() {
        assert getPosition().getWorldRegion() == null && getPosition().getX() != 0 && getPosition().getY() != 0 && getPosition().getZ() != 0;

        synchronized (this) {
            isVisible = true;
            getPosition().setWorldRegion(L2World.getInstance().getRegion(getPosition()));
            L2World.getInstance().addObject(this);
            getPosition().getWorldRegion().addVisibleObject(this);
        }
        L2World.getInstance().addVisibleObject(this, getPosition().getWorldRegion());
        onSpawn();
    }

    public final void spawnMe(int x, int y, int z) {
        assert getPosition().getWorldRegion() == null;
        synchronized (this) {
            isVisible = true;
            if (x > L2World.WORLD_X_MAX) { x = L2World.WORLD_X_MAX - 5000; }
            if (x < L2World.WORLD_X_MIN) { x = L2World.WORLD_X_MIN + 5000; }
            if (y > L2World.WORLD_Y_MAX) { y = L2World.WORLD_Y_MAX - 5000; }
            if (y < L2World.WORLD_Y_MIN) { y = L2World.WORLD_Y_MIN + 5000; }
            getPosition().setXYZ(x, y, z);
            getPosition().setWorldRegion(L2World.getInstance().getRegion(getPosition()));
        }
        L2World.getInstance().addObject(this);
        getPosition().getWorldRegion().addVisibleObject(this);
        L2World.getInstance().addVisibleObject(this, getPosition().getWorldRegion());
        onSpawn();
    }

    public boolean isAttackable() { return false; }

    public abstract boolean isAutoAttackable(L2Character attacker);

    public final boolean isVisible() { return getPosition().getWorldRegion() != null && isVisible; }

    public final void setIsVisible(boolean value) {
        isVisible = value;
        if (!isVisible) { getPosition().setWorldRegion(null); }
    }

    public ObjectKnownList getKnownList() { return null; }

    public final String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public final int getObjectId() { return objectId; }

    public final ObjectPoly getPoly() {
        if (poly == null) { poly = new ObjectPoly(this); }
        return poly;
    }

    public ObjectPosition getPosition() { return position; }

    public final void setObjectPosition(ObjectPosition value) { position = value; }

    public L2PcInstance getActingPlayer() { return null; }

    public L2WorldRegion getWorldRegion() { return getPosition().getWorldRegion(); }

    public void sendInfo(L2PcInstance activeChar) { }

    public boolean isChargedShot(ShotType type) { return false; }

    public void setChargedShot(ShotType type, boolean charged) { }

    public void rechargeShots(boolean physical, boolean magical) { }

    @Override
    public String toString() { return getClass().getSimpleName() + ":" + name + "[" + objectId + "]"; }

    public boolean isInsideZone(ZoneId zone) { return false; }

    public boolean isPlayer() { return false; }

    public boolean isSummon() { return false; }

    public boolean isCharacter() { return false; }
}