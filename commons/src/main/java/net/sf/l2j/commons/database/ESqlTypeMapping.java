package net.sf.l2j.commons.database;

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.*;

/**
 * @author Johnson / 03.06.2017
 */
public enum ESqlTypeMapping {
    UNKNOWN(Types.OTHER, "getObject"),
    INT(Types.INTEGER, "getInt"),
    BYTEA(Types.BINARY, "getBytes"),
    VARCHAR(Types.VARCHAR, "getString"),
    CURSOR(Types.OTHER, "getObject");

    private static final Map<Class<?>, ESqlTypeMapping> MAPPING = new HashMap<>();
    private final int type;
    private final String methodName;

    static {
        MAPPING.put(Integer.class, INT);

        MAPPING.put(byte[].class, BYTEA);

        MAPPING.put(List.class, CURSOR);
        MAPPING.put(ArrayList.class, CURSOR);
        MAPPING.put(LinkedList.class, CURSOR);
    }

    ESqlTypeMapping(int type, String methodName) {
        this.type = type;
        this.methodName = methodName;
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

    public int getType() {
        return type;
    }


    public static ESqlTypeMapping getType(Class<?> clazz) {
        if (clazz == null || !MAPPING.containsKey(clazz)) {
            return UNKNOWN;
        }
        return MAPPING.get(clazz);
    }

}
