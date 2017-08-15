package net.sf.l2j.commons.cron;

public enum MatchAllField implements TimeField {
    instance;

    @Override
    public boolean contains(int number) {
        return true;
    }
}
