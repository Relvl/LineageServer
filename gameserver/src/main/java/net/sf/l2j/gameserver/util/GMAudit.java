package net.sf.l2j.gameserver.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GMAudit {
    private static final Logger LOGGER = LoggerFactory.getLogger(GMAudit.class);

    static {
        new File("log/GMAudit").mkdirs();
    }

    public static void auditGMAction(String gmName, String action, String target, String params) {
        final File file = new File("log/GMAudit/" + gmName + ".txt");
        if (!file.exists()) {
            try {
                file.createNewFile();
            }
            catch (IOException e) {
            }
        }

        try (FileWriter save = new FileWriter(file, true)) {
            save.write(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date()) + ">" + gmName + ">" + action + ">" + target + ">" + params + "\r\n");
        }
        catch (IOException e) {
            LOGGER.error("GMAudit for GM {} could not be saved: ", gmName, e);
        }
    }

    public static void auditGMAction(String gmName, String action, String target) {
        auditGMAction(gmName, action, target, "");
    }
}