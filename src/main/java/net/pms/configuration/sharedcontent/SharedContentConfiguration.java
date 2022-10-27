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
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.util.FileWatcher;
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
		FileWatcher.add(new FileWatcher.Watch(CONFIGURATION.getSharedConfPath(), RELOAD_WATCHER));
	}

	/**
	 * This return SharedContent's List.
	 */
	public static SharedContentArray getSharedContentSources() {
		synchronized (SHARED_CONTENT_ARRAY) {
			return SHARED_CONTENT_ARRAY;
		}
	}

	/**
	 * Get all shared directories including virtual folders.
	 */
	public static List<File> getSharedFolders() {
		synchronized (SHARED_CONTENT_ARRAY) {
			List<File> files = new ArrayList<>();
			for (SharedContent sharedContent : SHARED_CONTENT_ARRAY) {
				if (sharedContent instanceof Folder folder && folder.getFile() != null) {
					files.add(folder.getFile());
				} else if (sharedContent instanceof Folders folders && folders.getFolders() != null) {
					for (Folder folder : folders.getFolders()) {
						if (folder!= null && folder.getFile() != null) {
							files.add(folder.getFile());
						}
					}
				}
			}
			return files;
		}
	}

	public static List<File> getMonitoredFolders() {
		synchronized (SHARED_CONTENT_ARRAY) {
			List<File> files = new ArrayList<>();
			for (SharedContent sharedContent : SHARED_CONTENT_ARRAY) {
				if (sharedContent instanceof Folder folder && folder.isMonitored() && folder.getFile() != null) {
					files.add(folder.getFile());
				} else if (sharedContent instanceof Folders folders && folders.getFolders() != null) {
					for (Folder folder : folders.getFolders()) {
						if (folder!= null && folder.isMonitored() && folder.getFile() != null) {
							files.add(folder.getFile());
						}
					}
				}
			}
			return files;
		}
	}

	public static void addListener(SharedContentListener listener) {
		synchronized (LISTENERS) {
			LISTENERS.add(listener);
			listener.updateSharedContent();
		}
	}

	public static synchronized SharedContentArray readConfiguration() {
		Path sharedConfFilePath = Paths.get(CONFIGURATION.getSharedConfPath());
		try {
			if (Files.exists(sharedConfFilePath)) {
				String json = Files.readString(sharedConfFilePath, StandardCharsets.UTF_8);
				return GSON.fromJson(json, SharedContentArray.class);
			} else {
				//import old settings
				SharedContentArray oldConfig = OldConfigurationImporter.getOldConfigurations();
				writeConfiguration(oldConfig);
			}
		} catch (IOException | JsonSyntaxException ex) {
			LOGGER.info("Error in shared content configuration file : " + ex.getMessage());
			LOGGER.debug(null, ex);
		}
		return new SharedContentArray();
	}

	public static synchronized void writeConfiguration(SharedContentArray value) {
		if (SHARED_CONTENT_ARRAY != null && SHARED_CONTENT_ARRAY.equals(value)) {
			return;
		}
		try {
			Path webConfFilePath = Paths.get(CONFIGURATION.getSharedConfPath());
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




	/**
	 * Get shared directories excluding virtual folders.
	 * Needed by old gui only because virtual folders are not implemented there.
	 */
	public static List<Folder> getOldSharedFolders() {
		synchronized (SHARED_CONTENT_ARRAY) {
			List<Folder> folders = new ArrayList<>();
			for (SharedContent sharedContent : SHARED_CONTENT_ARRAY) {
				if (sharedContent instanceof Folder folder && folder.getFile() != null) {
					folders.add(folder);
				}
			}
			return folders;
		}
	}

	/**
	 * Stores the shared folders in the configuration from the specified
	 * value.
	 *
	 * Needed by old gui only because virtual folders are not implemented there.
	 * @param tableSharedFolders the List of SharedFolder values to use.
	 */
	public static void setOldSharedFolders(List<Folder> tableSharedFolders) {

	}

	public static void setSharedContentConfiguration(List<SharedContent> tableSharedContent) {

	}

	private static boolean folderPathInList(Folder folder, List<Folder> folders) {
		if (folder != null && folder.getFile() != null) {
			for (Folder rowFolder : folders) {
				if (rowFolder != null && rowFolder.getFile() != null && folder.getFile().getPath().equals(rowFolder.getFile().getPath())) {
					return true;
				}
			}
		}
		return false;
	}
}
