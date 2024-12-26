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
package net.pms.external.musicbrainz.coverart;

import com.universalmediaserver.coverartarchive.api.CoverArtArchiveClient;
import com.universalmediaserver.coverartarchive.api.endpoint.ThumbnailSize;
import com.universalmediaserver.coverartarchive.api.schema.ResultSchema;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableCoverArtArchive;
import net.pms.database.MediaTableCoverArtArchive.CoverArtArchiveResult;
import net.pms.external.musicbrainz.api.MusicBrainzUtil;
import org.jaudiotagger.tag.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for fetching music covers from Cover Art Archive.
 *
 * It handles database caching binary cover data from Cover Art Archive.
 */
public class CoverArtArchiveUtil extends CoverUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(CoverArtArchiveUtil.class);
	private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000L; // 24 hours
	private static final Map<String, Object> LOCKS = new HashMap<>();

	/**
	 * This class is not meant to be instantiated.
	 */
	protected CoverArtArchiveUtil() {
	}

	private static Object getLock(String filename) {
		synchronized (LOCKS) {
			if (LOCKS.containsKey(filename)) {
				return LOCKS.get(filename);
			}
			Object lock = new Object();
			LOCKS.put(filename, lock);
			return lock;
		}
	}

	@Override
	public byte[] doGetThumbnail(Tag tag, boolean externalNetwork) {
		String mBID = MusicBrainzUtil.getMBID(tag, externalNetwork);
		return doGetThumbnail(mBID, externalNetwork);
	}

	public static byte[] doGetThumbnail(String mBID, boolean externalNetwork) {
		if (mBID != null) {
			// Secure exclusive access to search for this mBID
			Object lock = getLock(mBID);
			synchronized (lock) {
				Connection connection = null;
				try {
					connection = MediaDatabase.getConnectionIfAvailable();
					// Check if it's cached first
					if (connection != null) {
						CoverArtArchiveResult result = MediaTableCoverArtArchive.findMBID(mBID);
						if (result.isFound()) {
							if (result.hasCoverBytes()) {
								return result.getCoverBytes();
							} else if (System.currentTimeMillis() - result.getModifiedTime() < EXPIRATION_TIME) {
								// If a lookup has been done within expireTime and no result,
								// return null. Do another lookup after expireTime has passed
								return null;
							}
						}
					}

					if (!externalNetwork) {
						LOGGER.warn("Can't download cover from Cover Art Archive since external network is disabled");
						LOGGER.info("Either enable external network or disable cover download");
						return null;
					}

					CoverArtArchiveClient client = new CoverArtArchiveClient();

					ResultSchema result = client.release(mBID).getDetails();
					if (result == null) {
						LOGGER.debug("Cover for MBID \"{}\" was not found at CoverArtArchive", mBID);
						MediaTableCoverArtArchive.writeMBID(mBID, null);
						return null;
					} else if (result.getImages() == null || result.getImages().isEmpty()) {
						LOGGER.debug("MBID \"{}\" has no cover at CoverArtArchive", mBID);
						if (connection != null) {
							MediaTableCoverArtArchive.writeMBID(mBID, null);
						}
						return null;
					}
					byte[] cover = client.release(mBID).getFrontImageBytes(ThumbnailSize.LARGE);
					if (cover != null && cover.length > 0 && connection != null) {
						MediaTableCoverArtArchive.writeMBID(mBID, null);
					}
					return cover;
				} finally {
					MediaDatabase.close(connection);
				}
			}
		}
		return null;
	}

}
