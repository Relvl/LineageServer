package net.sf.l2j.commons.cron;

public class MonthField extends DefaultField {
    protected MonthField(Builder builder) {
        super(builder);
    }

    public static MonthField parse(Tokens tokens) {
        return new Builder().parse(tokens).build();
    }

    public static class Builder extends DefaultField.Builder {
        protected static final Keywords KEYWORDS = new Keywords();

        static {
            KEYWORDS.put("JAN", 1);
            KEYWORDS.put("FEB", 2);
            KEYWORDS.put("MAR", 3);
            KEYWORDS.put("APR", 4);
            KEYWORDS.put("MAY", 5);
            KEYWORDS.put("JUN", 6);
            KEYWORDS.put("JUL", 7);
            KEYWORDS.put("AUG", 8);
            KEYWORDS.put("SEP", 9);
            KEYWORDS.put("OCT", 10);
            KEYWORDS.put("NOV", 11);
            KEYWORDS.put("DEC", 12);
        }

        public Builder() {
            super(1, 12);
        }

        @Override
        protected Builder parse(Tokens tokens) {
            tokens.keywords(KEYWORDS);
            super.parse(tokens);
            tokens.reset();
            return this;
        }

        @Override
        public MonthField build() {
            return new MonthField(this);
        }
    }
}
