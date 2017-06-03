package net.sf.l2j.commons.reflection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * @author Johnson / 03.06.2017
 */
public class FieldAccessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(FieldAccessor.class);

    private final Field field;
    private final Annotation annotation;

    public FieldAccessor(Field field, Annotation annotation) {
        this.field = field;
        this.annotation = annotation;
    }

    /**  */
    public Field getField() { return field; }

    /**  */
    public <A extends Annotation> A getAnnotation() {
        //noinspection unchecked
        return (A) annotation;
    }

    /**  */
    public Object getInstanceValue(Object instance) {
        try {
            return getField().get(instance);
        } catch (IllegalAccessException e) {
            LOGGER.error("", e);
            return null;
        }
    }

    /**  */
    public Object getStaticValue() {
        if (!Modifier.isStatic(getField().getModifiers())) {
            return null;
        }
        return getInstanceValue(null);
    }

    @Override
    public String toString() {
        return "FieldAccessor{" +
                "field=" + getField() +
                ", annotation=" + annotation +
                '}';
    }
}