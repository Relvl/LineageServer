package net.sf.l2j.commons.cron;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

public class DayOfWeekField extends DefaultField {
    public static final long SECONDS_PER_WEEK = 604800l;
    private final Multimap<Integer, Integer> nth;
    private final Set<Integer> last;
    private final boolean hasNth, hasLast, unspecified;

    private DayOfWeekField(Builder b) {
        super(b);
        this.nth = b.nth.build();
        hasNth = !nth.isEmpty();
        this.last = b.last.build();
        hasLast = !last.isEmpty();
        unspecified = b.unspecified;
    }

    public boolean isUnspecified() {
        return unspecified;
    }

    public boolean matches(ZonedDateTime time) {
        if (unspecified)
            return true;
        final DayOfWeek dayOfWeek = time.getDayOfWeek();
        int number = number(dayOfWeek.getValue());
        if (hasLast) {
            return last.contains(number) && time.getMonth() != time.plusWeeks(1).getMonth();
        } else if (hasNth) {
            int dayOfYear = time.getDayOfYear();
            if (nth.containsKey(number)) {
                for (int possibleMatch : nth.get(number)) {
                    if (dayOfYear == time.with(TemporalAdjusters.dayOfWeekInMonth(possibleMatch, dayOfWeek)).getDayOfYear()) {
                        return true;
                    }
                }
            }
        }
        return contains(number);
    }

    private int number(int dayOfWeek) {
        return dayOfWeek % 7;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        DayOfWeekField that = (DayOfWeekField) o;
        if (hasLast != that.hasLast)
            return false;
        if (hasNth != that.hasNth)
            return false;
        if (!last.equals(that.last))
            return false;
        if (!nth.equals(that.nth))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + nth.hashCode();
        result = 31 * result + last.hashCode();
        result = 31 * result + (hasNth ? 1 : 0);
        result = 31 * result + (hasLast ? 1 : 0);
        return result;
    }

    public static DayOfWeekField parse(Tokens s, boolean oneBased) {
        return new Builder(oneBased).parse(s).build();
    }

    public static class Builder extends DefaultField.Builder {
        protected static final Keywords KEYWORDS = new Keywords();

        static {
            KEYWORDS.put("SUN", 0);
            KEYWORDS.put("MON", 1);
            KEYWORDS.put("TUE", 2);
            KEYWORDS.put("WED", 3);
            KEYWORDS.put("THU", 4);
            KEYWORDS.put("FRI", 5);
            KEYWORDS.put("SAT", 6);
        }

        private boolean oneBased, unspecified;
        private final ImmutableSet.Builder<Integer> last;
        private final ImmutableMultimap.Builder<Integer, Integer> nth;

        public Builder(boolean oneBased) {
            super(0, 6);
            this.oneBased = oneBased;
            last = ImmutableSet.builder();
            nth = ImmutableMultimap.builder();
        }

        @Override
        protected Builder parse(Tokens tokens) {
            tokens.keywords(KEYWORDS);
            if (oneBased)
                tokens.offset(1);
            super.parse(tokens);
            tokens.reset();
            return this;
        }

        @Override
        protected boolean parseValue(Tokens tokens, Token token, int first, int last) {
            if (token == Token.MATCH_ONE) {
                unspecified = true;
                return false;
            } else {
                return super.parseValue(tokens, token, first, last);
            }
        }

        @Override
        protected boolean parseNumber(Tokens tokens, Token token, int first, int last) {
            if (token == Token.LAST) {
                this.last.add(first);
            } else if (token == Token.NTH) {
                int number = nextNumber(tokens);
                if (oneBased)
                    number += 1;
                nth.put(first, number);
            } else {
                return super.parseNumber(tokens, token, first, last);
            }
            return false;
        }

        @Override
        public DayOfWeekField build() {
            return new DayOfWeekField(this);
        }
    }
}
