package net.sf.l2j.gameserver.model.actor.knownlist;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.util.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({ "ObjectEquality", "ClassHasNoToStringMethod" })
public class ObjectKnownList {
    protected final L2Object object;
    protected final Map<Integer, L2Object> knownObjects;

    public ObjectKnownList(L2Object activeObject) {
        object = activeObject;
        knownObjects = new ConcurrentHashMap<>();
    }

    public boolean addKnownObject(L2Object object) {
        if (isObjectKnown(object)) { return false; }
        if (!Util.checkIfInShortRadius(getDistanceToWatchObject(object), this.object, object, true)) { return false; }
        return knownObjects.put(object.getObjectId(), object) == null;
    }

    public boolean removeKnownObject(L2Object object) {
        if (object == null) { return false; }
        return knownObjects.remove(object.getObjectId()) != null;
    }

    public final void forgetObjects() {
        for (L2Object knownObject : knownObjects.values()) {
            if (!knownObject.isVisible() || !Util.checkIfInShortRadius(getDistanceToForgetObject(knownObject), object, knownObject, true)) {
                removeKnownObject(knownObject);
            }
        }
    }

    public void removeAllKnownObjects() { knownObjects.clear(); }

    public final boolean isObjectKnown(L2Object object) { return object != null && (this.object == object || knownObjects.containsKey(object.getObjectId())); }

    public final Collection<L2Object> getKnownObjects() { return knownObjects.values(); }

    public final <A> List<A> getKnownType(Class<A> type) {
        return getKnownTypeInRadius(type, -1);
    }

    @SuppressWarnings("unchecked")
    public final <A> List<A> getKnownTypeInRadius(Class<A> type, int radius) {
        List<A> result = new ArrayList<>();
        for (L2Object obj : knownObjects.values()) {
            if (type.isAssignableFrom(obj.getClass()) && Util.checkIfInRange(radius, object, obj, true)) {
                result.add((A) obj);
            }
        }
        return result;
    }

    public int getDistanceToWatchObject(L2Object object) { return 0; }

    public int getDistanceToForgetObject(L2Object object) { return 0; }
}