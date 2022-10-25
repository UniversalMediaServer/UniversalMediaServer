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
package net.pms.configuration.sharedcontent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.util.FileWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SharedContentConfiguration {
	private static final Logger LOGGER = LoggerFactory.getLogger(SharedContentConfiguration.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final List<SharedContentListener> LISTENERS = new ArrayList<>();
	private static final SharedContentArray SHARED_CONTENT_ARRAY = readConfiguration();

	// Automatic reloading
	public static final FileWatcher.Listener RELOAD_WATCHER = (String filename, String event, FileWatcher.Watch watch, boolean isDir) -> {
		synchronized (SHARED_CONTENT_ARRAY) {
			SHARED_CONTENT_ARRAY.clear();
			SHARED_CONTENT_ARRAY.addAll(readConfiguration());
			synchronized (LISTENERS) {
				for (SharedContentListener listener : LISTENERS) {
					if (listener != null) {
						listener.updateSharedContent();
					}
				}
			}
		}
	};
	static {
		FileWatcher.add(new FileWatcher.Watch(CONFIGURATION.getWebConfPath(), RELOAD_WATCHER));
	}

	/**
	 * This return SharedContent's List.
	 */
	public static SharedContentArray getSharedContentSources() {
		synchronized (SHARED_CONTENT_ARRAY) {
			return SHARED_CONTENT_ARRAY;
		}
	}

	public static void addListener(SharedContentListener listener) {
		synchronized (LISTENERS) {
			LISTENERS.add(listener);
			listener.updateSharedContent();
		}
	}

	public static synchronized SharedContentArray readConfiguration() {
		Path sharedConfFilePath = Paths.get(CONFIGURATION.getWebConfPath());
		try {
			if (Files.exists(sharedConfFilePath)) {
				String json = Files.readString(sharedConfFilePath, StandardCharsets.UTF_8);
				return GSON.fromJson(json, SharedContentArray.class);
			}
		} catch (IOException | JsonSyntaxException ex) {
			LOGGER.info("Error in shared content configuration file : " + ex.getMessage());
			LOGGER.debug(null, ex);
		}
		return new SharedContentArray();
	}

	public static synchronized void writeConfiguration(SharedContentArray value) {
		if (SHARED_CONTENT_ARRAY.equals(value)) {
			return;
		}
		try {
			Path webConfFilePath = Paths.get(CONFIGURATION.getWebConfPath());
			Files.writeString(webConfFilePath, GSON.toJson(value), StandardCharsets.UTF_8);
		} catch (IOException e) {
			LOGGER.debug("An error occurred while writing the web config file: {}", e);
		}
	}

	/**
	 * This returns the shared content configuration as a JSON array.
	 *
	 * @return
	 */
	public static synchronized JsonArray getAsJsonArray() {
		return GSON.toJsonTree(SHARED_CONTENT_ARRAY).getAsJsonArray();
	}

	public interface SharedContentListener {
		public void updateSharedContent();
	}
}
