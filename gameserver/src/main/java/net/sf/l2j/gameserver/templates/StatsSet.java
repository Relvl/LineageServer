package net.sf.l2j.gameserver.templates;

import java.util.HashMap;

/**
 * This class is used in order to have a set of couples (key,value).<BR>
 * Methods deployed are accessors to the set (add/get value from its key) and addition of a whole set in the current one.
 *
 * @author mkizub, G1ta0
 */
@SuppressWarnings("serial")
public class StatsSet extends HashMap<String, Object> {
    public StatsSet() {
    }

    public StatsSet(int size) {
        super(size);
    }

    public StatsSet(StatsSet set) {
        super(set);
    }

    public void set(String key, Object value) {
        put(key, value);
    }

    public void set(String key, String value) {
        put(key, value);
    }

    public void set(String key, boolean value) {
        put(key, value ? Boolean.TRUE : Boolean.FALSE);
    }

    public void set(String key, int value) {
        put(key, value);
    }

    public void set(String key, int[] value) {
        put(key, value);
    }

    public void set(String key, long value) {
        put(key, value);
    }

    public void set(String key, double value) {
        put(key, value);
    }

    public void set(String key, Enum<?> value) {
        put(key, value);
    }

    public void unset(String key) {
        remove(key);
    }

    public boolean isSet(String key) {
        return get(key) != null;
    }

    @Override
    public StatsSet clone() {
        return new StatsSet(this);
    }

    public boolean getBool(String key) {
        Object val = get(key);

        if (val instanceof Number) { return ((Number) val).intValue() != 0; }
        if (val instanceof String) { return Boolean.parseBoolean((String) val); }
        if (val instanceof Boolean) { return (Boolean) val; }

        throw new IllegalArgumentException("StatsSet : Boolean value required, but found: " + val + " for key: " + key + ".");
    }

    public boolean getBool(String key, boolean defaultValue) {
        Object val = get(key);

        if (val instanceof Number) { return ((Number) val).intValue() != 0; }
        if (val instanceof String) { return Boolean.parseBoolean((String) val); }
        if (val instanceof Boolean) { return (Boolean) val; }

        return defaultValue;
    }

    public byte getByte(String key) {
        Object val = get(key);

        if (val instanceof Number) { return ((Number) val).byteValue(); }
        if (val instanceof String) { return Byte.parseByte((String) val); }

        throw new IllegalArgumentException("StatsSet : Byte value required, but found: " + val + " for key: " + key + ".");
    }

    public byte getByte(String key, byte defaultValue) {
        Object val = get(key);

        if (val instanceof Number) { return ((Number) val).byteValue(); }
        if (val instanceof String) { return Byte.parseByte((String) val); }

        return defaultValue;
    }

    public float getFloat(String key) {
        Object val = get(key);

        if (val instanceof Number) { return ((Number) val).floatValue(); }
        if (val instanceof String) { return Float.parseFloat((String) val); }
        if (val instanceof Boolean) { return (Boolean) val ? 1 : 0; }

        throw new IllegalArgumentException("StatsSet : Float value required, but found: " + val + " for key: " + key + ".");
    }

    public float getFloat(String key, float defaultValue) {
        Object val = get(key);

        if (val instanceof Number) { return ((Number) val).floatValue(); }
        if (val instanceof String) { return Float.parseFloat((String) val); }
        if (val instanceof Boolean) { return (Boolean) val ? 1 : 0; }

        return defaultValue;
    }

    public int getInteger(String key) {
        Object val = get(key);

        if (val instanceof Number) { return ((Number) val).intValue(); }
        if (val instanceof String) { return Integer.parseInt((String) val); }
        if (val instanceof Boolean) { return (Boolean) val ? 1 : 0; }

        throw new IllegalArgumentException("StatsSet : Integer value required, but found: " + val + " for key: " + key + ".");
    }

    public int getInteger(String key, int defaultValue) {
        Object val = get(key);

        if (val instanceof Number) { return ((Number) val).intValue(); }
        if (val instanceof String) { return Integer.parseInt((String) val); }
        if (val instanceof Boolean) { return (Boolean) val ? 1 : 0; }

        return defaultValue;
    }

