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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableCoverArtArchive;
import net.pms.database.MediaTableCoverArtArchive.CoverArtArchiveResult;
import net.pms.external.musicbrainz.api.MusicBrainzUtil;
import org.apache.commons.lang3.StringUtils;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for fetching music covers from Cover Art Archive.
 *
 * It handles database caching binary cover data from Cover Art Archive.
 */
public class CoverArtArchiveUtil extends CoverUtil {

	private static class AlbumMBIDCache extends LinkedHashMap<String, Optional<String>> {

		private static final long serialVersionUID = 1L;

		AlbumMBIDCache() {
			super(16, 0.75f, true);
		}

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Optional<String>> eldest) {
			return size() > ALBUM_CACHE_SIZE;
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(CoverArtArchiveUtil.class);
	private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000L; // 24 hours

	private static final int LOCK_STRIPES = 64;
	private static final Object[] LOCKS = new Object[LOCK_STRIPES];

	static {
		for (int i = 0; i < LOCK_STRIPES; i++) {
			LOCKS[i] = new Object();
		}
	}

	// Resolved release MBID per album, so the tracks of an album share one MusicBrainz search.
	private static final int ALBUM_CACHE_SIZE = 1000;
	private static final Map<String, Optional<String>> ALBUM_MBIDS = Collections.synchronizedMap(new AlbumMBIDCache());

	/**
	 * This class is not meant to be instantiated.
	 */
	protected CoverArtArchiveUtil() {
	}

	private static Object getLock(String key) {
		return LOCKS[Math.floorMod(key.hashCode(), LOCK_STRIPES)];
	}

	@Override
	public byte[] doGetThumbnail(Tag tag, boolean externalNetwork) {
		String mBID = getReleaseMBID(tag, externalNetwork);
		return doGetThumbnail(mBID, externalNetwork);
	}

	/**
	 * A cover belongs to the release, so the MBID is searched once per album instead of once per track.
	 * Without this every track of an album repeats the same MusicBrainz search, which is rate limited
	 * to roughly one request per second.
	 *
	 * @param tag the {@link Tag} to get the release MBID for
	 * @param externalNetwork whether MusicBrainz may be queried
	 * @return the release MBID or <code>null</code> if none was found
	 */
	private static String getReleaseMBID(Tag tag, boolean externalNetwork) {
		// A tagged release id is per track information and needs no lookup at all.
		String taggedMBID = getTagValue(tag, FieldKey.MUSICBRAINZ_RELEASEID);
		if (taggedMBID != null) {
			return taggedMBID;
		}
		String albumKey = getAlbumKey(tag);
		if (albumKey == null) {
			// Not enough information to tell albums apart, search per track as before.
			return MusicBrainzUtil.getMBID(tag, externalNetwork);
		}
		Optional<String> cached = ALBUM_MBIDS.get(albumKey);
		if (cached != null) {
			return cached.orElse(null);
		}
		// Album keys and MBIDs never collide, so both can share the lock map.
		synchronized (getLock(albumKey)) {
			// Another track of this album may have resolved it while we waited for the lock.
			cached = ALBUM_MBIDS.get(albumKey);
			if (cached != null) {
				return cached.orElse(null);
			}
			String mBID = MusicBrainzUtil.getMBID(tag, externalNetwork);
			ALBUM_MBIDS.put(albumKey, Optional.ofNullable(mBID));
			return mBID;
		}
	}

	/**
	 * @param tag the {@link Tag} to build the key from
	 * @return a key identifying the album, or <code>null</code> if the tag has no album name
	 */
	private static String getAlbumKey(Tag tag) {
		String album = getTagValue(tag, FieldKey.ALBUM);
		if (album == null) {
			return null;
		}
		String artist = getTagValue(tag, FieldKey.ALBUM_ARTIST);
		if (artist == null) {
			artist = getTagValue(tag, FieldKey.ARTIST);
		}
		return album + '\u0000' + artist + '\u0000' + getTagValue(tag, FieldKey.YEAR);
	}

	/**
	 * @return the trimmed first value of the field key, or <code>null</code> if it is blank or unsupported
	 */
	private static String getTagValue(Tag tag, FieldKey key) {
		try {
			String value = tag.getFirst(key);
			return StringUtils.isBlank(value) ? null : value.trim();
		} catch (UnsupportedOperationException e) {
			return null;
		}
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
