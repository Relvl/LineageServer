package net.sf.l2j.commons.reflection;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Johnson / 02.06.2017
 */
public final class ReflectionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReflectionManager.class);

    /** Дженерики класса. Должны быть указаны явно. */
    private static final Map<Class<?>, List<Class<?>>> GENERICS_MAP = new HashMap<>();
    /** Список аннотированных полей класса. */
    private static final Table<Class<?>, Class<? extends Annotation>, List<FieldAccessor>> ANNOTATED_FIELDS_MAP = HashBasedTable.create();

    private ReflectionManager() {}

    public static <T extends Annotation> List<FieldAccessor> getAnnotatedFields(Class<?> clazz, Class<T> annotationClass) {
        if (!ANNOTATED_FIELDS_MAP.contains(clazz, annotationClass)) {
            List<FieldAccessor> accessors = new ArrayList<>();
            Class<?> classToProcess = clazz;
            do {
                for (Field field : classToProcess.getDeclaredFields()) {
                    // Это закешированные поля, и им не нужно обратно выставлять ограничения доступа.
                    // ReflectionManager гарантирует, что закешированные поля вернутся без ограничений.
                    field.setAccessible(true);
                    Annotation annotationInstance = field.getAnnotation(annotationClass);
                    if (annotationInstance != null) {
                        FieldAccessor accessor = new FieldAccessor(field, annotationInstance);
                        accessors.add(accessor);
                    }
                }
                classToProcess = classToProcess.getSuperclass();
            }
            while (classToProcess != null);

            ANNOTATED_FIELDS_MAP.put(clazz, annotationClass, accessors);
            return accessors;
        }
        return ANNOTATED_FIELDS_MAP.get(clazz, annotationClass);
    }

    /** Возвращает класс дженерика, параметризующего указанный класс. */
    public static <T> Class<T> getGenericClass(Class<?> basicClass, int genericIndex) {
        List<Class<?>> generics = new ArrayList<>();
        // Если уже есть в кеше - возвращаем.
        if (GENERICS_MAP.containsKey(basicClass)) {
            //noinspection unchecked На самом деле и есть анчекед, и тут может упасть ClassCastException, если запросить не то, что должно реально вернуться.
            return (Class<T>) GENERICS_MAP.get(basicClass).get(genericIndex);
        }

        try {
            LOGGER.info("Calculating generics of '{}'...", basicClass.getSimpleName());

            ParameterizedType genSuperclass = (ParameterizedType) basicClass.getGenericSuperclass();
            if (genSuperclass == null) { return null; }

            Type[] typeArguments = genSuperclass.getActualTypeArguments();
            if (typeArguments == null || typeArguments.length == 0) { return null; }

            for (Type argument : typeArguments) {
                LOGGER.debug("argument: {}", argument);
                generics.add((Class<?>) argument);
            }

            //noinspection unchecked На самом деле и есть анчекед, и тут может упасть ClassCastException, если запросить не то, что должно реально вернуться.
            return (Class<T>) generics.get(genericIndex);
        }
        catch (RuntimeException e) {
            LOGGER.error("", e);
            return null;
        }
        finally {
            GENERICS_MAP.put(basicClass, generics);
        }
    }

}
