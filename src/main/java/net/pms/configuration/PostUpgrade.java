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

import java.sql.Connection;
import java.sql.SQLException;
import net.pms.PMS;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableMetadata;
import net.pms.encoders.EngineFactory;
import net.pms.encoders.StandardEngineId;
import net.pms.swing.Splash;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Actions that happen only the first time UMS runs after an upgrade.
 *
 * This run after the DB upgrades.
 *
 * @author Surf@ceS
 */
public class PostUpgrade {

	private static final Logger LOGGER = LoggerFactory.getLogger(PostUpgrade.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final String METADATA_KEY = "POST_UPGRADE";

	/**
	 * Post upgrade version must be increased every time a change require a post
	 * upgrade change.
	 */
	private static final int POST_UPGRADE_VERSION = 2;

	/**
	 * This class is not meant to be instantiated.
	 */
	private PostUpgrade() {
	}

	public static void proceed() {
		if (!MediaDatabase.isAvailable()) {
			LOGGER.trace("Not doing Post Upgrade because database is closed");
			return;
		}
		try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
			if (connection == null) {
				return;
			}
			Integer currentVersion = 0;
			String versionStr = MediaTableMetadata.getMetadataValue(connection, METADATA_KEY);
			if (!StringUtils.isBlank(versionStr)) {
				try {
					currentVersion = Integer.valueOf(versionStr);
				} catch (NumberFormatException ex) {
					currentVersion = 0;
				}
			}
			if (currentVersion < POST_UPGRADE_VERSION) {
				Splash.setStatusMessage("InitFirstRun");
				upgrade(connection, currentVersion);
			}

		} catch (SQLException ex) {
			LOGGER.trace("Error in Post Upgrade:", ex);
		}
	}

	private static void upgrade(Connection connection, Integer currentVersion) throws SQLException {
		for (int version = currentVersion; version < POST_UPGRADE_VERSION; version++) {
			switch (version) {
				case 0 -> {
					if (!CONFIGURATION.hasRunOnce()) {
						/*
							 * Enable youtube-dl once, to ensure that if it is
							 * disabled, that was done by the user.
						 */
						if (!EngineFactory.isEngineActive(StandardEngineId.YOUTUBE_DL)) {
							CONFIGURATION.setEngineEnabled(StandardEngineId.YOUTUBE_DL, true);
							CONFIGURATION.setEnginePriorityBelow(StandardEngineId.YOUTUBE_DL, StandardEngineId.FFMPEG_WEB_VIDEO);
						}

						// Ensure this only happens once
						CONFIGURATION.setHasRunOnce();
					}
				}
				case 1 -> {
					/*
						 * Enable FFmpegHlsVideo once, to ensure that if it is
						 * disabled, that was done by the user.
					 */
					if (!EngineFactory.isEngineActive(StandardEngineId.FFMPEG_HLS_VIDEO)) {
						CONFIGURATION.setEngineEnabled(StandardEngineId.FFMPEG_HLS_VIDEO, true);
						CONFIGURATION.setEnginePriorityBelow(StandardEngineId.FFMPEG_HLS_VIDEO, StandardEngineId.FFMPEG_VIDEO);
					}
				}
				default ->
					throw new IllegalStateException("Post Upgrade version missmatch");
			}
		}
		MediaTableMetadata.setOrUpdateMetadataValue(connection, METADATA_KEY, String.valueOf(POST_UPGRADE_VERSION));
	}

}