    public int[] getIntegerArray(String key) {
        Object val = get(key);

        if (val instanceof int[]) { return (int[]) val; }
        if (val instanceof Number) {
            return new int[]
                    {
                            ((Number) val).intValue()
                    };
        }
        if (val instanceof String) {
            String[] vals = ((String) val).split(";");

            int[] result = new int[vals.length];

            int i = 0;
            for (String v : vals) { result[i++] = Integer.parseInt(v); }

            return result;
        }

        throw new IllegalArgumentException("StatsSet : Integer array required, but found: " + val + " for key: " + key + ".");
    }

    public int[] getIntegerArray(String key, int[] defaultArray) {
        try {
            return getIntegerArray(key);
        }
        catch (IllegalArgumentException e) {
            return defaultArray;
        }
    }

    public long getLong(String key) {
        Object val = get(key);

        if (val instanceof Number) { return ((Number) val).longValue(); }
        if (val instanceof String) { return Long.parseLong((String) val); }
        if (val instanceof Boolean) { return (Boolean) val ? 1L : 0L; }

        throw new IllegalArgumentException("StatsSet : Long value required, but found: " + val + " for key: " + key + ".");
    }

    public long getLong(String key, long defaultValue) {
        Object val = get(key);

        if (val instanceof Number) { return ((Number) val).longValue(); }
        if (val instanceof String) { return Long.parseLong((String) val); }
        if (val instanceof Boolean) { return (Boolean) val ? 1L : 0L; }

        return defaultValue;
    }

    public long[] getLongArray(String key) {
        Object val = get(key);

        if (val instanceof long[]) { return (long[]) val; }
        if (val instanceof Number) {
            return new long[]
                    {
                            ((Number) val).longValue()
                    };
        }
        if (val instanceof String) {
            String[] vals = ((String) val).split(";");

            long[] result = new long[vals.length];

            int i = 0;
            for (String v : vals) { result[i++] = Integer.parseInt(v); }

            return result;
        }

        throw new IllegalArgumentException("StatsSet : Long array required, but found: " + val + " for key: " + key + ".");
    }

    public double getDouble(String key) {
        Object val = get(key);

        if (val instanceof Number) { return ((Number) val).doubleValue(); }
        if (val instanceof String) { return Double.parseDouble((String) val); }
        if (val instanceof Boolean) { return (Boolean) val ? 1. : 0.; }

        throw new IllegalArgumentException("StatsSet : Double value required, but found: " + val + " for key: " + key + ".");
    }

    public double getDouble(String key, double defaultValue) {
        Object val = get(key);

        if (val instanceof Number) { return ((Number) val).doubleValue(); }
        if (val instanceof String) { return Double.parseDouble((String) val); }
        if (val instanceof Boolean) { return (Boolean) val ? 1. : 0.; }

        return defaultValue;
    }

    public String getString(String key) {
        Object val = get(key);

        if (val != null) { return String.valueOf(val); }

        throw new IllegalArgumentException("StatsSet : String value required, but unspecified for key: " + key + ".");
    }

    public String getString(String key, String defaultValue) {
        Object val = get(key);

        if (val != null) { return String.valueOf(val); }

        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    public <A> A getObject(String key, Class<A> type) {
        Object val = get(key);

        if (val == null || !type.isAssignableFrom(val.getClass())) { return null; }

        return (A) val;
    }

    @SuppressWarnings("unchecked")
    public <E extends Enum<E>> E getEnum(String name, Class<E> enumClass) {
        Object val = get(name);

        if (val != null && enumClass.isInstance(val)) { return (E) val; }
        if (val instanceof String) { return Enum.valueOf(enumClass, (String) val); }

        throw new IllegalArgumentException("Enum value of type " + enumClass.getName() + "required, but found: " + val + ".");
    }

    @SuppressWarnings("unchecked")
    public <E extends Enum<E>> E getEnum(String name, Class<E> enumClass, E defaultValue) {
        Object val = get(name);

        if (val != null && enumClass.isInstance(val)) { return (E) val; }
        if (val instanceof String) { return Enum.valueOf(enumClass, (String) val); }

        return defaultValue;
    }
}