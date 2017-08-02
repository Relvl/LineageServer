package net.sf.l2j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Johnson / 02.08.2017
 */
public class TestMain {
    private static final Pattern PTN_CSV_MAIN = Pattern.compile("\\G(?:^|,)(?:\"((?:[^\"]++|\"\")*+)\"|([^\",]*))");
    private static final Pattern PTN_CSV_QUOTE = Pattern.compile("\"\"");

    public static void main(String... args) {
        String sqlData = "(268480263,aa,awdaaw,,\"(0,0,0,0)\",\"(20,888683,27018)\")";
        if (sqlData.startsWith("(")) { sqlData = sqlData.substring(1); }
        if (sqlData.endsWith(")")) { sqlData = sqlData.substring(0, sqlData.length() - 1); }
        Matcher mMain = PTN_CSV_MAIN.matcher("");
        Matcher mQuote = PTN_CSV_QUOTE.matcher("");
        mMain.reset(sqlData); // Строка с текстом в формате CSV
        while (mMain.find()) {
            String field = mMain.start(2) >= 0 ? mMain.group(2) : mQuote.reset(mMain.group(1)).replaceAll("\"");
            System.out.println("Field [" + field + "]");
        }
    }
}
