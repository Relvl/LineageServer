package net.sf.l2j.commons.database.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Johnson / 22.07.2017
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OrmCollectionType {
    OrmCollection value() default OrmCollection.CURSOR;

    enum OrmCollection {
        CURSOR,
        ARRAY;
    }
}
