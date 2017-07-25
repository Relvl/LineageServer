package net.sf.l2j.commons;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Johnson / 25.07.2017
 */
public final class StreamHelper {

    /**
     * Возвращает стрим без дубликатов, уосновываясь на указанном ключе.
     * <p>
     * {@code persons.stream().filter(distinctByKey(p -> p.getName());}
     */
    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
}
