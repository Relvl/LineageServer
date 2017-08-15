package net.sf.l2j.commons.cron;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** https://github.com/anderswisch/cron-expression */
public abstract class CronExpression {
    public abstract boolean matches(ZonedDateTime t);

    private static final String YEARLY = "0 0 1 1 *";
    private static final String MONTHLY = "0 0 1 * *";
    private static final String WEEKLY = "0 0 * * 0";
    private static final String DAILY = "0 0 * * *";
    private static final String HOURLY = "0 * * * *";
    private static final Map<String, String> ALIASES = new Builder<String, String>()
            .put("yearly", YEARLY)
            .put("annually", YEARLY)
            .put("monthly", MONTHLY)
            .put("weekly", WEEKLY)
            .put("daily", DAILY)
            .put("midnight", DAILY)
            .put("hourly", HOURLY)
            .build();
    private static final Pattern ALIAS_PATTERN = Pattern.compile("[a-z]+");
    private static final boolean DEFAULT_ONE_BASED_DAY_OF_WEEK = false;
    private static final boolean DEFAULT_SECONDS = false;
    private static final boolean DEFAULT_ALLOW_BOTH_DAYS = true;

    public static CronExpression yearly() {
        return parse(YEARLY);
    }

    public static CronExpression monthly() {
        return parse(MONTHLY);
    }

    public static CronExpression weekly() {
        return parse(WEEKLY);
    }

    public static CronExpression daily() {
        return parse(DAILY);
    }

    public static CronExpression hourly() {
        return parse(HOURLY);
    }

    public static boolean isValid(String s) {
        return isValid(s, DEFAULT_ONE_BASED_DAY_OF_WEEK, DEFAULT_SECONDS, DEFAULT_ALLOW_BOTH_DAYS);
    }

    public static CronExpression parse(String s) {
        return parse(s, DEFAULT_ONE_BASED_DAY_OF_WEEK, DEFAULT_SECONDS, DEFAULT_ALLOW_BOTH_DAYS);
    }

    private static boolean isValid(String s, boolean oneBasedDayOfWeek, boolean seconds, boolean allowBothDays) {
        boolean valid = false;
        try {
            parse(s, oneBasedDayOfWeek, seconds, allowBothDays);
            valid = true;
        }
        catch (Exception e) {
        }
        return valid;
    }

    private static CronExpression parse(String s, boolean oneBasedDayOfWeek, boolean seconds, boolean allowBothDays) {
        Preconditions.checkNotNull(s);
        if (s.charAt(0) == '@') {
            Matcher aliasMatcher = ALIAS_PATTERN.matcher(s);
            if (aliasMatcher.find(1)) {
                String alias = aliasMatcher.group();
                if (ALIASES.containsKey(alias)) { return new DefaultCronExpression(ALIASES.get(alias), DEFAULT_ONE_BASED_DAY_OF_WEEK, DEFAULT_SECONDS, DEFAULT_ALLOW_BOTH_DAYS); }
                else if ("reboot".equals(alias)) { return new RebootCronExpression(); }
            }
        }
        return new DefaultCronExpression(s, seconds, oneBasedDayOfWeek, allowBothDays);
    }

    public static Parser parser() {
        return new Parser();
    }

    public static final class Parser {
        private boolean oneBasedDayOfWeek;
        private boolean seconds;
        private boolean allowBothDays;

        private Parser() {
            oneBasedDayOfWeek = DEFAULT_ONE_BASED_DAY_OF_WEEK;
            seconds = DEFAULT_SECONDS;
            allowBothDays = DEFAULT_ALLOW_BOTH_DAYS;
        }

        public boolean isValid(String s) {
            return CronExpression.isValid(s, oneBasedDayOfWeek, seconds, allowBothDays);
        }

        public CronExpression parse(String s) {
            return CronExpression.parse(s, oneBasedDayOfWeek, seconds, allowBothDays);
        }

        public Parser withOneBasedDayOfWeek(boolean oneBasedDayOfWeek) {
            this.oneBasedDayOfWeek = oneBasedDayOfWeek;
            return this;
        }

        public Parser withSecondsField(boolean secondsField) {
            this.seconds = secondsField;
            return this;
        }

        public Parser allowBothDayFields(boolean allowBothDayFields) {
            this.allowBothDays = allowBothDayFields;
            return this;
        }
    }
}
