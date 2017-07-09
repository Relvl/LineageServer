package net.sf.l2j.commons.math;

public class MathUtil {
    /**
     * @param objectsSize : The overall elements size.
     * @param pageSize    : The number of elements per page.
     * @return The number of pages, based on the number of elements and the number of elements we want per page.
     */
    public static int countPagesNumber(int objectsSize, int pageSize) {
        return objectsSize / pageSize + (objectsSize % pageSize == 0 ? 0 : 1);
    }
}