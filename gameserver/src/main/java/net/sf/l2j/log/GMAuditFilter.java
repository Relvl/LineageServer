package net.sf.l2j.log;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

public class GMAuditFilter implements Filter {
    public static final String GMAUDIT = "gmaudit";

    @Override
    public boolean isLoggable(LogRecord record) {
        return record.getLoggerName().equalsIgnoreCase(GMAUDIT);
    }
}