package net.sf.l2j.commons.database.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Johnson / 22.07.2017
 * @deprecated Пока нужен только для массивов структур. Надо бы переделать на новую механику.
 */
@Deprecated
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface OrmTypeName {
    @Deprecated String value();
}
