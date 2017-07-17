package net.sf.l2j.gameserver.model.zone;

import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.item.instance.L2ItemInstance;

import java.awt.geom.Line2D;

public abstract class L2ZoneForm {
    protected static final int STEP = 20;

    protected static void dropDebugItem(int itemId, int num, int x, int y, int z) {
        L2ItemInstance item = new L2ItemInstance(IdFactory.getInstance().getNextId(), itemId);
        item.setCount(num);
        item.spawnMe(x, y, z + 5);
        ZoneManager.getInstance().getDebugItems().add(item);
    }

    public abstract boolean isInsideZone(int x, int y, int z);

    public abstract boolean isIntersectsRectangle(int x1, int x2, int y1, int y2);

    public abstract double getDistanceToZone(int x, int y);

    public abstract int getLowZ();

    public abstract int getHighZ();

    protected boolean isLineSegmentsIntersect(int ax1, int ay1, int ax2, int ay2, int bx1, int by1, int bx2, int by2) {
        return Line2D.linesIntersect(ax1, ay1, ax2, ay2, bx1, by1, bx2, by2);
    }

    public abstract void visualizeZone(int z);
}