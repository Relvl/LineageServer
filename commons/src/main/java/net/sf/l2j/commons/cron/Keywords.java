package net.sf.l2j.commons.cron;

import java.util.Arrays;

final class Keywords {
    private final int[][][] keywords = new int[26][26][26];

    public Keywords() {
        for (int[][] second : keywords) {
            for (int[] third : second) {
                Arrays.fill(third, -1);
            }
        }
    }

    public void put(String keyword, int value) {
        keywords[letterAt(keyword, 0)][letterAt(keyword, 1)][letterAt(keyword, 2)] = value;
    }

    public int get(String s, int start, int end) {
        if (end - start != 3) { throw new IllegalArgumentException(); }
        int number = keywords[arrayIndex(s, start)][arrayIndex(s, start + 1)][arrayIndex(s, start + 2)];
        if (number >= 0) { return number; }
        throw new IllegalArgumentException();
    }

    private int arrayIndex(String s, int charIndex) {
        int index = letterAt(s, charIndex);
        if (index < 0 || index >= keywords.length) { throw new IllegalArgumentException(); }
        return index;
    }

    private static int letterAt(String s, int charIndex) {
        return s.charAt(charIndex) - 'A';
    }
}