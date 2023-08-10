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
package net.pms.media;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableFiles;
import net.pms.formats.Format;
import net.pms.parsers.FFmpegParser;
import net.pms.parsers.Parser;
import net.pms.util.InputFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaInfoStore {

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaInfoStore.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final Map<String, WeakReference<MediaInfo>> STORE = new HashMap<>();

	private MediaInfoStore() {
		//should not be instantiated
	}

	public static MediaInfo getMediaInfo(String filename, File file, Format format, int type) {
		synchronized (STORE) {
			if (STORE.containsKey(filename) && STORE.get(filename).get() != null) {
				return STORE.get(filename).get();
			}
			boolean found = false;
			MediaInfo media = null;
			Connection connection = null;
			InputFile input = new InputFile();
			input.setFile(file);
			try {
				if (CONFIGURATION.getUseCache()) {
					connection = MediaDatabase.getConnectionIfAvailable();
					if (connection != null) {
						connection.setAutoCommit(false);
						try {
							media = MediaTableFiles.getMediaInfo(connection, filename, file.lastModified());
							if (media != null) {
								if (!media.isMediaParsed()) {
									Parser.parse(media, input, format, type);
									MediaTableFiles.insertOrUpdateData(connection, filename, file.lastModified(), type, media);
								}
								media.postParse(type);
								found = true;
							}
						} catch (IOException | SQLException e) {
							LOGGER.debug("Error while getting cached information about {}, reparsing information: {}", filename, e.getMessage());
							LOGGER.trace("", e);
						}
					}
				}

				if (!found) {
					media = new MediaInfo();

					if (format != null) {
						Parser.parse(media, input, format, type);
					} else {
						// Don't think that will ever happen
						FFmpegParser.parse(media, input, format, type);
					}

					media.waitMediaParsing(5);
					if (connection != null && media.isMediaParsed()) {
						try {
							MediaTableFiles.insertOrUpdateData(connection, filename, file.lastModified(), type, media);
						} catch (SQLException e) {
							LOGGER.error(
								"Database error while trying to add parsed information for \"{}\" to the cache: {}",
								filename,
								e.getMessage());
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace("SQL error code: {}", e.getErrorCode());
								if (
									e.getCause() instanceof SQLException &&
									((SQLException) e.getCause()).getErrorCode() != e.getErrorCode()
								) {
									LOGGER.trace("Cause SQL error code: {}", ((SQLException) e.getCause()).getErrorCode());
								}
								LOGGER.trace("", e);
							}
						}
					}
				}
			} catch (SQLException e) {
				LOGGER.error("Error in RealFile.resolve: {}", e.getMessage());
				LOGGER.trace("", e);
			} finally {
				try {
					if (connection != null) {
						connection.commit();
						connection.setAutoCommit(true);
					}
				} catch (SQLException e) {
					LOGGER.error("Error in commit in RealFile.resolve: {}", e.getMessage());
					LOGGER.trace("", e);
				}
				MediaDatabase.close(connection);
			}
			if (media != null) {
				STORE.put(filename, new WeakReference<>(media));
			}
			return media;
		}
	}

	public static void clear() {
		synchronized (STORE) {
			STORE.clear();
		}
	}

}
