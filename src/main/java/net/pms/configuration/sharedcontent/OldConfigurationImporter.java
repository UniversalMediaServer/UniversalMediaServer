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

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.pms.PMS;
import net.pms.configuration.MapFileConfiguration;
import net.pms.configuration.UmsConfiguration;
import net.pms.configuration.WebSourcesConfiguration;

public class OldConfigurationImporter {
	private OldConfigurationImporter() {}
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();

	public static synchronized SharedContentArray getOldConfigurations() {
		SharedContentArray result = new SharedContentArray();
		List<Path> sharedFolders = CONFIGURATION.getSharedFolders();
		if (!sharedFolders.isEmpty()) {
			for (Path sharedFolder : sharedFolders) {
				File file = sharedFolder.toFile();
				boolean monitored = CONFIGURATION.getMonitoredFolders().contains(sharedFolder);
				result.add(new Folder(file, monitored));
			}
		}
		List<MapFileConfiguration> virtualsFolders = MapFileConfiguration.parseVirtualFolders();
		if (virtualsFolders != null && !virtualsFolders.isEmpty()) {
			for (MapFileConfiguration virtualsFolder : virtualsFolders) {
				String name = virtualsFolder.getName();
				List<Folder> folders = new ArrayList<>();
				for (File file : virtualsFolder.getFiles()) {
					folders.add(new Folder(file));
				}
				result.add(new Folders(name, folders));
			}
		}
		List<WebSourcesConfiguration.WebSource> webSources = WebSourcesConfiguration.getWebSources();
		if (webSources != null && !webSources.isEmpty()) {
			for (WebSourcesConfiguration.WebSource webSource : webSources) {
				String sourceType = webSource.getSourceType();
				switch (sourceType) {
					case "audiofeed" -> result.add(new FeedAudio(webSource.getFolderName().replace(",", "/"), webSource.getResourceName(), webSource.getUri()));
					case "imagefeed" -> result.add(new FeedImage(webSource.getFolderName().replace(",", "/"), webSource.getResourceName(), webSource.getUri()));
					case "videofeed" -> result.add(new FeedVideo(webSource.getFolderName().replace(",", "/"), webSource.getResourceName(), webSource.getUri()));
					case "audiostream" -> result.add(new StreamAudio(webSource.getFolderName().replace(",", "/"), webSource.getResourceName(), webSource.getUri(), webSource.getThumbnail()));
					case "videostream" -> result.add(new StreamVideo(webSource.getFolderName().replace(",", "/"), webSource.getResourceName(), webSource.getUri(), webSource.getThumbnail()));
					default -> {
						//do nothing
					}
				}
			}
		}
		return result;
	}
}
