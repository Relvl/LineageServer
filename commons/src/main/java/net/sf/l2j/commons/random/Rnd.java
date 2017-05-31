/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.commons.random;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class Rnd {
    public static double nextDouble() {
        return ThreadLocalRandom.current().nextDouble();
    }

    public static int nextInt(int n) {
        return ThreadLocalRandom.current().nextInt(n);
    }

    public static int nextInt() {
        return ThreadLocalRandom.current().nextInt();
    }

    public static int get(int n) {
        return nextInt(n);
    }

    public static int get(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max == Integer.MAX_VALUE ? max : max + 1);
    }

    public static long nextLong(long n) {
        return ThreadLocalRandom.current().nextLong(n);
    }

    public static long nextLong() {
        return ThreadLocalRandom.current().nextLong();
    }

    public static long get(long n) {
        return nextLong(n);
    }

    public static long get(long min, long max) {
        return ThreadLocalRandom.current().nextLong(min, max == Long.MAX_VALUE ? max : max + 1L);
    }

    public static boolean calcChance(double applicableUnits, int totalUnits) {
        return applicableUnits > nextInt(totalUnits);
    }

    public static double nextGaussian() {
        return ThreadLocalRandom.current().nextGaussian();
    }

    public static boolean nextBoolean() {
        return ThreadLocalRandom.current().nextBoolean();
    }

    public static byte[] nextBytes(int count) {
        return nextBytes(new byte[count]);
    }

    public static byte[] nextBytes(byte[] array) {
        ThreadLocalRandom.current().nextBytes(array);
        return array;
    }

    /**
     * Returns a randomly selected element taken from the given list.
     *
     * @param <T>  type of list elements.
     * @param list a list.
     * @return a randomly selected element.
     */
    public static final <T> T get(List<T> list) {
        if (list == null || list.size() == 0)
            return null;

        return list.get(get(list.size()));
    }

    /**
     * Returns a randomly selected element taken from the given array.
     *
     * @param <T>   type of array elements.
     * @param array an array.
     * @return a randomly selected element.
     */
    public static final <T> T get(T[] array) {
        if (array == null || array.length == 0)
            return null;

        return array[get(array.length)];
    }
}