package net.sf.l2j.gameserver.model.world;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.actor.*;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.model.zone.type.L2DerbyTrackZone;
import net.sf.l2j.gameserver.model.zone.type.L2PeaceZone;
import net.sf.l2j.gameserver.model.zone.type.L2TownZone;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

public final class L2WorldRegion {
    private final Map<Integer, L2Object> visibleObjects = new ConcurrentHashMap<>();
    private final Map<Integer, L2Playable> playableObjects = new ConcurrentHashMap<>();

    private final List<L2WorldRegion> neighborRegions = new ArrayList<>();
    private final List<L2ZoneType> zones = new ArrayList<>();

    private final int tileX;
    private final int tileY;

    private boolean regionActive;
    private ScheduledFuture<?> updateNeighborsActiveTask;

    public L2WorldRegion(int tileX, int tileY) {
        this.tileX = tileX;
        this.tileY = tileY;
    }

    public List<L2ZoneType> getZones() {
        return zones;
    }

    public void addZone(L2ZoneType zone) {
        zones.add(zone);
    }

    public void removeZone(L2ZoneType zone) {
        zones.remove(zone);
    }

    public void revalidateZones(L2Character character) {
        if (character.isTeleporting()) { return; }

        for (L2ZoneType z : zones) {
            if (z != null) {
                z.revalidateInZone(character);
            }
        }
    }

    public void removeFromZones(L2Character character) {
        for (L2ZoneType z : zones) {
            if (z != null) {
                z.removeCharacter(character);
            }
        }
    }

    public boolean containsZone(int zoneId) {
        for (L2ZoneType z : zones) {
            if (z.getId() == zoneId) {
                return true;
            }
        }
        return false;
    }

    public boolean isEffectRangeInsidePeaceZone(L2Skill skill, int x, int y, int z) {
        int range = skill.getEffectRange();
        int up = y + range;
        int down = y - range;
        int left = x + range;
        int right = x - range;

        for (L2ZoneType e : zones) {
            if ((e instanceof L2TownZone && ((L2TownZone) e).isPeaceZone()) || e instanceof L2DerbyTrackZone || e instanceof L2PeaceZone) {
                if (e.isInsideZone(x, up, z)) { return false; }
                if (e.isInsideZone(x, down, z)) { return false; }
                if (e.isInsideZone(left, y, z)) { return false; }
                if (e.isInsideZone(right, y, z)) { return false; }
                if (e.isInsideZone(x, y, z)) { return false; }
            }
        }
        return true;
    }

    public void onDeath(L2Character character) {
        for (L2ZoneType zoneType : zones) {
            if (zoneType != null) {
                zoneType.onDieInside(character);
            }
        }
    }

    public void onRevive(L2Character character) {
        for (L2ZoneType zoneType : zones) {
            if (zoneType != null) {
                zoneType.onReviveInside(character);
            }
        }
    }

    public boolean isActive() {
        return regionActive;
    }

    public void setActive() {
        if (regionActive) {return;}
        regionActive = true;
        for (L2Object o : visibleObjects.values()) {
            if (o instanceof L2Attackable) {
                ((L2Attackable) o).getStatus().startHpMpRegeneration();
            }
            else if (o instanceof L2Npc) {
                ((L2Npc) o).startRandomAnimationTimer();
            }
        }
    }

    public void setInactive() {
        if (!regionActive) {return;}
        regionActive = true;
        for (L2Object o : visibleObjects.values()) {
            if (o instanceof L2Attackable) {
                L2Attackable mob = (L2Attackable) o;
                mob.setTarget(null);
                mob.stopMove(null);
                mob.stopAllEffects();
                mob.clearAggroList();
                mob.getAttackByList().clear();
                mob.getKnownList().removeAllKnownObjects();

                if (mob.hasAI()) {
                    mob.getAI().setIntention(EIntention.IDLE);
                    mob.getAI().stopAITask();
                }
            }
            else if (o instanceof L2Vehicle) {
                ((L2Vehicle) o).getKnownList().removeAllKnownObjects();
            }
        }
    }

    public boolean isNeighborsEmpty() {
        if (regionActive && !playableObjects.isEmpty()) {
            return false;
        }
        for (L2WorldRegion neighbor : neighborRegions) {
            if (neighbor.regionActive && !neighbor.playableObjects.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Immediately sets self as active and starts a timer to set neighbors as active this timer is to avoid turning on neighbors in the case when a person just teleported into a region and then teleported out immediately...there is no reason to activate all the neighbors in that case.
     */
    private void startActivation() {
        setActive();

        // if the timer to deactivate neighbors is running, cancel it.
        synchronized (this) {
            if (updateNeighborsActiveTask != null) {
                updateNeighborsActiveTask.cancel(true);
                updateNeighborsActiveTask = null;
            }

            // then, set a timer to activate the neighbors
            updateNeighborsActiveTask = ThreadPoolManager.getInstance().scheduleGeneral(new NeighborsTask(true), 1000);
        }
    }

    private void startDeactivation() {
        synchronized (this) {
            if (updateNeighborsActiveTask != null) {
                updateNeighborsActiveTask.cancel(true);
                updateNeighborsActiveTask = null;
            }
            updateNeighborsActiveTask = ThreadPoolManager.getInstance().scheduleGeneral(new NeighborsTask(false), 120000);
        }
    }

    public void addVisibleObject(L2Object object) {
        if (object == null) { return; }

        assert object.getWorldRegion() == this;

        visibleObjects.put(object.getObjectId(), object);

        if (object instanceof L2Playable) {
            playableObjects.put(object.getObjectId(), (L2Playable) object);
            if (playableObjects.size() == 1) { startActivation(); }
        }
    }

    public void removeVisibleObject(L2Object object) {
        if (object == null) { return; }

        assert object.getWorldRegion() == this || object.getWorldRegion() == null;

        visibleObjects.remove(object.getObjectId());

        if (object instanceof L2Playable) {
            playableObjects.remove(object.getObjectId());

            if (playableObjects.isEmpty()) { startDeactivation(); }
        }
    }

    public void addSurroundingRegion(L2WorldRegion region) {
        neighborRegions.add(region);
    }

    public List<L2WorldRegion> getNeighbors() {
        return neighborRegions;
    }

    public Map<Integer, L2Playable> getVisiblePlayable() {
        return playableObjects;
    }

    public Map<Integer, L2Object> getVisibleObjects() {
        return visibleObjects;
    }

    public String getName() {
        return "(" + tileX + ", " + tileY + ')';
    }

    private final class NeighborsTask implements Runnable {
        private final boolean isActivating;

        private NeighborsTask(boolean isActivating) {
            this.isActivating = isActivating;
        }

        @Override
        public void run() {
            if (isActivating) {
                for (L2WorldRegion neighbor : neighborRegions) {
                    neighbor.setActive();
                }
            }
            else {
                if (isNeighborsEmpty()) {
                    setInactive();
                }

                for (L2WorldRegion neighbor : neighborRegions) {
                    if (neighbor.isNeighborsEmpty()) {
                        neighbor.setInactive();
                    }
                }
            }
        }
    }

}