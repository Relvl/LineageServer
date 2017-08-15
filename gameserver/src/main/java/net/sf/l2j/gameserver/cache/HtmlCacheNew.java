package net.sf.l2j.gameserver.cache;

import net.sf.l2j.gameserver.util.threading.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * @author Johnson / 06.08.2017
 */
public class HtmlCacheNew implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlCacheNew.class);
    private static final String HTML_DIRECTORY = "data/html/";
    private static final int CACHE_LIFETIME = 10 * 60 * 1000;
    private static final long MAX_CACHEABLE_FILE_SIZE = 512 * 1024;
    private static final long FILECACHE_PURGE_DELAY = 60 * 1000;

    @SuppressWarnings("RedundantIfStatement")
    private static final Predicate<CachedFile> REMOVE_PREDICATE = cachedFile -> {
        if (System.currentTimeMillis() - cachedFile.lastCacheAccess > CACHE_LIFETIME) {return true;}
        if (!cachedFile.exists()) {return true;}
        if (cachedFile.lastFileModified != cachedFile.lastModified()) {return true;}
        return false;
    };

    private final Map<String, CachedFile> cache = new ConcurrentHashMap<>();

    private HtmlCacheNew() {
        ThreadPoolManager.getInstance().scheduleAiAtFixedRate(this, FILECACHE_PURGE_DELAY, FILECACHE_PURGE_DELAY);
    }

    public static HtmlCacheNew getInstance() { return SingletonHolder.INSTANCE; }

    public String getHtml(String fileName) {
        if (fileName == null || fileName.isEmpty()) {return null;}
        if (!fileName.startsWith(HTML_DIRECTORY)) {
            fileName = HTML_DIRECTORY + fileName;
        }
        CachedFile file;
        if (cache.containsKey(fileName)) {
            file = cache.get(fileName);
            if (REMOVE_PREDICATE.test(file)) { cache.remove(fileName); }
            else { return file.getContent(); }
        }
        file = new CachedFile(fileName);
        if (file.exists()) {
            cache.put(fileName, file);
            return file.getContent();
        }
        return null;
    }

    @Override
    public void run() {
        int oldLenght = cache.size();
        cache.values().removeIf(REMOVE_PREDICATE);
        if (cache.size() < oldLenght) {
            LOGGER.info("HtmlCache parged {} files", oldLenght - cache.size());
        }
    }

    private static final class CachedFile extends File {
        private static final long serialVersionUID = -2554320991151301691L;
        private static final Pattern PTN_RETURNS = Pattern.compile("[\\r\\n]");
        private static final Pattern PTN_INLINE = Pattern.compile("\\s{2,}");
        private final long lastFileModified;
        private final String fileName;
        private long lastCacheAccess = System.currentTimeMillis();
        private String fileContents;

        private CachedFile(String pathname) {
            super(pathname);
            fileName = pathname;
            lastFileModified = lastModified();
        }

        private String getContent() {
            lastCacheAccess = System.currentTimeMillis();
            if (fileContents != null) {
                return fileContents;
            }
            try (InputStreamReader isr = new FileReader(this); BufferedReader br = new BufferedReader(isr)) {
                LOGGER.debug("Loading file to cache: {}", fileName);
                String line;
                StringBuilder sb = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                if (sb.length() <= MAX_CACHEABLE_FILE_SIZE) {
                    fileContents = PTN_INLINE.matcher(PTN_RETURNS.matcher(sb.toString()).replaceAll("")).replaceAll("");
                    return fileContents;
                }
                return sb.toString();
            }
            catch (IOException e) {
                LOGGER.error("Cannot read cached file!", e);
                return null;
            }
        }
    }

    private static final class SingletonHolder {
        private static final HtmlCacheNew INSTANCE = new HtmlCacheNew();
    }
}
