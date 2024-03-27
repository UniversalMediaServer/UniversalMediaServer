/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.PMS;
import net.pms.encoders.FFMpegVideo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FFmpegWebFilters {
	private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegWebFilters.class);
	protected static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	/**
	 * Must be used to protect all access to {@link #excludes}, {@link #autoOptions} and {@link #replacements}
	 */
	private static final ReentrantReadWriteLock FILTERS_LOCK = new ReentrantReadWriteLock();

	static {
		readWebFilters(CONFIGURATION.getProfileDirectory() + File.separator + "ffmpeg.webfilters");
	}

	private FFmpegWebFilters() { }


	/**
	 * All access must be protected with {@link #filtersLock}
	 */
	private static final PatternMap<Object> EXCLUDES = new PatternMap<>();

	/**
	 * All access must be protected with {@link #filtersLock}
	 */
	private static final PatternMap<ArrayList> AUTO_OPTIONS = new PatternMap<ArrayList>() {
		private static final long serialVersionUID = 5225786297932747007L;

		@Override
		public ArrayList add(String key, Object value) {
			return put(key, (ArrayList) FFMpegVideo.parseOptions((String) value));
		}
	};

	/**
	 * All access must be protected with {@link #filtersLock}
	 */
	private static final PatternMap<String> REPLACEMENTS = new PatternMap<>();

	private static boolean readWebFilters(String filename) {
		String line;
		try {
			LineIterator it = FileUtils.lineIterator(new File(filename));
			FILTERS_LOCK.writeLock().lock();
			try {
				PatternMap<?> filter = null;
				while (it.hasNext()) {
					line = it.nextLine().trim();
					if (line.equals("EXCLUDE")) {
						filter = EXCLUDES;
					} else if (line.equals("OPTIONS")) {
						filter = AUTO_OPTIONS;
					} else if (line.equals("REPLACE")) {
						filter = REPLACEMENTS;
					} else if (!line.isEmpty() && !line.startsWith("#") && filter != null) {
						String[] value = line.split(" \\| ", 2);
						filter.add(value[0], value.length > 1 ? value[1] : null);
					}
				}
				return true;
			} finally {
				FILTERS_LOCK.writeLock().unlock();
				it.close();
			}
		} catch (FileNotFoundException e) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.info("FFmpeg web filters \"{}\" not found, web filters ignored: {}", filename, e.getMessage());
			} else {
				LOGGER.info("FFmpeg web filters \"{}\" not found, web filters ignored", filename);
			}
		} catch (IOException e) {
			LOGGER.debug("Error reading ffmpeg web filters from file \"{}\": {}", filename, e.getMessage());
			LOGGER.trace("", e);
		}
		return false;
	}

	public static boolean isExluded(String value) {
		FILTERS_LOCK.readLock().lock();
		try {
			return EXCLUDES.match(value) != null;
		} finally {
			FILTERS_LOCK.readLock().unlock();
		}
	}

	public static List<String> getAutoOptions(String value) {
		FILTERS_LOCK.readLock().lock();
		try {
			String match = AUTO_OPTIONS.match(value);
			if (match != null) {
				return AUTO_OPTIONS.get(match);
			}
		} finally {
			FILTERS_LOCK.readLock().unlock();
		}
		return null;
	}

	public static String getReplacements(String value) {
		FILTERS_LOCK.readLock().lock();
		try {
			String r = REPLACEMENTS.match(value);
			if (r != null) {
				value = value.replaceAll(r, REPLACEMENTS.get(r));
				LOGGER.debug("Modified url: {}", value);
			}
		} finally {
			FILTERS_LOCK.readLock().unlock();
		}
		return value;
	}

	// A HashMap that reports whether it's been modified
	// (necessary because 'modCount' isn't accessible outside java.util)
	private static class ModAwareHashMap<K, V> extends HashMap<K, V> {
		private static final long serialVersionUID = -5334451082377480129L;
		protected boolean modified = false;

		@Override
		public void clear() {
			modified = true;
			super.clear();
		}

		@Override
		public V put(K key, V value) {
			modified = true;
			return super.put(key, value);
		}

		@Override
		public V remove(Object key) {
			modified = true;
			return super.remove(key);
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof ModAwareHashMap<?, ?> m)) {
				return false;
			}
			if (m.size() != size()) {
				return false;
			}
			if (m.modified != modified) {
				return false;
			}

			try {
				for (Entry<K, V> e : entrySet()) {
					K key = e.getKey();
					V value = e.getValue();
					if (value == null) {
						if (!(m.get(key) == null && m.containsKey(key))) {
							return false;
						}
					} else {
						if (!value.equals(m.get(key))) {
							return false;
						}
					}
				}
			} catch (ClassCastException | NullPointerException unused) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			return super.hashCode() + (modified ? 1 : 0);
		}
	}

	// A self-combining map of regexes that recompiles if modified
	private static class PatternMap<T> extends ModAwareHashMap<String, T> {
		private static final long serialVersionUID = 3096452459003158959L;
		Matcher combo;
		List<String> groupmap = new ArrayList<>();

		public T add(String key, Object value) {
			return put(key, (T) value);
		}

		// Returns the first matching regex
		String match(String str) {
			if (!isEmpty()) {
				if (modified) {
					compile();
				}
				if (combo.reset(str).find()) {
					for (int i = 0; i < combo.groupCount(); i++) {
						if (combo.group(i + 1) != null) {
							return groupmap.get(i);
						}
					}
				}
			}
			return null;
		}

		void compile() {
			StringBuilder joined = new StringBuilder();
			groupmap.clear();
			for (String regex : this.keySet()) {
				// add each regex as a capture group
				joined.append("|(").append(regex).append(')');
				// map all subgroups to the parent
				for (int i = 0; i < Pattern.compile(regex).matcher("").groupCount() + 1; i++) {
					groupmap.add(regex);
				}
			}
			// compile the combined regex
			combo = Pattern.compile(joined.substring(1)).matcher("");
			modified = false;
		}
	}

}
