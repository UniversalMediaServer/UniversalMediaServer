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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.store.container.Feed;
import net.pms.util.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Old WebSourcesConfiguration from file WEB.conf.
 * Now handled by SharedContentConfiguration
 */
public class WebSourcesConfiguration {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebSourcesConfiguration.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final String KEY_WEB_CONF_PATH = "web_conf";
	private static final String DEFAULT_WEB_CONF_FILENAME = "WEB.conf";

	/**
	 * This class is not meant to be instantiated.
	 */
	private WebSourcesConfiguration() {
	}

	/**
	 * This parses the web config and return WebSource's List.
	 */
	public static synchronized List<WebSource> getWebSourcesFromConfiguration() {
		List<WebSource> result = new ArrayList<>();
		File webConf = new File(getWebConfPath());
		if (!webConf.exists()) {
			return result;
		}
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
								String uri = Feed.getFeedUrl(values[0]);
								String thumbnail = values.length > 2 ? values[2] : null;
								// If the resource does not yet have a name, attempt to get one now
								String resourceName = values.length > 3 ? values[3] : null;
								if (StringUtils.isBlank(resourceName)) {
									try {
										switch (sourceType) {
											case "imagefeed", "videofeed", "audiofeed" -> {
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
								result.add(new WebSource(sourceType, folderName, uri, resourceName, thumbnail));
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

	/**
	 * Returns the absolute path to the WEB.conf file. By default
	 * this is <pre>PROFILE_DIRECTORY + File.pathSeparator + WEB.conf</pre>,
	 * but it can be overridden via the <pre>web_conf</pre> profile option.
	 * The existence of the file is not checked.
	 *
	 * @return the path to the WEB.conf file.
	 */
	public static String getWebConfPath() {
		String webConfPath = FileUtil.getFileLocation(
				CONFIGURATION.getString(KEY_WEB_CONF_PATH, null),
				CONFIGURATION.getProfileDirectory(),
				DEFAULT_WEB_CONF_FILENAME
			).getFilePath();
		return CONFIGURATION.getString(KEY_WEB_CONF_PATH, webConfPath);
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
}
