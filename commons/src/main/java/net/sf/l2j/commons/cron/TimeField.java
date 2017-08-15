package net.sf.l2j.commons.cron;

@FunctionalInterface
public interface TimeField {
    boolean contains(int number);
}
