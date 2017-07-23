package net.sf.l2j.commons.database;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.database.annotation.*;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.reflection.FieldAccessor;
import net.sf.l2j.commons.reflection.ReflectionManager;
import org.postgresql.jdbc.PgArray;
import org.slf4j.Logger;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Johnson / 02.06.2017
 */
public abstract class IndexedCall implements AutoCloseable {
    private static final Map<String, Class<?>> TYPE_MAPPING = new HashMap<>();

    private Connection connection;
    private CallableStatement statement;
    private String sqlStatementString;
    private String logStatementString;

    protected IndexedCall(String procedureName, int argumentsCount, boolean isFunction) {
        generateSqlStatement(procedureName, argumentsCount, isFunction);
    }

    public Connection getConnection() {
        if (connection == null) {
            connection = L2DatabaseFactory.getInstance().getConnection();
        }
        return connection;
    }

    public CallableStatement getStatement() throws SQLException {
        if (statement == null) {
            statement = getConnection().prepareCall(sqlStatementString);
        }
        return statement;
    }

    private void storeTypeMapping(FieldAccessor accessor) {
        OrmTypeName typeName = accessor.getField().getType().getAnnotation(OrmTypeName.class);
        if (typeName != null && !TYPE_MAPPING.containsKey(typeName.value())) {
            try {
                Map<String, Class<?>> map = getConnection().getTypeMap();
                if (map == null) {
                    map = new HashMap<>();
                    map.putAll(TYPE_MAPPING);
                }
                map.put(typeName.value(), accessor.getField().getType());
                getConnection().setTypeMap(map);
                TYPE_MAPPING.put(typeName.value(), accessor.getField().getType());
            }
            catch (SQLException e) {
                getLogger().error("Cannot add type mapping {}", typeName.value(), e);
            }
        }

    }

    // { ? = call some_func(?, ?) }
    private void generateSqlStatement(String procedureName, int argumentsCount, boolean isFunction) {
        StringBuilder sqlBuilder = new StringBuilder("{ ");
        StringBuilder logBuilder = new StringBuilder("{ ");
        if (isFunction) {
            sqlBuilder.append("? = ");
            logBuilder.append("{1} = ");
        }
        sqlBuilder.append("call ").append(procedureName).append("(");
        logBuilder.append("call ").append(procedureName).append("(");
        if (argumentsCount > 0) {
            for (int i = isFunction ? 2 : 1; i < argumentsCount + (isFunction ? 2 : 1); i++) {
                sqlBuilder.append("?, ");
                logBuilder.append("{").append(i).append("}, ");
            }
            sqlBuilder.delete(sqlBuilder.length() - 2, sqlBuilder.length());
            logBuilder.delete(logBuilder.length() - 2, logBuilder.length());
        }
        this.sqlStatementString = sqlBuilder.append(") }").toString();
        this.logStatementString = logBuilder.append(") }").toString();
    }

