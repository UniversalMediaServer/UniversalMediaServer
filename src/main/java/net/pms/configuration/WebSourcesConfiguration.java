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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.pms.PMS;
import net.pms.dlna.Feed;
import net.pms.util.FileWatcher;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSourcesConfiguration {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebSourcesConfiguration.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final List<String> HEADER = Arrays.asList(
		"##########################################################################################################",
		"#                                                                                                        #",
		"# WEB.conf: configure support for web feeds and streams                                                  #",
		"#                                                                                                        #",
		"# NOTE: This file must be placed in the profile directory to work                                        #",
		"#                                                                                                        #",
		"# Supported types:                                                                                       #",
		"#                                                                                                        #",
		"#     imagefeed, audiofeed, videofeed, audiostream, videostream                                          #",
		"#                                                                                                        #",
		"# Format for feeds:                                                                                      #",
		"#                                                                                                        #",
		"#     type.folders,separated,by,commas=URL,,,name                                                        #",
		"#                                                                                                        #",
		"# Format for streams:                                                                                    #",
		"#                                                                                                        #",
		"#     type.folders,separated,by,commas=name,URL,optional thumbnail URL                                   #",
		"#                                                                                                        #",
		"##########################################################################################################"
	);
	private static final List<String> EXEMPLES = Arrays.asList(
		"",
		"# image feeds",
		"imagefeed.Web,Pictures=https://api.flickr.com/services/feeds/photos_public.gne?format=rss2",
		"imagefeed.Web,Pictures=https://api.flickr.com/services/feeds/photos_public.gne?id=39453068@N05&format=rss2",
		"imagefeed.Web,Pictures=https://api.flickr.com/services/feeds/photos_public.gne?id=14362684@N08&format=rss2",
		"",
		"# audio feeds",
		"audiofeed.Web,Podcasts=https://rss.art19.com/caliphate",
		"audiofeed.Web,Podcasts=https://www.nasa.gov/rss/dyn/Gravity-Assist.rss",
		"audiofeed.Web,Podcasts=https://rss.art19.com/wolverine-the-long-night",
		"",
		"# video feeds",
		"videofeed.Web,Vodcasts=https://feeds.feedburner.com/tedtalks_video",
		"videofeed.Web,Vodcasts=https://www.nasa.gov/rss/dyn/nasax_vodcast.rss",
		"videofeed.Web,YouTube Channels=https://www.youtube.com/feeds/videos.xml?channel_id=UC0PEAMcRK7Mnn2G1bCBXOWQ",
		"videofeed.Web,YouTube Channels=https://www.youtube.com/feeds/videos.xml?channel_id=UCccjdJEay2hpb5scz61zY6Q",
		"videofeed.Web,YouTube Channels=https://www.youtube.com/feeds/videos.xml?channel_id=UCqFzWxSCi39LnW1JKFR3efg",
		"videofeed.Web,YouTube Channels=https://www.youtube.com/feeds/videos.xml?channel_id=UCfAOh2t5DpxVrgS9NQKjC7A",
		"videofeed.Web,YouTube Channels=https://www.youtube.com/feeds/videos.xml?channel_id=UCzRBkt4a2hy6HObM3cl-x7g",
		"",
		"# audio streams",
		"audiostream.Web,Radio=RNZ,http://radionz-ice.streamguys.com/national.mp3,https://www.rnz.co.nz/assets/cms_uploads/000/000/159/RNZ_logo-Te-Reo-NEG-500.png",
		"",
		"# video streams",
		"# videostream.Web,TV=France 24,mms://stream1.france24.yacast.net/f24_liveen,http://www.france24.com/en/sites/france24.com.en/themes/france24/logo-fr.png",
		"# videostream.Web,TV=BFM TV (French TV),mms://vipmms9.yacast.net/bfm_bfmtv,http://upload.wikimedia.org/wikipedia/en/6/62/BFMTV.png",
		"# videostream.Web,Webcams=View of Shanghai Harbour,mmst://www.onedir.com/cam3,http://media-cdn.tripadvisor.com/media/photo-s/00/1d/4b/d8/pudong-from-the-bund.jpg"
	);
	private static final List<WebSourcesListener> LISTENERS = new ArrayList<>();
	private static final List<WebSource> SOURCES = getWebSourcesFromConfiguration();

	// Automatic reloading
	public static final FileWatcher.Listener RELOAD_WATCHER = (String filename, String event, FileWatcher.Watch watch, boolean isDir) -> {
		synchronized (SOURCES) {
			SOURCES.clear();
			SOURCES.addAll(getWebSourcesFromConfiguration());
			synchronized (LISTENERS) {
				for (WebSourcesListener listener : LISTENERS) {
					if (listener != null) {
						listener.updateWebSources();
					}
				}
			}
		}
	};
	static {
		FileWatcher.add(new FileWatcher.Watch(CONFIGURATION.getWebConfPath(), RELOAD_WATCHER));
	}

	/**
	 * This parses the web config and return WebSource's List.
	 */
	public static synchronized List<WebSource> getWebSourcesFromConfiguration() {
		File webConf = new File(CONFIGURATION.getWebConfPath());
		if (!webConf.exists()) {
			writeDefaultWebSourcesConfigurationFile();
		}
		FileWatcher.add(new FileWatcher.Watch(webConf.getPath(), RELOAD_WATCHER));
		List<WebSource> result = new ArrayList<>();
		try {
			try (LineNumberReader br = new LineNumberReader(new InputStreamReader(new FileInputStream(webConf), StandardCharsets.UTF_8))) {
				String line;
				while ((line = br.readLine()) != null) {
					line = line.trim();

					if (line.length() > 0 && !line.startsWith("#") && line.indexOf('=') > -1) {
						String key = line.substring(0, line.indexOf('='));
						String value = line.substring(line.indexOf('=') + 1);
						String[] keys = parseFeedKey(key);
						String sourceType = keys[0];
						String folderName = keys[1] == null ? null : keys[1];
						try {
							if (
								sourceType.equals("imagefeed") ||
								sourceType.equals("audiofeed") ||
								sourceType.equals("videofeed") ||
								sourceType.equals("audiostream") ||
								sourceType.equals("videostream")
							) {
								String[] values = parseFeedValue(value);
								String uri = values[0];
								String thumbnail = values.length > 2 ? values[2] : null;
								// If the resource does not yet have a name, attempt to get one now
								String resourceName = values.length > 3 ? values[3] : null;
								if (StringUtils.isBlank(resourceName)) {
									try {
										switch (sourceType) {
											case "imagefeed", "videofeed", "audiofeed" -> {
												// Convert YouTube channel URIs to their feed URIs
												if (uri.contains("youtube.com/channel/")) {
													uri = uri.replaceAll("youtube.com/channel/", "youtube.com/feeds/videos.xml?channel_id=");
												}
												resourceName = Feed.getFeedTitle(uri);
											}
											case "videostream", "audiostream" -> {
												resourceName = values.length > 0 && values[0] != null ? values[0] : null;
												uri = values.length > 1 && values[1] != null ? values[1] : null;
											}
											default -> {
												//nothing to do
											}
										}
									} catch (Exception e) {
										LOGGER.debug("Error while getting feed title: " + e);
									}
								}
								result.add(new WebSource(sourceType, resourceName, folderName, uri, thumbnail));
							}
						} catch (ArrayIndexOutOfBoundsException e) {
							// catch exception here and back to parsing
							LOGGER.info("Error at line " + br.getLineNumber() + " of WEB.conf: " + e.getMessage());
							LOGGER.debug(null, e);
						}
					}
				}
			}
		} catch (FileNotFoundException e) {
			LOGGER.debug("Can't read web configuration file {}", e.getMessage());
		} catch (IOException e) {
			LOGGER.warn("Unexpected error in WEB.conf: " + e.getMessage());
			LOGGER.debug("", e);
		}
		return result;
	}

	/**
	 * This return WebSource's List.
	 */
	public static List<WebSource> getWebSources() {
		synchronized (SOURCES) {
			return SOURCES;
		}
	}

	/**
	 * Splits the first part of a WEB.conf spec into a pair of Strings
	 * representing the resource type and its DLNA folder.
	 *
	 * @param spec (String) to be split
	 * @return Array of (String) that represents the tokenized entry.
	 */
	private static String[] parseFeedKey(String spec) {
		String[] pair = StringUtils.split(spec, ".", 2);

		if (pair == null || pair.length < 2) {
			pair = new String[2];
		}

		if (pair[0] == null) {
			pair[0] = "";
		}

		return pair;
	}

	/**
	 * Splits the second part of a WEB.conf spec into a triple of Strings
	 * representing the DLNA path, resource URI, optional thumbnail URI
	 * and name.
	 *
	 * @param spec (String) to be split
	 * @return Array of (String) that represents the tokenized entry.
	 */
	private static String[] parseFeedValue(String spec) {
		return spec.split(",");
	}

	public static void addListener(WebSourcesListener listener) {
		synchronized (LISTENERS) {
			LISTENERS.add(listener);
			listener.updateWebSources();
		}
	}

	public static void writeDefaultWebSourcesConfigurationFile() {
		writeWebSourcesConfigurationFile(EXEMPLES);
	}

	private static boolean sourcesEquals(List<WebSource> value) {
		synchronized (SOURCES) {
			if (value == null || value.size() != SOURCES.size()) {
				return false;
			}
			for (int i = 0; i < SOURCES.size(); i++) {
				if (!SOURCES.get(i).equals(value.get(i))) {
					return false;
				}
			}
			return true;
		}
	}

	private static synchronized void writeWebSourcesConfigurationFile(List<String> fileContents) {
		List<String> contentsToWrite = new ArrayList<>();
		contentsToWrite.addAll(HEADER);
		contentsToWrite.addAll(fileContents);

		try {
			Path webConfFilePath = Paths.get(CONFIGURATION.getWebConfPath());
			Files.write(webConfFilePath, contentsToWrite, StandardCharsets.UTF_8);
		} catch (IOException e) {
			LOGGER.debug("An error occurred while writing the web config file: {}", e);
		}
	}

	public static synchronized void writeWebSourcesConfiguration(List<WebSource> value) {
		if (sourcesEquals(value)) {
			return;
		}
		List<String> entries = new ArrayList<>();
		for (WebSource webSource : value) {
			StringBuilder entryToAdd = new StringBuilder();
			entryToAdd.append(webSource.getSourceType()).append(".").append(webSource.getFolderName()).append("=");
			switch (webSource.getSourceType()) {
				case "imagefeed", "videofeed", "audiofeed" -> {
					entryToAdd.append(webSource.getUri());
					if (webSource.getResourceName() != null) {
						entryToAdd.append(",,,").append(webSource.getResourceName());
					}
				}
				default -> {
					if (webSource.getResourceName() != null) {
						entryToAdd.append(webSource.getResourceName()).append(",").append(webSource.getUri());
						if (webSource.getThumbnail() != null) {
							entryToAdd.append(",").append(webSource.getThumbnail());
						}
					}
				}
			}
			entries.add(entryToAdd.toString());
		}
		writeWebSourcesConfigurationFile(entries);
	}

	public static synchronized void writeWebSourcesConfiguration(JsonArray fileContents) {
		List<String> entries = new ArrayList<>();
		for (JsonElement webContentItem : fileContents) {
			JsonObject webContentItemObject = webContentItem.getAsJsonObject();
			String name = webContentItemObject.get("name").getAsString();
			String type = webContentItemObject.get("type").getAsString();
			String folders = webContentItemObject.get("folders").getAsString();
			String source = webContentItemObject.get("source").getAsString();

			StringBuilder entryToAdd = new StringBuilder();
			entryToAdd.append(type).append(".").append(folders).append("=");

			switch (type) {
				case "imagefeed", "videofeed", "audiofeed" -> {
					entryToAdd.append(source);
					if (name != null) {
						entryToAdd.append(",,,").append(name);
					}
				}
				default -> {
					if (name != null) {
						entryToAdd.append(name).append(",").append(source);
					}
				}
			}

			entries.add(entryToAdd.toString());
		}
		writeWebSourcesConfigurationFile(entries);
	}

	/**
	 * This parses the web config and returns it as a JSON array.
	 *
	 * @return
	 */
	public static synchronized JsonArray getAllWebSourcesAsJsonArray() {
		JsonArray jsonArray = new JsonArray();
		for (WebSource webSource : getWebSources()) {
			JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty("name", webSource.getResourceName());
			jsonObject.addProperty("type", webSource.getSourceType());
			jsonObject.addProperty("folders", webSource.getFolderName());
			jsonObject.addProperty("source", webSource.getUri());
			jsonObject.addProperty("thumbnail", webSource.getThumbnail());
			jsonArray.add(jsonObject);
		}
		return jsonArray;
	}

	public static class WebSource {
		private final String sourceType;
		private final String folderName;
		private final String uri;
		private final String resourceName;
		private final String thumbnail;

		public WebSource(String sourceType, String folderName, String uri, String resourceName, String thumbnail) {
			this.sourceType = sourceType;
			this.folderName = folderName;
			this.uri = uri;
			this.resourceName = resourceName;
			this.thumbnail = thumbnail;
		}

		public String getSourceType() {
			return sourceType;
		}

		public String getFolderName() {
			return folderName;
		}

		public String getUri() {
			return uri;
		}

		public String getResourceName() {
			return resourceName;
		}

		public String getThumbnail() {
			return thumbnail;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof WebSource source) {
				return StringUtils.equals(sourceType, source.sourceType) &&
					StringUtils.equals(folderName, source.folderName) &&
					StringUtils.equals(uri, source.uri) &&
					StringUtils.equals(resourceName, source.resourceName) &&
					StringUtils.equals(thumbnail, source.thumbnail);
			}
			return false;
		}

		@Override
		public int hashCode() {
			int hash = 5;
			hash = 97 * hash + Objects.hashCode(this.sourceType);
			hash = 97 * hash + Objects.hashCode(this.folderName);
			hash = 97 * hash + Objects.hashCode(this.uri);
			hash = 97 * hash + Objects.hashCode(this.resourceName);
			hash = 97 * hash + Objects.hashCode(this.thumbnail);
			return hash;
		}
	}

	public interface WebSourcesListener {
		public void updateWebSources();
	}
}
