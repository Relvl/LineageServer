package net.sf.l2j.log;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class GMAuditFormatter extends Formatter {
    @Override
    public String format(LogRecord record) {
        return record.getMessage() + "\r\n";
    }
}