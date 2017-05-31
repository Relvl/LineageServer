package net.sf.l2j.log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Log {
    private static final Logger _log = Logger.getLogger(Log.class.getName());

    public static final void add(String text, String cat) {
        String date = (new SimpleDateFormat("yy.MM.dd H:mm:ss")).format(new Date());
        String curr = (new SimpleDateFormat("yyyy-MM-dd-")).format(new Date());
        new File("log/game").mkdirs();

        final File file = new File("log/game/" + (curr != null ? curr : "") + (cat != null ? cat : "unk") + ".txt");
        try (FileWriter save = new FileWriter(file, true)) {
            String out = "[" + date + "] " + text + "\n";
            save.write(out);
        } catch (IOException e) {
            _log.log(Level.WARNING, "Error saving logfile: ", e);
        }
    }
}