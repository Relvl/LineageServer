package net.sf.l2j.commons.database;

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.*;
import java.util.function.Function;

/**
 * @author Johnson / 03.06.2017
 */
public enum ESqlTypeMapping {
    VARCHAR(Types.VARCHAR, "getString", s -> s),
    BYTE(Types.SMALLINT, "getByte", Byte::parseByte),
    SMALLINT(Types.SMALLINT, "getShort", Short::parseShort),
    INT(Types.INTEGER, "getInt", Integer::parseInt),
    BIGINT(Types.BIGINT, "getLong", Long::parseLong),
    BYTEA(Types.BINARY, "getBytes", VARCHAR.fromStringTransformer),
    USER_DEFINED_TYPE(Types.OTHER, "getObject", VARCHAR.fromStringTransformer),
    CURSOR(Types.OTHER, "getObject", VARCHAR.fromStringTransformer),
    ARRAY(Types.ARRAY, "getArray", VARCHAR.fromStringTransformer),

    UNKNOWN(Types.OTHER, "getObject", VARCHAR.fromStringTransformer);

    private static final Map<Class<?>, ESqlTypeMapping> MAPPING = new HashMap<>();
    private final int type;
    private final String methodName;
    private final Function<String, Object> fromStringTransformer;

    static {
        MAPPING.put(String.class, VARCHAR);

        MAPPING.put(byte.class, SMALLINT);
        MAPPING.put(Short.class, SMALLINT);
        MAPPING.put(Integer.class, INT);
        MAPPING.put(Long.class, BIGINT);

        MAPPING.put(byte[].class, BYTEA);

        MAPPING.put(List.class, CURSOR);
        MAPPING.put(ArrayList.class, CURSOR);
        MAPPING.put(LinkedList.class, CURSOR);

        MAPPING.put(Integer[].class, ARRAY);
        MAPPING.put(Long[].class, ARRAY);
        MAPPING.put(String[].class, ARRAY);
        MAPPING.put(Boolean[].class, ARRAY);
    }

    ESqlTypeMapping(int type, String methodName, Function<String, Object> fromStringTransformer) {
        this.type = type;
        this.methodName = methodName;
        this.fromStringTransformer = fromStringTransformer;
    }

    public Object readFromStatement(CallableStatement statement, int position) throws ReflectiveOperationException {
        Method method = statement.getClass().getMethod(methodName, int.class);
        method.setAccessible(true);
        return method.invoke(statement, position);
    }

    public Object readFromResultSet(ResultSet rs, String name) throws ReflectiveOperationException {
        Method method = rs.getClass().getMethod(methodName, String.class);
        method.setAccessible(true);
        return method.invoke(rs, name);
    }

    public int getType() { return type; }

    public Object fromString(String value) { return fromStringTransformer.apply(value); }

    public static ESqlTypeMapping getType(Class<?> clazz) {
        if (AUserDefinedType.class.isAssignableFrom(clazz)) {
            return USER_DEFINED_TYPE;
        }
        if (!MAPPING.containsKey(clazz)) {
            return UNKNOWN;
        }
        return MAPPING.get(clazz);
    }

}
