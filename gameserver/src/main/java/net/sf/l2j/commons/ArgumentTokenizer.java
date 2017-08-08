package net.sf.l2j.commons;

import java.util.LinkedList;

/**
 * Класс для хранения и постепенного вычитывания аргументов из строки-источника.
 * TODO Осторожно, потоконебезопасный класс! Источник может быть обновлен одновременно в нескольких потоках.
 *
 * @author Johnson / 07.08.2017
 */
public class ArgumentTokenizer {
    private final Character spliterator;
    private final LinkedList<String> pooledArguments = new LinkedList<>();
    private final String source;
    private int index = 0;

    public ArgumentTokenizer(String source) {
        this.source = source;
        this.spliterator = ' ';
    }

    public ArgumentTokenizer(String source, Character spliterator) {
        this.source = source;
        this.spliterator = spliterator;
    }

    /** Вычитывает из строки следующий аргумент. */
    public String getNextArgument() {
        int endIndex = source.indexOf(spliterator, index);
        if (endIndex == -1) {
            endIndex = source.length();
        }
        if (index == endIndex) {
            return "";
        }
        String command = source.substring(index, endIndex);
        index = endIndex;
        while (source.length() > index && source.charAt(index) == spliterator) {
            index++;
        }
        pooledArguments.add(command);
        return command;
    }

    /** Возвращает последний прочитанный аргумент или пустую строку. */
    public String getLastArgument() {
        return pooledArguments.isEmpty() ? "" : pooledArguments.getLast();
    }

    /** Возвращает первый прочитанный аргумент или пустую строку. */
    public String getFirstArgument() {
        return pooledArguments.isEmpty() ? "" : pooledArguments.getFirst();
    }

    /** Сбрасывает строку-источник до первоначального состояния и обнуляет список прочитанных аргументов. */
    public void reset() {
        pooledArguments.clear();
        index = 0;
    }
}
