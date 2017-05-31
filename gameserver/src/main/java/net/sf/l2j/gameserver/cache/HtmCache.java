/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.cache;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import net.sf.l2j.commons.io.UnicodeReader;

/**
 * @author Layane, reworked by Java-man and Hasha
 */
public class HtmCache
{
	private static final Logger _log = Logger.getLogger(HtmCache.class.getName());
	
	private final Map<Integer, String> _htmCache;
	private final FileFilter _htmFilter;
	
	public static HtmCache getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected HtmCache()
	{
		_htmCache = new HashMap<>();
		_htmFilter = new HtmFilter();
	}
	
	/**
	 * Cleans HtmCache.
	 */
	public void reload()
	{
		_log.info("HtmCache: Cache cleared, had " + _htmCache.size() + " entries.");
		
		_htmCache.clear();
	}
	
	/**
	 * Reloads given directory. All sub-directories are parsed, all html files are loaded to HtmCache.
	 * @param path : Directory to be reloaded.
	 */
	public void reloadPath(String path)
	{
		parseDir(new File(path));
		_log.info("HtmCache: Reloaded specified " + path + " path.");
	}
	
	/**
	 * Parse given directory, all html files are loaded to HtmCache.
	 * @param dir : Directory to be parsed.
	 */
	private void parseDir(File dir)
	{
		for (File file : dir.listFiles(_htmFilter))
		{
			if (file.isDirectory())
				parseDir(file);
			else
				loadFile(file);
		}
	}
	
	/**
	 * Loads html file content to HtmCache.
	 * @param file : File to be cached.
	 * @return String : Content of the file.
	 */
	private String loadFile(File file)
	{
		try (FileInputStream fis = new FileInputStream(file); UnicodeReader ur = new UnicodeReader(fis, "UTF-8"); BufferedReader br = new BufferedReader(ur))
		{
			final StringBuilder sb = new StringBuilder();
			String line;
			
			while ((line = br.readLine()) != null)
				sb.append(line).append('\n');
			
			final String content = sb.toString().replaceAll("\r\n", "\n");
			
			_htmCache.put(file.getPath().replace("\\", "/").hashCode(), content);
			return content;
		}
		catch (Exception e)
		{
			_log.warning("HtmCache: problem with loading file " + e);
			return null;
		}
	}
	
	/**
	 * Check if an HTM exists and can be loaded. If so, it is loaded into HtmCache.
	 * @param path The path to the HTM
	 * @return true if the HTM can be loaded.
	 */
	public boolean isLoadable(String path)
	{
		final File file = new File(path);
		
		if (file.exists() && _htmFilter.accept(file) && !file.isDirectory())
			return loadFile(file) != null;
		
		return false;
	}
	
	/**
	 * Return content of html message given by filename.
	 * @param filename : Desired html filename.
	 * @return String : Returns content if filename exists, otherwise returns null.
	 */
	public String getHtm(String filename)
	{
		if (filename == null || filename.isEmpty())
			return "";
		
		String content = _htmCache.get(filename.hashCode());
		if (content == null)
		{
			final File file = new File(filename);
			
			if (file.exists() && _htmFilter.accept(file) && !file.isDirectory())
				content = loadFile(file);
		}
		
		return content;
	}
	
	/**
	 * Return content of html message given by filename. In case filename does not exist, returns notice.
	 * @param filename : Desired html filename.
	 * @return String : Returns content if filename exists, otherwise returns notice.
	 */
	public String getHtmForce(String filename)
	{
		String content = getHtm(filename);
		if (content == null)
		{
			content = "<html><body>My html is missing:<br>" + filename + "</body></html>";
			_log.warning("HtmCache: " + filename + " is missing.");
		}
		
		return content;
	}
	
	private static class SingletonHolder
	{
		protected static final HtmCache _instance = new HtmCache();
	}
	
	protected class HtmFilter implements FileFilter
	{
		@Override
		public boolean accept(File file)
		{
			// directories, *.htm and *.html files
			return file.isDirectory() || file.getName().endsWith(".htm") || file.getName().endsWith(".html");
		}
	}
}