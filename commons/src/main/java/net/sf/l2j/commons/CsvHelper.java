package net.sf.l2j.commons;

import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Утилитарный класс для работы с CSV файлами и строками.
 * <p>
 * И даже не пытайтесь меня спрашивать, как устроена эта магия на регулярках...
 * Я, судя по всему, был под лютой химией, когда писал это. Но оно работает, лучше не трогать.
 *
 * @author Johnson / 02.08.2017
 */
public final class CsvHelper {
    // \\G = Ends of previous match
    private static final Pattern PTN_CSV_MAIN = Pattern.compile("\\G(?:^|,)(?:\"((?:[^\"]++|\"\")*+)\"|([^\",]*))");
    private static final Pattern PTN_CSV_QUOTE = Pattern.compile("\"\"");

    private CsvHelper() {}

    /** Разбирает CSV строку на поля и передаёт в функцию с указанием индекса поля (от ноля). */
    public static void parseCsvLine(String csvLine, BiConsumer<String, Integer> consumer) {
        if (csvLine == null || csvLine.isEmpty()) { return; }
        if (csvLine.charAt(0) == '(') { csvLine = csvLine.substring(1); }
        if (csvLine.charAt(csvLine.length() - 1) == ')') { csvLine = csvLine.substring(0, csvLine.length() - 1); }
        Matcher mMain = PTN_CSV_MAIN.matcher("");
        Matcher mQuote = PTN_CSV_QUOTE.matcher("");
        mMain.reset(csvLine); // Строка с текстом в формате CSV
        int index = 0;
        while (mMain.find()) {
            String csvField = mMain.start(2) >= 0 ? mMain.group(2) : mQuote.reset(mMain.group(1)).replaceAll("\"");
            consumer.accept(csvField, index++);
        }
    }

}
