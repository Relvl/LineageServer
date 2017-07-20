package net.sf.l2j.commons.database;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.commons.database.annotation.OrmParamCursor;
import net.sf.l2j.commons.database.annotation.OrmParamIn;
import net.sf.l2j.commons.database.annotation.OrmParamOut;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.reflection.FieldAccessor;
import net.sf.l2j.commons.reflection.ReflectionManager;
import org.slf4j.Logger;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * @author Johnson / 02.06.2017
 */
public abstract class IndexedCall implements AutoCloseable {
    private Connection connection;
    private CallableStatement statement;
    private String sqlStatementString;
    private String logStatementString;

    public IndexedCall(String procedureName, int argumentsCount, boolean isFunction) {
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
            statement = getConnection().prepareCall(this.sqlStatementString);
        }
        return statement;
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
        boolean hasCursors = false;

        try {
            // Регистрируем исходящие аргументы.
            List<FieldAccessor> paramsOut = ReflectionManager.getAnnotatedFields(this.getClass(), OrmParamOut.class);
            if (paramsOut != null && !paramsOut.isEmpty()) {
                for (FieldAccessor accessor : paramsOut) {
                    int position = accessor.getField().getAnnotation(OrmParamOut.class).value();
                    ESqlTypeMapping sqlType = ESqlTypeMapping.getType(accessor.getField().getType());
                    if (sqlType == ESqlTypeMapping.CURSOR) {
                        hasCursors = true;
                    }
                    getStatement().registerOutParameter(position, sqlType.getType());
                }
            }
            // Назначаем входящие аргументы.
            List<FieldAccessor> paramsIn = ReflectionManager.getAnnotatedFields(this.getClass(), OrmParamIn.class);
            if (paramsIn != null && !paramsIn.isEmpty()) {
                for (FieldAccessor accessor : paramsIn) {
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
                    int position = ormParamOut.value();
                    ESqlTypeMapping sqlType = ESqlTypeMapping.getType(accessor.getField().getType());

                    Object argument = sqlType.readFromStatement(getStatement(), position);
                    // CURSORS =============================================================================================
                    if (sqlType == ESqlTypeMapping.CURSOR) {
                        StringBuilder logSb = new StringBuilder("REF_CURSOR[");

                        List cursorList = (List) accessor.getField().get(this);
                        if (cursorList == null) {
                            throw new IllegalArgumentException("Please use instances of List in param '" + getClass().getName() + ":" + accessor.getField().getName() + "'.");
                        }
                        cursorList.clear();

                        Class<?> cursorClass = ormParamOut.cursorClass();
                        if (cursorClass == Object.class) {
                            throw new IllegalArgumentException("Please use 'cursorClass' annotation argument in param '" + getClass().getName() + ":" + accessor.getField().getName() + "'.");
                        }

                        List<FieldAccessor> cursorAccessors = ReflectionManager.getAnnotatedFields(cursorClass, OrmParamCursor.class);

                        try (ResultSet resultSet = (ResultSet) argument) {
                            while (resultSet.next()) {
                                logSb.append("{");
                                Object cursorElement = cursorClass.newInstance();
                                for (FieldAccessor cursorAccessor : cursorAccessors) {
                                    ESqlTypeMapping cursorElementSql = ESqlTypeMapping.getType(cursorAccessor.getField().getType());
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
        } catch (SQLException | ReflectiveOperationException e) {
            throw new CallException("Cannot execute call '" + getClass().getSimpleName() + "'", e);
        }
    }

    @Override
    public void close() throws CallException {
        try {
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
            throw new CallException("Cannot close statement of '" + getClass().getSimpleName() + "'", e);
        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new CallException("Cannot close connection of '" + getClass().getSimpleName() + "'", e);
        }
    }

    public abstract Logger getLogger();

    public Integer getResultCode() {
        return 0;
    }
}
