package net.sf.l2j.commons.cron;

import java.time.ZonedDateTime;

public class DefaultCronExpression extends CronExpression {
    private final String string;
    private final TimeField second,
            minute,
            hour,
            month,
            year;
    private final DayOfWeekField dayOfWeek;
    private final DayOfMonthField dayOfMonth;

    protected DefaultCronExpression(String s, boolean seconds, boolean oneBasedDayOfWeek, boolean allowBothDayFields) {
        string = s;
        s = s.toUpperCase();
        Tokens tokens = new Tokens(s);
        if (seconds)
            second = DefaultField.parse(tokens, 0, 59);
        else
            second = MatchAllField.instance;
        minute = DefaultField.parse(tokens, 0, 59);
        hour = DefaultField.parse(tokens, 0, 23);
        dayOfMonth = DayOfMonthField.parse(tokens);
        month = MonthField.parse(tokens);
        dayOfWeek = DayOfWeekField.parse(tokens, oneBasedDayOfWeek);
        if (tokens.hasNext())
            year = DefaultField.parse(tokens, 0, 0);
        else
            year = MatchAllField.instance;
        if (!allowBothDayFields && !dayOfMonth.isUnspecified() && !dayOfWeek.isUnspecified())
            throw new IllegalArgumentException("Day of month and day of week may not both be specified");
    }

    @Override
    public String toString() {
        return string;
    }

    @Override
    public boolean matches(ZonedDateTime t) {
        return second.contains(t.getSecond())
                && minute.contains(t.getMinute())
                && hour.contains(t.getHour())
                && month.contains(t.getMonthValue())
                && year.contains(t.getYear())
                && dayOfWeek.matches(t)
                && dayOfMonth.matches(t);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultCronExpression that = (DefaultCronExpression) o;
        if (dayOfMonth != null ? !dayOfMonth.equals(that.dayOfMonth) : that.dayOfMonth != null) return false;
        if (dayOfWeek != null ? !dayOfWeek.equals(that.dayOfWeek) : that.dayOfWeek != null) return false;
        if (hour != null ? !hour.equals(that.hour) : that.hour != null) return false;
        if (minute != null ? !minute.equals(that.minute) : that.minute != null) return false;
        if (month != null ? !month.equals(that.month) : that.month != null) return false;
        if (second != null ? !second.equals(that.second) : that.second != null) return false;
        if (!string.equals(that.string)) return false;
        if (year != null ? !year.equals(that.year) : that.year != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = string.hashCode();
        result = 31 * result + (second != null ? second.hashCode() : 0);
        result = 31 * result + (minute != null ? minute.hashCode() : 0);
        result = 31 * result + (hour != null ? hour.hashCode() : 0);
        result = 31 * result + (month != null ? month.hashCode() : 0);
        result = 31 * result + (year != null ? year.hashCode() : 0);
        result = 31 * result + (dayOfWeek != null ? dayOfWeek.hashCode() : 0);
        result = 31 * result + (dayOfMonth != null ? dayOfMonth.hashCode() : 0);
        return result;
    }
}
