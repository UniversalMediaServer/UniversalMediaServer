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
package net.pms.store.container;

import com.sun.jna.Platform;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import net.pms.renderers.Renderer;
import net.pms.store.StoreContainer;
import net.pms.store.item.RealFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xmlwise.Plist;
import xmlwise.XmlParseException;

public class IPhotoLibrary extends LocalizedStoreContainer {

	private static final Logger LOGGER = LoggerFactory.getLogger(IPhotoLibrary.class);

	public IPhotoLibrary(Renderer renderer) {
		super(renderer, "IphotoLibrary");
	}

	/**
	 * Creates, populates and returns a virtual folder mirroring the
	 * contents of the system's iPhoto folder.
	 * Mac OS X only.
	 *
	 * @return iPhotoVirtualFolder the populated <code>LibraryContainer</code>, or null if one couldn't be created.
	 */
	public static IPhotoLibrary getiPhotoFolder(Renderer renderer) {
		IPhotoLibrary iPhotoVirtualFolder = null;

		if (Platform.isMac()) {
			File file = getiPhotoDbFile();
			if (file != null) {
				Map<String, Object> iPhotoLib;
				try {
					// Load the properties XML file.
					iPhotoLib = Plist.load(file);
				} catch (XmlParseException | IOException e) {
					LOGGER.error("Something went wrong with the iPhoto Library scan: ", e);
					return null;
				}
				// The list of all photos
				Map<?, ?> photoList = (Map<?, ?>) iPhotoLib.get("Master Image List");

				// The list of events (rolls)
				List<Map<?, ?>> listOfRolls = (List<Map<?, ?>>) iPhotoLib.get("List of Rolls");

				iPhotoVirtualFolder = new IPhotoLibrary(renderer);

				for (Map<?, ?> roll : listOfRolls) {
					Object rollName = roll.get("RollName");

					if (rollName != null) {
						StoreContainer virtualFolder = new StoreContainer(renderer, rollName.toString(), null);

						// List of photos in an event (roll)
						List<?> rollPhotos = (List<?>) roll.get("KeyList");

						for (Object photo : rollPhotos) {
							Map<?, ?> photoProperties = (Map<?, ?>) photoList.get(photo);

							if (photoProperties != null) {
								Object imagePath = photoProperties.get("ImagePath");

								if (imagePath != null) {
									RealFile realFile = new RealFile(renderer, new File(imagePath.toString()));
									virtualFolder.addChild(realFile);
								}
							}
						}

						iPhotoVirtualFolder.addChild(virtualFolder);
					}
				}
			} else {
				LOGGER.info("iPhoto folder not found");
			}

		}

		return iPhotoVirtualFolder;
	}

	private static File getiPhotoDbFile() {
		File file = null;

		if (Platform.isMac()) {
			LOGGER.debug("Looking for iPhoto folder");
			Process process;
			try {
				// This command will show the XML files for recently opened iPhoto databases
				process = Runtime.getRuntime().exec(new String[]{"defaults", "read", "com.apple.iApps", "iPhotoRecentDatabases"});
			} catch (IOException e1) {
				LOGGER.error("Something went wrong with the iPhoto folder finder: ", e1);
				return null;
			}

			try (InputStream inputStream = process.getInputStream()) {
				List<String> lines = IOUtils.readLines(inputStream, StandardCharsets.UTF_8);
				LOGGER.debug("iPhotoRecentDatabases: {}", lines);

				if (lines.size() >= 2) {
					// we want the 2nd line
					String line = lines.get(1);

					// Remove extra spaces
					line = line.trim();

					// Remove quotes
					line = line.substring(1, line.length() - 1);

					URL url = URI.create(line).toURL();
					file = FileUtils.toFile(url);
					LOGGER.debug("Resolved URL to file: {} -> {}", url, file.getAbsolutePath());
				} else {
					LOGGER.info("iPhoto folder not found");
				}
			} catch (IllegalArgumentException | IOException e) {
				LOGGER.error("Something went wrong with the iPhoto iPhoto folder finder: ", e);
			}
		} else {
			LOGGER.info("iPhoto folder not found: Mac OS X only");
		}

		return file;
	}

}
