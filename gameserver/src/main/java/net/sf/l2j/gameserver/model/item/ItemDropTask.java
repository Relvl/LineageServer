package net.sf.l2j.gameserver.model.item;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.geoengine.PathFinding;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.taskmanager.ItemsOnGroundTaskManager;

/**
 * @author Johnson / 21.07.2017
 */
public class ItemDropTask implements Runnable {
    private final L2Character dropper;
    private final L2ItemInstance itemInstance;
    private int posX;
    private int posY;
    private int posZ;

    public ItemDropTask(L2ItemInstance item, L2Character dropper, int x, int y, int z) {
        posX = x;
        posY = y;
        posZ = z;
        this.dropper = dropper;
        itemInstance = item;
    }

    @Override
    public final void run() {
        assert itemInstance.getPosition().getWorldRegion() == null;

        if (Config.GEODATA > 0 && dropper != null) {
            Location dropDest = PathFinding.getInstance().canMoveToTargetLoc(dropper.getX(), dropper.getY(), dropper.getZ(), posX, posY, posZ);
            posX = dropDest.getX();
            posY = dropDest.getY();
            posZ = dropDest.getZ();
        }

        synchronized (itemInstance) {
            itemInstance.setIsVisible(true);
            itemInstance.getPosition().setXYZ(posX, posY, posZ);
            itemInstance.getPosition().setWorldRegion(L2World.getInstance().getRegion(itemInstance.getPosition()));
        }

        itemInstance.getPosition().getWorldRegion().addVisibleObject(itemInstance);

        itemInstance.setDropperObjectId(dropper != null ? dropper.getObjectId() : 0);
        L2World.getInstance().addVisibleObject(itemInstance, itemInstance.getPosition().getWorldRegion());
        ItemsOnGroundTaskManager.getInstance().add(itemInstance, dropper);
        itemInstance.setDropperObjectId(0);
    }
}