    public final void execute() throws CallException {

        try {
            // Регистрируем исходящие аргументы.
            List<FieldAccessor> paramsOut = ReflectionManager.getAnnotatedFields(getClass(), OrmParamOut.class);
            boolean hasCursors = false;
            if (paramsOut != null && !paramsOut.isEmpty()) {
                for (FieldAccessor accessor : paramsOut) {
                    storeTypeMapping(accessor);
                    int position = accessor.getField().getAnnotation(OrmParamOut.class).value();
                    ESqlTypeMapping sqlType = ESqlTypeMapping.getType(accessor.getField().getType());
                    if (sqlType == ESqlTypeMapping.CURSOR) {
                        hasCursors = true;
                    }
                    getStatement().registerOutParameter(position, sqlType.getType());
                }
            }
            // Назначаем входящие аргументы.
            List<FieldAccessor> paramsIn = ReflectionManager.getAnnotatedFields(getClass(), OrmParamIn.class);
            if (paramsIn != null && !paramsIn.isEmpty()) {
                for (FieldAccessor accessor : paramsIn) {
                    storeTypeMapping(accessor);
                    int position = accessor.getField().getAnnotation(OrmParamIn.class).value();
                    ESqlTypeMapping sqlType = ESqlTypeMapping.getType(accessor.getField().getType());
                    getStatement().setObject(position, accessor.getInstanceValue(this), sqlType.getType());
                    logStatementString = logStatementString.replaceAll("\\{" + position + "\\}", String.valueOf(accessor.getInstanceValue(this)));
                }
            }

            if (hasCursors) {
                getConnection().setAutoCommit(false);
            }
            getStatement().execute();

            if (paramsOut != null && !paramsOut.isEmpty()) {
                for (FieldAccessor accessor : paramsOut) {
                    OrmParamOut ormParamOut = accessor.getField().getAnnotation(OrmParamOut.class);
                    OrmCollectionType ormCollectionType = accessor.getField().getAnnotation(OrmCollectionType.class);
                    int position = ormParamOut.value();
                    ESqlTypeMapping sqlType = ESqlTypeMapping.getType(accessor.getField().getType());

                    Object argument = sqlType.readFromStatement(getStatement(), position);
                    // CURSORS =============================================================================================
                    if (sqlType == ESqlTypeMapping.CURSOR) {
                        StringBuilder logSb = new StringBuilder("REF_CURSOR[");

                        List cursorList = (List) accessor.getField().get(this);
                        if (cursorList == null) {
                            throw new CallException("Please use instances of List in param '" + getClass().getName() + ":" + accessor.getField().getName() + "'.", null);
                        }
                        cursorList.clear();

                        Class<?> cursorClass = ormParamOut.cursorClass();
                        if (cursorClass.isAssignableFrom(Object.class)) {
                            throw new CallException("Please use 'cursorClass' annotation argument in param '" + getClass().getName() + ":" + accessor.getField().getName() + "'.", null);
                        }

                        List<FieldAccessor> cursorAccessors = ReflectionManager.getAnnotatedFields(cursorClass, OrmParamCursor.class);

                        try (ResultSet resultSet = (ResultSet) argument) {
                            while (resultSet.next()) {
                                logSb.append("{");
                                Object cursorElement = cursorClass.getConstructor().newInstance();
                                for (FieldAccessor cursorAccessor : cursorAccessors) {
                                    OrmParamCursor ormParamCursor = cursorAccessor.getField().getAnnotation(OrmParamCursor.class);
                                    Object cursorElementField = sqlType.readFromResultSet(resultSet, ormParamCursor.value());
                                    cursorAccessor.getField().set(cursorElement, cursorElementField);

                                    logSb.append(ormParamCursor.value()).append("=").append(StringUtil.objectToString(cursorElementField)).append(", ");
                                }
                                logSb.delete(logSb.length() - 2, logSb.length() - 1);
                                //noinspection unchecked
                                cursorList.add(cursorElement);
                                logSb.append("},");
                            }
                            logSb.delete(logSb.length() - 1, logSb.length());
                        }
                        logStatementString = logStatementString.replaceAll("\\{" + position + "\\}", logSb.append("]").toString());
                    }
                    // ARRAYS ==============================================================================================
                    else if (sqlType == ESqlTypeMapping.ARRAY && PgArray.class.isAssignableFrom(argument.getClass())) {
                        StringBuilder logSb = new StringBuilder("ARRAY");
                        PgArray pgArray = (PgArray) argument;
                        Object[] array = (Object[]) pgArray.getArray();

                        System.out.println(">>> pgArray.getBaseTypeName()" + pgArray.getBaseTypeName());
                        switch (pgArray.getBaseTypeName()) {
                            case "numeric":
                                BigDecimal[] bdArray = (BigDecimal[]) array;
                                Object[] finalArray = (Object[]) Array.newInstance(accessor.getField().getType().getComponentType(), bdArray.length);
                                for (int i = 0; i < bdArray.length; i++) {
                                    if (accessor.getField().getType().getComponentType().isAssignableFrom(Integer.class)) {
                                        finalArray[i] = bdArray[i].intValue();
                                    }
                                    else if (accessor.getField().getType().getComponentType().isAssignableFrom(Long.class)) {
                                        finalArray[i] = bdArray[i].longValue();
                                    }
                                }
                                logSb.append(Arrays.toString(finalArray));
                                accessor.getField().set(this, finalArray);
                                break;
                            default:
                                logSb.append(Arrays.toString(array));
                                accessor.getField().set(this, array);
                                break;
                        }
                        logStatementString = logStatementString.replaceAll("\\{" + position + "\\}", logSb.toString());
                    }
                    // OBJECTS =============================================================================================
                    else if (sqlType == ESqlTypeMapping.UNKNOWN) {
                        System.out.println(">>> unn field");
                    }
                    // REGULAR FIELDS ======================================================================================
                    else {
                        accessor.getField().set(this, argument);
                        logStatementString = logStatementString.replaceAll("\\{" + position + "\\}", StringUtil.objectToString(argument));
                    }
                }
            }

            if (hasCursors) {
                getConnection().setAutoCommit(true);
            }

            getLogger().info("DB <-> {}", logStatementString);
        }
        catch (SQLException | ReflectiveOperationException e) {
            getLogger().info("DB <-> {}", logStatementString);
            throw new CallException("Cannot execute call '" + getClass().getSimpleName() + "'", e);
        }
        finally {
            if (throwErrorOnRecultCode() && (getResultCode() == null || getResultCode() != 0)) {
                //noinspection ThrowFromFinallyBlock
                throw new CallException("Result code " + getResultCode() + " for call " + getClass().getSimpleName(), null);
            }
        }
    }

    @Override
    public void close() throws CallException {
        try {
            if (statement != null) {
                statement.close();
            }
        }
        catch (SQLException e) {
            throw new CallException("Cannot close statement of '" + getClass().getSimpleName() + "'", e);
        }
        try {
            if (connection != null) {
                connection.close();
            }
        }
        catch (SQLException e) {
            throw new CallException("Cannot close connection of '" + getClass().getSimpleName() + "'", e);
        }
    }

    public abstract Logger getLogger();

    public Integer getResultCode() { return 0; }

    protected boolean throwErrorOnRecultCode() {
        return false;
    }
}
