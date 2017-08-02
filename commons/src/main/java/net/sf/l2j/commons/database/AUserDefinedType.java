package net.sf.l2j.commons.database;

import net.sf.l2j.commons.CsvHelper;
import net.sf.l2j.commons.database.annotation.OrmTypeParam;
import net.sf.l2j.commons.reflection.FieldAccessor;
import net.sf.l2j.commons.reflection.ReflectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Представляет бозовый класс механики маршаллинга UserDefinedType<->JavaBean для PostgreSQL. <br/>
 * Поддерживает вложенные тайпы. <br/>
 * Поддерживает все типы данных, описанные в {@link ESqlTypeMapping}
 *
 * @author Johnson / 02.08.2017
 */
public abstract class AUserDefinedType {
    private static final Logger LOGGER = LoggerFactory.getLogger(AUserDefinedType.class);

    /** Строки приходят без кавычек, вложенные типы - в кавычках. */
    public static AUserDefinedType readFromSql(String sqlData, Class<?> clazz) {
        if (!AUserDefinedType.class.isAssignableFrom(clazz)) {
            LOGGER.error("", new RuntimeException("Wrong class bound"));
            return null;
        }

        try {
            List<FieldAccessor> fieldset = ReflectionManager.getAnnotatedFields(clazz, OrmTypeParam.class);
            //noinspection unchecked
            AUserDefinedType instance = ((Class<? extends AUserDefinedType>) clazz).getConstructor().newInstance();

            CsvHelper.parseCsvLine(sqlData, (csvField, index) -> {
                // Ищем первое поле, у которого значение в аннотации совпадает с индексом поля в типе.
                FieldAccessor accessor = fieldset.stream().filter(a -> ((OrmTypeParam) a.getAnnotation()).value() == index).findFirst().orElse(null);

                if (accessor != null) {
                    ESqlTypeMapping mapping = ESqlTypeMapping.getType(accessor.getField().getType());
                    if (mapping == ESqlTypeMapping.UNKNOWN) {
                        return;
                    }
                    if (mapping == ESqlTypeMapping.USER_DEFINED_TYPE) {
                        try {
                            accessor.getField().set(instance, readFromSql(csvField, accessor.getField().getType()));
                        }
                        catch (IllegalAccessException e) {
                            LOGGER.error("", e);
                        }
                        return;
                    }
                    try {
                        accessor.getField().set(instance, mapping.fromString(csvField));
                    }
                    catch (IllegalAccessException e) {
                        LOGGER.error("", e);
                    }
                }
            });

            return instance;
        }
        catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException | InstantiationException e) {
            LOGGER.error("", e);
            return null;
        }
    }

    public String getSqlString() {
        return ReflectionManager.getAnnotatedFields(getClass(), OrmTypeParam.class).stream()
                .sorted((o1, o2) -> {
                    OrmTypeParam a1 = o1.getAnnotation();
                    OrmTypeParam a2 = o2.getAnnotation();
                    return Integer.compare(a1.value(), a2.value());
                })
                .map(fieldAccessor -> {
                    if (AUserDefinedType.class.isAssignableFrom(fieldAccessor.getField().getType())) {
                        return ((AUserDefinedType) fieldAccessor.getInstanceValue(this)).getSqlString();
                    }
                    if (Objects.equals(fieldAccessor.getField().getType(), String.class)) {
                        return '\'' + (String) fieldAccessor.getInstanceValue(this) + '\'';
                    }
                    return String.valueOf(fieldAccessor.getInstanceValue(this));
                })
                .collect(Collectors.joining(",", "(", ")"));
    }
}
