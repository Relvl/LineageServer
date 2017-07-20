package net.sf.l2j.commons.lang;

import java.sql.ResultSet;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class StringUtil {
    public static final String DIGITS = "0123456789";
    public static final String LOWER_CASE_LETTERS = "abcdefghijklmnopqrstuvwxyz";
    public static final String UPPER_CASE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static final String LETTERS = LOWER_CASE_LETTERS + UPPER_CASE_LETTERS;
    public static final String LETTERS_AND_DIGITS = LETTERS + DIGITS;
    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    /**
     * Checks each String passed as parameter. If at least one is empty or null, than return false.
     *
     * @param strings : The Strings to test.
     * @return false if at least one String is empty or null.
     */
    public static boolean isEmpty(String... strings) {
        for (String str : strings) {
            if (str == null || str.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Appends objects to an existing StringBuilder.
     *
     * @param sb      : the StringBuilder to edit.
     * @param content : parameters to append.
     */
    public static void append(StringBuilder sb, Object... content) {
        for (Object obj : content) { sb.append((obj == null) ? null : obj.toString()); }
    }

    /**
     * @param text : the String to check.
     * @return true if the String contains only numbers, false otherwise.
     */
    public static boolean isDigit(String text) {
        if (text == null) {
            return false;
        }

        return text.matches("[0-9]+");
    }

    /**
     * @param text : the String to check.
     * @return true if the String contains only numbers and letters, false otherwise.
     */
    public static boolean isAlphaNumeric(String text) {
        if (text == null) {
            return false;
        }

        for (char chars : text.toCharArray()) {
            if (!Character.isLetterOrDigit(chars)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param value : the number to format.
     * @return a number formatted with "," delimiter.
     */
    public static String formatNumber(long value) {
        return NumberFormat.getInstance(Locale.ENGLISH).format(value);
    }

    /**
     * @param string : the initial word to scramble.
     * @return an anagram of the given string.
     */
    public static String scrambleString(String string) {
        List<String> letters = Arrays.asList(string.split(""));
        Collections.shuffle(letters);

        StringBuilder sb = new StringBuilder(string.length());
        for (String c : letters) { sb.append(c); }

        return sb.toString();
    }

    /**
     * Verify if the given text matches with the regex pattern.
     *
     * @param text  : the text to test.
     * @param regex : the regex pattern to make test with.
     * @return true if matching.
     */
    public static boolean isValidName(String text, String regex) {
        Pattern pattern;
        try {
            pattern = Pattern.compile(regex);
        }
        catch (PatternSyntaxException e) // case of illegal pattern
        {
            pattern = Pattern.compile(".*");
        }

        Matcher regexp = pattern.matcher(text);

        return regexp.matches();
    }

    /**
     * Child of isValidName, with regular pattern for players' name.
     *
     * @param text : the text to test.
     * @return true if matching.
     */
    public static boolean isValidPlayerName(String text) {
        return isValidName(text, "^[A-Za-z0-9]{1,16}$");
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 3 - 1];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = hexArray[v >>> 4];
            hexChars[j * 3 + 1] = hexArray[v & 0x0F];
            if (j < bytes.length - 1) { hexChars[j * 3 + 2] = ' '; }
        }
        return new String(hexChars);
    }

    public static String objectToString(Object o) {
        if (o == null){
            return "NULL";
        }
        if (o.getClass() == byte[].class) {
            return "[" + bytesToHex((byte[]) o) + "]";
        }
        if (o instanceof ResultSet) {
            return "[REF_CURSOR]";
        }
        return String.valueOf(o);
    }
}