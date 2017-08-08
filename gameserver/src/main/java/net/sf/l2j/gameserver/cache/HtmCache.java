package net.sf.l2j.gameserver.cache;

import net.sf.l2j.commons.io.UnicodeReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

@Deprecated
public class HtmCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(HtmCache.class);

    private final Map<Integer, String> _htmCache;
    private final FileFilter _htmFilter;

    public static HtmCache getInstance() {
        return SingletonHolder._instance;
    }

    protected HtmCache() {
        _htmCache = new HashMap<>();
        _htmFilter = new HtmFilter();
    }

    /**
     * Loads html file content to HtmCache.
     *
     * @param file : File to be cached.
     * @return String : Content of the file.
     */
    private String loadFile(File file) {
        try (FileInputStream fis = new FileInputStream(file); UnicodeReader ur = new UnicodeReader(fis, "UTF-8"); BufferedReader br = new BufferedReader(ur)) {
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) { sb.append(line).append('\n'); }

            String content = sb.toString().replaceAll("\r\n", "\n");

            _htmCache.put(file.getPath().replace("\\", "/").hashCode(), content);
            return content;
        }
        catch (Exception e) {
            LOGGER.error("HtmCache: problem with loading file ", e);
            return null;
        }
    }

    /**
     * Check if an HTM exists and can be loaded. If so, it is loaded into HtmCache.
     *
     * @param path The path to the HTM
     * @return true if the HTM can be loaded.
     */
    public boolean isLoadable(String path) {
        File file = new File(path);

        if (file.exists() && _htmFilter.accept(file) && !file.isDirectory()) { return loadFile(file) != null; }

        return false;
    }

    /**
     * Return content of html message given by filename.
     *
     * @param filename : Desired html filename.
     * @return String : Returns content if filename exists, otherwise returns null.
     */
    public String getHtm(String filename) {
        if (filename == null || filename.isEmpty()) { return ""; }
        String content = _htmCache.get(filename.hashCode());
        if (content == null) {
            File file = new File(filename);
            if (file.exists() && _htmFilter.accept(file) && !file.isDirectory()) { content = loadFile(file); }
        }
        return content;
    }

    /**
     * Return content of html message given by filename. In case filename does not exist, returns notice.
     *
     * @param filename : Desired html filename.
     * @return String : Returns content if filename exists, otherwise returns notice.
     */
    public String getHtmForce(String filename) {
        String content = getHtm(filename);
        if (content == null) {
            content = "<html><body>My html is missing:<br>" + filename + "</body></html>";
            LOGGER.warn("HtmCache: {} is missing.", filename);
        }

        return content;
    }

    private static class SingletonHolder {
        protected static final HtmCache _instance = new HtmCache();
    }

    protected class HtmFilter implements FileFilter {
        @Override
        public boolean accept(File file) {
            // directories, *.htm and *.html files
            return file.isDirectory() || file.getName().endsWith(".htm") || file.getName().endsWith(".html");
        }
    }
}