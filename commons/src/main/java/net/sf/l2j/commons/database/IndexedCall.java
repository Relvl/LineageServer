package net.sf.l2j.commons.database;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.database.annotation.OrmParamCursor;
import net.sf.l2j.commons.database.annotation.OrmParamIn;
import net.sf.l2j.commons.database.annotation.OrmParamOut;
import net.sf.l2j.commons.database.annotation.OrmTypeName;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.reflection.FieldAccessor;
import net.sf.l2j.commons.reflection.ReflectionManager;
import org.postgresql.jdbc.PgArray;
import org.postgresql.util.PGobject;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/**
 * @author Johnson / 02.06.2017
 */
@SuppressWarnings("resource")  // Класс автозакрываемый, close() будет вызван из try-with-resources уровнем выше.
public abstract class IndexedCall implements AutoCloseable {
    @Deprecated
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
            //noinspection JDBCPrepareStatementWithNonConstantString
            statement = getConnection().prepareCall(sqlStatementString);
        }
        return statement;
    }

    @Deprecated
    private void storeTypeMapping(String typeName, Class<?> clazz) {
        if (typeName != null && !TYPE_MAPPING.containsKey(typeName)) {
            try {
                Map<String, Class<?>> map = getConnection().getTypeMap();
                if (map == null) {
                    map = new HashMap<>();
                    map.putAll(TYPE_MAPPING);
                }
                map.put(typeName, clazz);
                getConnection().setTypeMap(map);
                TYPE_MAPPING.put(typeName, clazz);
            }
            catch (SQLException e) {
                getLogger().error("Cannot add type mapping {}", typeName, e);
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
        sqlBuilder.append("call ").append(procedureName).append('(');
        logBuilder.append("call ").append(procedureName).append('(');
        if (argumentsCount > 0) {
            for (int i = isFunction ? 2 : 1; i < argumentsCount + (isFunction ? 2 : 1); i++) {
                sqlBuilder.append("?, ");
                logBuilder.append('{').append(i).append("}, ");
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
                    Field field = accessor.getField();
                    if (field.isAnnotationPresent(OrmTypeName.class)) { storeTypeMapping(field.getAnnotation(OrmTypeName.class).value(), field.getType()); }
                    int position = field.getAnnotation(OrmParamOut.class).value();
                    ESqlTypeMapping sqlType = ESqlTypeMapping.getType(field.getType());
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
                    Field field = accessor.getField();
                    if (field.isAnnotationPresent(OrmTypeName.class)) { storeTypeMapping(field.getAnnotation(OrmTypeName.class).value(), field.getType()); }
                    int position = field.getAnnotation(OrmParamIn.class).value();

                    if (Collection.class.isAssignableFrom(field.getType())) {
                        //noinspection rawtypes
                        Collection collection = (Collection) accessor.getInstanceValue(this);
                        Class<?> arrayElementClass = field.getAnnotation(OrmParamIn.class).arrayElementClass();
                        if (AUserDefinedType.class.isAssignableFrom(arrayElementClass)) {
                            // TODO! User Defined Types in arrays!
                            getLogger().error("", new Exception("Unsupported nesting!"));
                        }
                        else {
                            String arrayElementName = arrayElementClass.getAnnotation(OrmTypeName.class).value();
                            storeTypeMapping(arrayElementName, arrayElementClass);
                            java.sql.Array array = getConnection().createArrayOf(arrayElementName, collection.toArray());
                            getStatement().setArray(position, array);
                            logStatementString = logStatementString.replaceAll("\\{" + position + "\\}", String.valueOf(accessor.getInstanceValue(this)));
                        }
                    }
                    // User Defined Types. Штатный механизм этого драйвера постгреса мне не понравился... Делаем велосипеды.
                    else if (AUserDefinedType.class.isAssignableFrom(field.getType())) {
                        String sqlUdtString = ((AUserDefinedType) accessor.getInstanceValue(this)).getSqlString();
                        getStatement().setObject(position, sqlUdtString, Types.OTHER);
                        logStatementString = logStatementString.replaceAll("\\{" + position + "\\}", sqlUdtString);
                    }
                    else {
                        ESqlTypeMapping sqlType = ESqlTypeMapping.getType(field.getType());
                        getStatement().setObject(position, accessor.getInstanceValue(this), sqlType.getType());
                        logStatementString = logStatementString.replaceAll("\\{" + position + "\\}", String.valueOf(accessor.getInstanceValue(this)));
                    }
                }
            }

            if (hasCursors) {
                getConnection().setAutoCommit(false);
            }
            getStatement().execute();

            if (paramsOut != null && !paramsOut.isEmpty()) {
                for (FieldAccessor accessor : paramsOut) {
                    OrmParamOut ormParamOut = accessor.getField().getAnnotation(OrmParamOut.class);
                    int position = ormParamOut.value();
                    ESqlTypeMapping sqlType = ESqlTypeMapping.getType(accessor.getField().getType());

                    Object argument = sqlType.readFromStatement(getStatement(), position);
                    // CURSORS =============================================================================================
                    if (sqlType == ESqlTypeMapping.CURSOR) {
                        StringBuilder logSb = new StringBuilder("REF_CURSOR[");

                        //noinspection rawtypes
                        Collection cursorList = (Collection) accessor.getField().get(this);
                        if (cursorList == null) {
                            throw new CallException("Please use instances of List in param '" + getClass().getName() + ':' + accessor.getField().getName() + "'.", null);
                        }
                        cursorList.clear();

                        Class<?> cursorClass = ormParamOut.cursorClass();
                        if (cursorClass.isAssignableFrom(Object.class)) {
                            throw new CallException("Please use 'cursorClass' annotation argument in param '" + getClass().getName() + ':' + accessor.getField().getName() + "'.", null);
                        }

                        List<FieldAccessor> cursorAccessors = ReflectionManager.getAnnotatedFields(cursorClass, OrmParamCursor.class);

                        try (ResultSet resultSet = (ResultSet) argument) {
                            if (resultSet != null) {
                                while (resultSet.next()) {
                                    logSb.append('{');
                                    Object cursorElement = cursorClass.getConstructor().newInstance();
                                    for (FieldAccessor cursorAccessor : cursorAccessors) {
                                        OrmParamCursor ormParamCursor = cursorAccessor.getField().getAnnotation(OrmParamCursor.class);
                                        Object cursorElementField = sqlType.readFromResultSet(resultSet, ormParamCursor.value());
                                        cursorAccessor.getField().set(cursorElement, cursorElementField);

                                        logSb.append(ormParamCursor.value()).append('=').append(StringUtil.objectToString(cursorElementField)).append(", ");
                                    }
                                    logSb.delete(logSb.length() - 2, logSb.length() - 1);
                                    //noinspection unchecked
                                    cursorList.add(cursorElement);
                                    logSb.append("},");
                                }
                            }
                            logSb.delete(logSb.length() - 1, logSb.length());
                        }
                        catch (InvocationTargetException ignored) { }
                        logStatementString = logStatementString.replaceAll("\\{" + position + "\\}", logSb.append(']').toString());
                    }
                    // ARRAYS ==============================================================================================
                    else if (sqlType == ESqlTypeMapping.ARRAY && PgArray.class.isAssignableFrom(argument.getClass())) {
                        StringBuilder logSb = new StringBuilder("ARRAY");
                        java.sql.Array pgArray = (java.sql.Array) argument;
                        Object[] array = (Object[]) pgArray.getArray();

                        switch (pgArray.getBaseTypeName()) {
                            case "numeric":
                                //noinspection SuspiciousArrayCast
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
                    // TYPES ===============================================================================================
                    else if (sqlType == ESqlTypeMapping.USER_DEFINED_TYPE && PGobject.class.isAssignableFrom(argument.getClass())) {
                        if (!AUserDefinedType.class.isAssignableFrom(accessor.getField().getType())) {
                            getLogger().error("Wrong class: {}", accessor.getField().getType().getName());
                            continue;
                        }
                        PGobject pgobject = (PGobject) argument;
                        accessor.getField().set(this, AUserDefinedType.readFromSql(pgobject.getValue(), accessor.getField().getType()));
                        logStatementString = logStatementString.replaceAll("\\{" + position + "\\}", pgobject.getValue());
                    }
                    // OBJECTS =============================================================================================
                    else if (sqlType == ESqlTypeMapping.UNKNOWN) {
                        getLogger().warn(">>> unn field");
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
        catch (PSQLException e) {
            getLogger().info("DB <-> {}", logStatementString.replaceAll("\\{\\d*\\}", "NULL"));
            throw new CallException("Cannot execute call '" + getClass().getSimpleName() + "' : " + e.getMessage());
        }
        catch (SQLException | ReflectiveOperationException e) {
            getLogger().info("DB <-> {}", logStatementString.replaceAll("\\{\\d*\\}", "NULL"));
            throw new CallException("Cannot execute call '" + getClass().getSimpleName() + "' : " + e.getMessage(), e);
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
            throw new CallException("Cannot close statement of '" + getClass().getSimpleName() + '\'', e);
        }
        try {
            if (connection != null) {
                connection.close();
            }
        }
        catch (SQLException e) {
            throw new CallException("Cannot close connection of '" + getClass().getSimpleName() + '\'', e);
        }
    }

    public abstract Logger getLogger();

    public Integer getResultCode() { return 0; }

    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    protected boolean throwErrorOnRecultCode() {
        return false;
    }
}
