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
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.configuration.old.OldConfigurationImporter;
import net.pms.network.webguiserver.servlets.SseApiServlet;
import net.pms.platform.PlatformUtils;
import net.pms.util.FileWatcher;
import org.apache.commons.lang.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SharedContentConfiguration {
	private static final Logger LOGGER = LoggerFactory.getLogger(SharedContentConfiguration.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	protected static final Gson GSON = new GsonBuilder()
		.setPrettyPrinting()
		.registerTypeAdapter(SharedContent.class, new SharedContentTypeAdapter())
		.registerTypeAdapter(File.class, new FileTypeAdapter())
		.create();
	private static final List<SharedContentListener> LISTENERS = new ArrayList<>();
	private static final SharedContentArray SHARED_CONTENT_ARRAY = new SharedContentArray();

	// Automatic reloading
	private static boolean isWritingConfiguration = false;
	public static final FileWatcher.Listener RELOAD_WATCHER = (String filename, String event, FileWatcher.Watch watch, boolean isDir) -> reloadConfiguration();

	static {
		readAllConfigurations();
		FileWatcher.add(new FileWatcher.Watch(CONFIGURATION.getSharedConfPath(), RELOAD_WATCHER));
	}

	/**
	 * This class is not meant to be instantiated.
	 */
	private SharedContentConfiguration() {}

	/**
	 * This return SharedContent's List.
	 */
	public static SharedContentArray getSharedContentArray() {
		synchronized (SHARED_CONTENT_ARRAY) {
			return (SharedContentArray) SerializationUtils.clone(SHARED_CONTENT_ARRAY);
		}
	}

	/**
	 * Get all shared directories including virtual folders.
	 */
	public static List<File> getSharedFolders() {
		synchronized (SHARED_CONTENT_ARRAY) {
			return getSharedFolders(SHARED_CONTENT_ARRAY);
		}
	}

	public static List<File> getSharedFolders(List<SharedContent> sharedContents) {
		List<File> files = new ArrayList<>();
		for (SharedContent sharedContent : sharedContents) {
			if (sharedContent instanceof FolderContent folder && folder.getFile() != null) {
				files.add(folder.getFile());
			} else if (sharedContent instanceof VirtualFolderContent folders && folders.getChilds() != null) {
				files.addAll(getSharedFolders(folders.getChilds()));
			}
		}
		return files;
	}

	/**
	 * Get all monitored directories including virtual folders.
	 */
	public static List<File> getMonitoredFolders() {
		synchronized (SHARED_CONTENT_ARRAY) {
			return getMonitoredFolders(SHARED_CONTENT_ARRAY);
		}
	}

	public static List<File> getMonitoredFolders(List<SharedContent> sharedContents) {
		List<File> files = new ArrayList<>();
		for (SharedContent sharedContent : sharedContents) {
			if (sharedContent instanceof FolderContent folder && folder.isMonitored() && folder.getFile() != null) {
				files.add(folder.getFile());
			} else if (sharedContent instanceof VirtualFolderContent folders && folders.getChilds() != null && !folders.getChilds().isEmpty()) {
				files.addAll(getMonitoredFolders(folders.getChilds()));
			}
		}
		return files;
	}

	public static void addListener(SharedContentListener listener) {
		synchronized (LISTENERS) {
			LISTENERS.add(listener);
			listener.updateSharedContent();
		}
	}

	public static void updateSharedContent(SharedContentArray values, boolean save) {
		LOGGER.debug("New shared content configuration sent.");
		boolean updated = false;
		synchronized (SHARED_CONTENT_ARRAY) {
			if (!values.equals(SHARED_CONTENT_ARRAY)) {
				LOGGER.debug("Updating shared content configuration");
				SHARED_CONTENT_ARRAY.clear();
				//check viability
				for (SharedContent sharedContent : values) {
					if (sharedContent instanceof FolderContent folderContent) {
						if (folderContent.getFile() != null) {
							SHARED_CONTENT_ARRAY.add(sharedContent);
						}
					} else {
						SHARED_CONTENT_ARRAY.add(sharedContent);
					}
				}
				if (save) {
					writeConfiguration();
				}
				sendSseApiUpdate();
				if (PMS.isReady()) {
					PMS.get().resetRenderersRoot();
				}
				updated = true;
			} else {
				LOGGER.debug("Current shared content configuration is already up to date.");
			}
		}
		if (updated) {
			synchronized (LISTENERS) {
				for (SharedContentListener listener : LISTENERS) {
					if (listener != null) {
						listener.updateSharedContent();
					}
				}
			}
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

	public static void setFromJsonArray(JsonArray jsonArray) {
		SharedContentArray values = GSON.fromJson(jsonArray, SharedContentArray.class);
		updateSharedContent(values, true);
	}

	/**
	 * This just preserves wizard functionality of offering the user a choice
	 * to share a directory.
	 *
	 * @param directory
	 */
	public static void addFolderShared(File directory) {
		SharedContentArray values = getSharedContentArray();
		values.add(new FolderContent(directory));
		updateSharedContent(values, true);
	}

	/**
	 * Set Shared contents configuration.
	 * First, parse shared contents configs file (default to SHARED.conf).
	 * If not exists, try to update from old shared contents configs files (WEB.conf, VirtualFolders.conf and UMS.conf)
	 * If no results, default to the default media folders on your computer.
	 * That is:
	 * On macOS:
	 *    - /user/Movies
	 *    - /user/Music
	 *    - /user/Pictures
	 *  On Windows:
	 *    - /user/Music
	 *    - /user/Pictures
	 *    - /user/Videos
	 *  On Linux:
	 *    - /user
	 */
	private static synchronized void readAllConfigurations() {
		Path sharedConfFilePath = Paths.get(CONFIGURATION.getSharedConfPath());
		try {
			if (Files.exists(sharedConfFilePath)) {
				LOGGER.info("Getting shared content from configuration file : " + sharedConfFilePath);
				String json = Files.readString(sharedConfFilePath, StandardCharsets.UTF_8);
				updateSharedContent(GSON.fromJson(json, SharedContentArray.class), false);
			} else {
				//import old settings
				LOGGER.info("Importing old shared content configuration files");
				SharedContentArray oldConfig = OldConfigurationImporter.getOldConfigurations();
				if (oldConfig.isEmpty()) {
					//no shared conf files, set to default media folders
					oldConfig = defaultConfiguration();
				}
				updateSharedContent(oldConfig, true);
			}
		} catch (IOException | JsonSyntaxException ex) {
			LOGGER.info("Error in shared content configuration file : " + ex.getMessage());
			LOGGER.debug(null, ex);
			updateSharedContent(new SharedContentArray(), false);
		}
	}

	private static synchronized SharedContentArray readConfiguration() {
		Path sharedConfFilePath = Paths.get(CONFIGURATION.getSharedConfPath());
		LOGGER.debug("Reading shared content configuration file: " + sharedConfFilePath);
		try {
			if (Files.exists(sharedConfFilePath)) {
				String json = Files.readString(sharedConfFilePath, StandardCharsets.UTF_8);
				return GSON.fromJson(json, SharedContentArray.class);
			}
			LOGGER.trace("Shared content configuration file missing: " + sharedConfFilePath);
		} catch (IOException | JsonSyntaxException ex) {
			LOGGER.info("Error in shared content configuration file : " + ex.getMessage());
			LOGGER.debug(null, ex);
		}
		return new SharedContentArray();
	}

	/**
	 * Gets the shared folders and the monitor folders to the platform default
	 * folders.
	 */
	private static synchronized SharedContentArray defaultConfiguration() {
		SharedContentArray result = new SharedContentArray();
		for (Path path : PlatformUtils.INSTANCE.getDefaultFolders()) {
			File file = path.toFile();
			if (file != null) {
				result.add(new FolderContent(path.toFile()));
			}
		}
		return result;
	}

	private static synchronized void writeConfiguration() {
		try {
			isWritingConfiguration = true;
			Path sharedConfFilePath = Paths.get(CONFIGURATION.getSharedConfPath());
			LOGGER.debug("Writing shared content configuration file: " + sharedConfFilePath);
			Files.writeString(sharedConfFilePath, GSON.toJson(SHARED_CONTENT_ARRAY), StandardCharsets.UTF_8);
		} catch (IOException e) {
			LOGGER.debug("An error occurred while writing the shared content configuration file: {}", e);
		}
		isWritingConfiguration = false;
	}

	private static synchronized void reloadConfiguration() {
		if (!isWritingConfiguration) {
			LOGGER.debug("Reloading shared content configuration file");
			updateSharedContent(readConfiguration(), false);
		}
	}

	private static synchronized void sendSseApiUpdate() {
		JsonObject sharedMessage = new JsonObject();
		sharedMessage.addProperty("action", "set_configuration_changed");
		JsonObject sharedData = new JsonObject();
		sharedData.add("shared_content", getAsJsonArray());
		sharedMessage.add("value", sharedData);
		SseApiServlet.broadcastSharedMessage(sharedMessage.toString());
	}
}
