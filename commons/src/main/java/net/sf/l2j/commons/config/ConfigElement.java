package net.sf.l2j.commons.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Johnson / 15.07.2017
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface ConfigElement {

    /** Относительный путь к файлу настройки */
    String fileName();

    /** Не записывать конфиг без явной необходимости динамически. */
    boolean doNotSaveDinamically() default false;
}
