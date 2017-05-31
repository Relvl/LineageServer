package net.sf.l2j.log;

import java.io.IOException;
import java.util.logging.FileHandler;

@Deprecated
public class ErrorLogHandler extends FileHandler {
    public ErrorLogHandler() throws IOException, SecurityException {
        super();
    }
}