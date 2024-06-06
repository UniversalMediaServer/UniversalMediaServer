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
package net.pms.configuration.old;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.configuration.sharedcontent.ApertureContent;
import net.pms.configuration.sharedcontent.FeedAudioContent;
import net.pms.configuration.sharedcontent.FeedImageContent;
import net.pms.configuration.sharedcontent.FeedVideoContent;
import net.pms.configuration.sharedcontent.FolderContent;
import net.pms.configuration.sharedcontent.IPhotoContent;
import net.pms.configuration.sharedcontent.ITunesContent;
import net.pms.configuration.sharedcontent.SharedContent;
import net.pms.configuration.sharedcontent.SharedContentArray;
import net.pms.configuration.sharedcontent.StreamAudioContent;
import net.pms.configuration.sharedcontent.StreamVideoContent;
import net.pms.configuration.sharedcontent.VirtualFolderContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OldConfigurationImporter {
	private static final Logger LOGGER = LoggerFactory.getLogger(OldConfigurationImporter.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final String KEY_FOLDERS = "folders";
	private static final String KEY_FOLDERS_IGNORED = "folders_ignored";
	private static final String KEY_FOLDERS_MONITORED = "folders_monitored";
	private static final String KEY_ITUNES_LIBRARY_PATH = "itunes_library_path";
	private static final String KEY_SHOW_APERTURE_LIBRARY = "show_aperture_library";
	private static final String KEY_SHOW_IPHOTO_LIBRARY = "show_iphoto_library";
	private static final String KEY_SHOW_ITUNES_LIBRARY = "show_itunes_library";

	/**
	 * This class is not meant to be instantiated.
	 */
	private OldConfigurationImporter() {}

	public static synchronized SharedContentArray getOldConfigurations() {
		SharedContentArray result = new SharedContentArray();
		List<Path> sharedFolders = getSharedFolders();
		if (!sharedFolders.isEmpty()) {
			for (Path sharedFolder : sharedFolders) {
				File file = sharedFolder.toFile();
				boolean monitored = getMonitoredFolders().contains(sharedFolder);
				result.add(new FolderContent(file, monitored));
			}
		}
		List<MapFileConfiguration> virtualsFolders = MapFileConfiguration.parseVirtualFolders();
		if (virtualsFolders != null && !virtualsFolders.isEmpty()) {
			for (MapFileConfiguration virtualsFolder : virtualsFolders) {
				result.add(getVirtualFolderFromMapFileConfiguration(virtualsFolder));
			}
		}
		List<WebSourcesConfiguration.WebSource> webSources = WebSourcesConfiguration.getWebSourcesFromConfiguration();
		if (webSources != null && !webSources.isEmpty()) {
			for (WebSourcesConfiguration.WebSource webSource : webSources) {
				String sourceType = webSource.getSourceType();
				switch (sourceType) {
					case "audiofeed" -> result.add(new FeedAudioContent(webSource.getFolderName().replace(",", "/"), webSource.getResourceName(), webSource.getUri()));
					case "imagefeed" -> result.add(new FeedImageContent(webSource.getFolderName().replace(",", "/"), webSource.getResourceName(), webSource.getUri()));
					case "videofeed" -> result.add(new FeedVideoContent(webSource.getFolderName().replace(",", "/"), webSource.getResourceName(), webSource.getUri()));
					case "audiostream" -> result.add(new StreamAudioContent(webSource.getFolderName().replace(",", "/"), webSource.getResourceName(), webSource.getUri(), webSource.getThumbnail()));
					case "videostream" -> result.add(new StreamVideoContent(webSource.getFolderName().replace(",", "/"), webSource.getResourceName(), webSource.getUri(), webSource.getThumbnail()));
					default -> {
						//do nothing
					}
				}
			}
		}
		ensureiPhotoSettings(result);
		ensureApertureSettings(result);
		ensureiTunesSettings(result);
		return result;
	}

	private static VirtualFolderContent getVirtualFolderFromMapFileConfiguration(MapFileConfiguration virtualsFolder) {
		String name = virtualsFolder.getName();
		List<SharedContent> sharedContents = new ArrayList<>();
		for (File file : virtualsFolder.getFiles()) {
			sharedContents.add(new FolderContent(file));
		}
		for (MapFileConfiguration child : virtualsFolder.getChildren()) {
			sharedContents.add(getVirtualFolderFromMapFileConfiguration(child));
		}
		return new VirtualFolderContent(null, name, sharedContents, virtualsFolder.isAddToMediaLibrary());
	}

	/**
	 * Transforms a comma-separated list of directory entries into an
	 * {@link ArrayList} of {@link Path}s. Verifies that the folder exists and
	 * is valid.
	 *
	 * @param key the {@link Configuration} key to read.
	 * @return The {@link List} of folders or {@code null}.
	 */
	@Nonnull
	private static ArrayList<Path> getFolders(String key) {
		String foldersString = CONFIGURATION.getString(key, null);

		ArrayList<Path> folders = new ArrayList<>();
		if (foldersString == null || foldersString.length() == 0) {
			return folders;
		}

		String[] foldersArray = foldersString.trim().split("\\s*,\\s*");

		for (String folder : foldersArray) {
			/*
			 * Unescape embedded commas. Note: Backslashing isn't safe as it
			 * conflicts with the Windows path separator.
			 */
			folder = folder.replace("&comma;", ",");

			if (KEY_FOLDERS.equals(key)) {
				LOGGER.info("Checking shared folder: \"{}\"", folder);
			}

			Path path = Paths.get(folder);
			if (Files.exists(path)) {
				if (!Files.isDirectory(path)) {
					if (KEY_FOLDERS.equals(key)) {
						LOGGER.warn(
							"The \"{}\" is not a folder! Please remove it from your shared folders " +
							"list on the \"Shared Content\" tab or in the configuration file.",
							folder
						);
					} else {
						LOGGER.debug("The \"{}\" is not a folder - check the configuration for key \"{}\"", folder, key);
					}
				}
			} else if (KEY_FOLDERS.equals(key)) {
				LOGGER.warn(
					"\"{}\" does not exist. Please remove it from your shared folders " +
					"list on the \"Shared Content\" tab or in the configuration file.",
					folder
				);
			} else {
				LOGGER.debug("\"{}\" does not exist - check the configuration for key \"{}\"", folder, key);
			}

			// add the path even if there are problems so that the user can update the shared folders as required.
			folders.add(path);
		}

		return folders;
	}

	/**
	 * @return The {@link List} of {@link Path}s of shared folders.
	 */
	private static List<Path> getSharedFolders() {
		return getFolders(KEY_FOLDERS);
	}

	/**
	 * @return The {@link List} of {@link Path}s of monitored folders.
	 */
	private static List<Path> getMonitoredFolders() {
		return getFolders(KEY_FOLDERS_MONITORED);
	}

	/**
	 * @return The {@link List} of {@link Path}s of ignored folders.
	 */
	private static List<Path> getIgnoredFolders() {
		return getFolders(KEY_FOLDERS_IGNORED);
	}

	public static boolean ensureSettingsChanges(SharedContentArray values) {
		boolean updated = OldConfigurationImporter.ensureiPhotoSettings(values);
		updated |= OldConfigurationImporter.ensureApertureSettings(values);
		updated |= OldConfigurationImporter.ensureiTunesSettings(values);
		return updated;
	}

	private static boolean ensureiPhotoSettings(SharedContentArray values) {
		boolean updated = false;
		if (CONFIGURATION.getBoolean(KEY_SHOW_IPHOTO_LIBRARY, false)) {
			boolean iphotoFound = false;
			for (SharedContent value : values) {
				if (value instanceof IPhotoContent) {
					iphotoFound = true;
					break;
				}
			}
			if (!iphotoFound) {
				values.add(new IPhotoContent());
				updated = true;
			}
			CONFIGURATION.getConfiguration().clearProperty(KEY_SHOW_IPHOTO_LIBRARY);
		}
		return updated;
	}

	private static boolean ensureApertureSettings(SharedContentArray values) {
		boolean updated = false;
		if (CONFIGURATION.getBoolean(KEY_SHOW_APERTURE_LIBRARY, false)) {
			boolean apertureFound = false;
			for (SharedContent value : values) {
				if (value instanceof ApertureContent) {
					apertureFound = true;
					break;
				}
			}
			if (!apertureFound) {
				values.add(new ApertureContent());
				updated = true;
			}
			CONFIGURATION.getConfiguration().clearProperty(KEY_SHOW_APERTURE_LIBRARY);
		}
		return updated;
	}

	private static boolean ensureiTunesSettings(SharedContentArray values) {
		boolean updated = false;
		if (CONFIGURATION.getBoolean(KEY_SHOW_ITUNES_LIBRARY, false)) {
			String path = CONFIGURATION.getString(KEY_ITUNES_LIBRARY_PATH, "");
			boolean itunesFound = false;
			for (SharedContent value : values) {
				if (value instanceof ITunesContent iTunesContent && path.equals(iTunesContent.getPath())) {
					itunesFound = true;
					break;
				}
			}
			if (!itunesFound) {
				values.add(new ITunesContent(path));
				updated = true;
			}
			CONFIGURATION.getConfiguration().clearProperty(KEY_SHOW_ITUNES_LIBRARY);
			CONFIGURATION.getConfiguration().clearProperty(KEY_ITUNES_LIBRARY_PATH);
		}
		return updated;
	}

}
