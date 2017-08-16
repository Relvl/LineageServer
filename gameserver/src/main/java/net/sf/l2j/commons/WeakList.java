package net.sf.l2j.commons;

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * Лист со слабыми ссылками на объект.
 * Особенность заключается в том, что объект удаляется мусорщиком в том случае, если на него ссылаются только слабые ссылки.
 * Этот лист стоит использовать только в том случае, если нельзя допускать удержание объекта, которы должен был бы удалиться, если бы на него не было ссылки в листе.
 * Например, листы слушателей.
 * <p>
 * Чёт я не смог победить затирание типов по-нормальному, без костылей. Поэтому пусть конструкторы принимают класс.
 *
 * @author Johnson / 14.08.2017
 */
public class WeakList<T> extends AbstractList<T> {
    private final List<WeakReference<T>> innerList = new ArrayList<>();
    private final Class<T> genericType;

    public WeakList(Class<T> genericType) {
        this.genericType = genericType;
    }

    public WeakList(Collection<T> collection, Class<T> genericType) {
        this.genericType = genericType;
        addAll(0, collection);
    }

    @Override
    public void add(int index, T element) {
        innerList.add(index, new WeakReference<T>(element));
    }

    @Override
    public T get(int index) {
        return innerList.get(index).get();
    }

    @Override
    public int size() {
        clearReleased();
        return innerList.size();
    }

    /** Удаляет все WeakReference, объект которых уже удалён мусорщиком. */
    @SuppressWarnings({ "SuspiciousMethodCalls", "ForLoopReplaceableByForEach", "ForLoopWithMissingComponent" })
    public void clearReleased() {
        for (Iterator<WeakReference<T>> it = innerList.iterator(); it.hasNext(); ) {
            WeakReference<T> ref = it.next();
            if (ref.get() == null) { innerList.remove(ref); }
        }
    }

    @Override
    @SuppressWarnings("ReturnOfInnerClass")
    public Iterator<T> iterator() {
        return new WeakListIterator();
    }

    public Class<T> getGenericType() {
        return genericType;
    }

    @Override
    public String toString() {
        return "WeakList{" + innerList + '}';
    }

    @SuppressWarnings("NonStaticInnerClassInSecureContext")
    private final class WeakListIterator implements Iterator<T> {
        private final int currentSize;
        private int currentIndex;

        private WeakListIterator() {
            currentSize = size();
            currentIndex = 0;
        }

        @Override
        public boolean hasNext() {
            return currentIndex < currentSize;
        }

        @Override
        public T next() {
            return get(currentIndex++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Iterator is a lie!");
        }
    }
}
