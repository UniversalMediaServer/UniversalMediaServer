/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.util;

import fm.last.musicbrainz.coverart.CoverArt;
import fm.last.musicbrainz.coverart.CoverArtException;
import fm.last.musicbrainz.coverart.CoverArtImage;
import fm.last.musicbrainz.coverart.impl.DefaultCoverArtArchiveClient;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.pms.database.TableCoverArtArchive;
import net.pms.database.TableCoverArtArchive.CoverArtArchiveResult;
import net.pms.database.TableMusicBrainzReleases;
import net.pms.database.TableMusicBrainzReleases.MusicBrainzReleasesResult;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpResponseException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class is responsible for fetching music covers from Cover Art Archive.
 * It handles database caching and http lookup of both MusicBrainz ID's (MBID)
 * and binary cover data from Cover Art Archive.
 *
 * @author Nadahar
 */

public class CoverArtArchiveUtil extends CoverUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(CoverArtArchiveUtil.class);
	private static final long WAIT_TIMEOUT_MS = 30000;
	private static final long expireTime = 24 * 60 * 60 * 1000; // 24 hours
	private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

	private static enum ReleaseType {
		Single,
		Album,
		EP,
		Broadcast,
		Other
	}

	private static class ReleaseRecord {

		String id;
		int score;
		String title;
		List<String> artists = new ArrayList<>();
		ReleaseType type;
		String year;

		public ReleaseRecord() {
		}

		public ReleaseRecord(ReleaseRecord source) {
			id = source.id;
			score = source.score;
			title = source.title;
			type = source.type;
			year = source.year;
			for (String artist : source.artists) {
				artists.add(artist);
			}
		}

	}

	/**
	 * This class is a container to hold information used by
	 * {@link CoverArtArchiveUtil} to look up covers.
	 */
	public static class CoverArtArchiveTagInfo {
		public final String album;
		public final String artist;
		public final String title;
		public final String year;
		public final String artistId;
		public final String trackId;

		public boolean hasInfo() {
			return
				StringUtil.hasValue(album) ||
				StringUtil.hasValue(artist) ||
				StringUtil.hasValue(title) ||
				StringUtil.hasValue(year) ||
				StringUtil.hasValue(artistId) ||
				StringUtil.hasValue(trackId);
		}

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			if (StringUtil.hasValue(artist)) {
				result.append(artist);
			}
			if (StringUtil.hasValue(artistId)) {
				if (result.length() > 0) {
					result.append(" (").append(artistId).append(')');
				} else {
					result.append(artistId);
				}
			}
			if (
				result.length() > 0 &&
				(
					StringUtil.hasValue(title) ||
					StringUtil.hasValue(album) ||
					StringUtil.hasValue(trackId)
				)

			) {
				result.append(" - ");
			}
			if (StringUtil.hasValue(album)) {
				result.append(album);
				if (StringUtil.hasValue(title) || StringUtil.hasValue(trackId)) {
					result.append(": ");
				}
			}
			if (StringUtil.hasValue(title)) {
				result.append(title);
				if (StringUtil.hasValue(trackId)) {
					result.append(" (").append(trackId).append(')');
				}
			} else if (StringUtil.hasValue(trackId)) {
				result.append(trackId);
			}
			if (StringUtil.hasValue(year)) {
				if (result.length() > 0) {
					result.append(" (").append(year).append(')');
				} else {
					result.append(year);
				}
			}
			return result.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((album == null) ? 0 : album.hashCode());
			result = prime * result + ((artist == null) ? 0 : artist.hashCode());
			result = prime * result + ((artistId == null) ? 0 : artistId.hashCode());
			result = prime * result + ((title == null) ? 0 : title.hashCode());
			result = prime * result + ((trackId == null) ? 0 : trackId.hashCode());
			result = prime * result + ((year == null) ? 0 : year.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof CoverArtArchiveTagInfo)) {
				return false;
			}
			CoverArtArchiveTagInfo other = (CoverArtArchiveTagInfo) obj;
			if (album == null) {
				if (other.album != null) {
					return false;
				}
			} else if (!album.equals(other.album)) {
				return false;
			}
			if (artist == null) {
				if (other.artist != null) {
					return false;
				}
			} else if (!artist.equals(other.artist)) {
				return false;
			}
			if (artistId == null) {
				if (other.artistId != null) {
					return false;
				}
			} else if (!artistId.equals(other.artistId)) {
				return false;
			}
			if (title == null) {
				if (other.title != null) {
					return false;
				}
			} else if (!title.equals(other.title)) {
				return false;
			}
			if (trackId == null) {
				if (other.trackId != null) {
					return false;
				}
			} else if (!trackId.equals(other.trackId)) {
				return false;
			}
			if (year == null) {
				if (other.year != null) {
					return false;
				}
			} else if (!year.equals(other.year)) {
				return false;
			}
			return true;
		}

		public CoverArtArchiveTagInfo(Tag tag) {
			if (AudioUtils.tagSupportsFieldKey(tag, FieldKey.ALBUM)) {
				album = tag.getFirst(FieldKey.ALBUM);
			} else {
				album = null;
			}

			if (AudioUtils.tagSupportsFieldKey(tag, FieldKey.ARTIST)) {
				artist = tag.getFirst(FieldKey.ARTIST);
			} else {
				artist = null;
			}

			if (AudioUtils.tagSupportsFieldKey(tag, FieldKey.TITLE)) {
				title = tag.getFirst(FieldKey.TITLE);
			} else {
				title = null;
			}

			if (AudioUtils.tagSupportsFieldKey(tag, FieldKey.YEAR)) {
				year = tag.getFirst(FieldKey.YEAR);
			} else {
				year = null;
			}

			if (AudioUtils.tagSupportsFieldKey(tag, FieldKey.MUSICBRAINZ_ARTISTID)) {
				artistId = tag.getFirst(FieldKey.MUSICBRAINZ_ARTISTID);
			} else {
				artistId = null;
			}

			if (AudioUtils.tagSupportsFieldKey(tag, FieldKey.MUSICBRAINZ_TRACK_ID)) {
				trackId = tag.getFirst(FieldKey.MUSICBRAINZ_TRACK_ID);
			} else {
				trackId = null;
			}
		}
	}

	private static class CoverArtArchiveTagLatch {
		final CoverArtArchiveTagInfo info;
		final CountDownLatch latch = new CountDownLatch(1);

		public CoverArtArchiveTagLatch(CoverArtArchiveTagInfo info) {
			this.info = info;
		}
	}

	private static class CoverArtArchiveCoverLatch {
		final String mBID;
		final CountDownLatch latch = new CountDownLatch(1);

		public CoverArtArchiveCoverLatch(String mBID) {
			this.mBID = mBID;
		}
	}

	/**
	 * Do not instantiate this class, use {@link CoverUtil#get()}
	 */
	protected CoverArtArchiveUtil() {
	}

	private static final Object tagLatchListLock = new Object();
	private static final List<CoverArtArchiveTagLatch> tagLatchList = new ArrayList<>();

	/**
	 * Used to serialize search on a per {@link Tag} basis. Every thread doing
	 * a search much hold a {@link CoverArtArchiveTagLatch} and release it when
	 * the search is done and the result is written. Any other threads
	 * attempting to search for the same {@link Tag} will wait for the existing
	 * {@link CoverArtArchiveTagLatch} to be released, and can then use the
	 * results from the previous thread instead of conducting it's own search.
	 */
	private static CoverArtArchiveTagLatch reserveTagLatch(final CoverArtArchiveTagInfo tagInfo) {
		CoverArtArchiveTagLatch tagLatch = null;

		boolean owner = false;
		long startTime = System.currentTimeMillis();

		while (!owner && !Thread.currentThread().isInterrupted()) {

			// Find if any other tread is currently searching the same tag
			synchronized (tagLatchListLock) {
				for (CoverArtArchiveTagLatch latch : tagLatchList) {
					if (latch.info.equals(tagInfo)) {
						tagLatch = latch;
						break;
					}
				}
				// None found, our turn
				if (tagLatch == null) {
					tagLatch = new CoverArtArchiveTagLatch(tagInfo);
					tagLatchList.add(tagLatch);
					owner = true;
				}
			}

			// Check for timeout here instead of in the while loop make logging
			// it easier.
			if (!owner && System.currentTimeMillis() - startTime > WAIT_TIMEOUT_MS) {
				LOGGER.debug("A MusicBrainz search timed out while waiting it's turn");
				return null;
			}

			if (!owner) {
				try {
					tagLatch.latch.await();
				} catch (InterruptedException e) {
					LOGGER.debug("A MusicBrainz search was interrupted while waiting it's turn");
					Thread.currentThread().interrupt();
					return null;
				} finally {
					tagLatch = null;
				}
			}
		}

		return tagLatch;
	}

	private static void releaseTagLatch(CoverArtArchiveTagLatch tagLatch) {
		synchronized (tagLatchListLock) {
			if (!tagLatchList.remove(tagLatch)) {
				LOGGER.error("Concurrency error: Held tagLatch not found in latchList");
			}
		}
		tagLatch.latch.countDown();
	}

	private static final Object coverLatchListLock = new Object();
	private static final List<CoverArtArchiveCoverLatch> coverLatchList = new ArrayList<>();

	/**
	 * Used to serialize search on a per MBID basis. Every thread doing
	 * a search much hold a {@link CoverArtArchiveCoverLatch} and release it
	 * when the search is done and the result is written. Any other threads
	 * attempting to search for the same MBID will wait for the existing
	 * {@link CoverArtArchiveCoverLatch} to be released, and can then use the
	 * results from the previous thread instead of conducting it's own search.
	 */
	private static CoverArtArchiveCoverLatch reserveCoverLatch(final String mBID) {
		CoverArtArchiveCoverLatch coverLatch = null;

		boolean owner = false;
		long startTime = System.currentTimeMillis();

		while (!owner && !Thread.currentThread().isInterrupted()) {

			// Find if any other tread is currently searching the same MBID
			synchronized (coverLatchListLock) {
				for (CoverArtArchiveCoverLatch latch : coverLatchList) {
					if (latch.mBID.equals(mBID)) {
						coverLatch = latch;
						break;
					}
				}
				// None found, our turn
				if (coverLatch == null) {
					coverLatch = new CoverArtArchiveCoverLatch(mBID);
					coverLatchList.add(coverLatch);
					owner = true;
				}
			}

			// Check for timeout here instead of in the while loop make logging
			// it easier.
			if (!owner && System.currentTimeMillis() - startTime > WAIT_TIMEOUT_MS) {
				LOGGER.debug("A Cover Art Achive search timed out while waiting it's turn");
				return null;
			}

			if (!owner) {
				try {
					coverLatch.latch.await();
				} catch (InterruptedException e) {
					LOGGER.debug("A Cover Art Archive search was interrupted while waiting it's turn");
					Thread.currentThread().interrupt();
					return null;
				} finally {
					coverLatch = null;
				}
			}
		}

		return coverLatch;
	}

	private static void releaseCoverLatch(CoverArtArchiveCoverLatch coverLatch) {
		synchronized (coverLatchListLock) {
			if (!coverLatchList.remove(coverLatch)) {
				LOGGER.error("Concurrency error: Held coverLatch not found in latchList");
			}
		}
		coverLatch.latch.countDown();
	}

	@Override
	protected byte[] doGetThumbnail(Tag tag, boolean externalNetwork) {
		String mBID = getMBID(tag, externalNetwork);
		if (mBID != null) {
			// Secure exclusive access to search for this tag
			CoverArtArchiveCoverLatch latch = reserveCoverLatch(mBID);
			if (latch == null) {
				// Couldn't reserve exclusive access, giving up
				return null;
			}
			try {
				// Check if it's cached first
				CoverArtArchiveResult result = TableCoverArtArchive.findMBID(mBID);
				if (result.found) {
					if (result.cover != null) {
						return result.cover;
					} else if (System.currentTimeMillis() - result.modified.getTime() < expireTime) {
						// If a lookup has been done within expireTime and no result,
						// return null. Do another lookup after expireTime has passed
						return null;
					}
				}

				if (!externalNetwork) {
					LOGGER.warn("Can't download cover from Cover Art Archive since external network is disabled");
					LOGGER.info("Either enable external network or disable cover download");
					return null;
				}

				DefaultCoverArtArchiveClient client = new DefaultCoverArtArchiveClient();

				CoverArt coverArt;
				try {
					coverArt = client.getByMbid(UUID.fromString(mBID));
				} catch (CoverArtException e) {
					LOGGER.debug("Could not get cover with MBID \"{}\": {}", mBID, e.getMessage());
					LOGGER.trace("", e);
					return null;
				}
				if (coverArt == null || coverArt.getImages().isEmpty()) {
					LOGGER.debug("MBID \"{}\" has no cover at CoverArtArchive", mBID);
					TableCoverArtArchive.writeMBID(mBID, null);
					return null;
				}
				CoverArtImage image = coverArt.getFrontImage();
				if (image == null) {
					image = coverArt.getImages().get(0);
				}
				try (InputStream is = image.getLargeThumbnail()) {
					byte[] cover = IOUtils.toByteArray(is);
					TableCoverArtArchive.writeMBID(mBID, cover);
					return cover;
				} catch (HttpResponseException e) {
					if (e.getStatusCode() == 404) {
						LOGGER.debug("Cover for MBID \"{}\" was not found at CoverArtArchive", mBID);
						TableCoverArtArchive.writeMBID(mBID, null);
						return null;
					} else {
						LOGGER.warn("Got HTTP response {} while trying to download over for MBID \"{}\" from CoverArtArchive: {}", e.getStatusCode(), mBID, e.getMessage());
					}
				} catch (IOException e) {
					LOGGER.error("An error occurred while downloading cover for MBID \"{}\": {}", mBID, e.getMessage());
					LOGGER.trace("", e);
					return null;
				}
			} finally {
				releaseCoverLatch(latch);
			}
		}
		return null;
	}

	private String fuzzString(String s) {
		String[] words = s.split(" ");
		StringBuilder sb = new StringBuilder("(");
		for (String word : words) {
			sb.append(StringUtil.luceneEscape(word)).append("~ ");
		}
		sb.append(')');
		return sb.toString();
	}

	private String buildMBReleaseQuery(final CoverArtArchiveTagInfo tagInfo, final boolean fuzzy) {
		final String AND = urlEncode(" AND ");
		StringBuilder query = new StringBuilder("release/?query=");
		boolean added = false;

		if (StringUtil.hasValue(tagInfo.album)) {
			if (fuzzy) {
				query.append(urlEncode(fuzzString(tagInfo.album)));
			} else {
				query.append(urlEncode("\"" + StringUtil.luceneEscape(tagInfo.album) + "\""));
			}
			added = true;
		}

		if (StringUtil.hasValue(tagInfo.artistId)) {
			if (added) {
				query.append(AND);
			}
			query.append("arid:").append(tagInfo.artistId);
			added = true;
		} else if (StringUtil.hasValue(tagInfo.artist)) {
			if (added) {
				query.append(AND);
			}
			query.append("artistname:");
			if (fuzzy) {
				query.append(urlEncode(fuzzString(tagInfo.artist)));
			} else {
				query.append(urlEncode("\"" + StringUtil.luceneEscape(tagInfo.artist) + "\""));
			}
			added = true;
		}

		if (
			StringUtil.hasValue(tagInfo.trackId) && (
				!StringUtil.hasValue(tagInfo.album) || !(
					StringUtil.hasValue(tagInfo.artist) ||
					StringUtil.hasValue(tagInfo.artistId)
				)
			)
		) {
			if (added) {
				query.append(AND);
			}
			query.append("tid:").append(tagInfo.trackId);
			added = true;
		} else if (
			StringUtil.hasValue(tagInfo.title) && (
				!StringUtil.hasValue(tagInfo.album) || !(
					StringUtil.hasValue(tagInfo.artist) ||
					StringUtil.hasValue(tagInfo.artistId)
				)
			)
		) {
			if (added) {
				query.append(AND);
			}
			query.append("recording:");
			if (fuzzy) {
				query.append(urlEncode(fuzzString(tagInfo.title)));
			} else {
				query.append(urlEncode("\"" + StringUtil.luceneEscape(tagInfo.title) + "\""));
			}
			added = true;
		}

		if (StringUtil.hasValue(tagInfo.year)) {
			if (added) {
				query.append(AND);
			}
			query.append("date:").append(urlEncode(tagInfo.year)).append('*');
			added = true;
		}
		return query.toString();
	}

	private String buildMBRecordingQuery(final CoverArtArchiveTagInfo tagInfo, final boolean fuzzy) {
		final String AND = urlEncode(" AND ");
		StringBuilder query = new StringBuilder("recording/?query=");
		boolean added = false;

		if (StringUtil.hasValue(tagInfo.title)) {
			if (fuzzy) {
				query.append(urlEncode(fuzzString(tagInfo.title)));
			} else {
				query.append(urlEncode("\"" + StringUtil.luceneEscape(tagInfo.title) + "\""));
			}
			added = true;
		}

		if (StringUtil.hasValue(tagInfo.trackId)) {
			if (added) {
				query.append(AND);
			}
			query.append("tid:").append(tagInfo.trackId);
			added = true;
		}

		if (StringUtil.hasValue(tagInfo.artistId)) {
			if (added) {
				query.append(AND);
			}
			query.append("arid:").append(tagInfo.artistId);
			added = true;
		} else if (StringUtil.hasValue(tagInfo.artist)) {
			if (added) {
				query.append(AND);
			}
			query.append("artistname:");
			if (fuzzy) {
				query.append(urlEncode(fuzzString(tagInfo.artist)));
			} else {
				query.append(urlEncode("\"" + StringUtil.luceneEscape(tagInfo.artist) + "\""));
			}
		}

		if (StringUtil.hasValue(tagInfo.year)) {
			if (added) {
				query.append(AND);
			}
			query.append("date:").append(urlEncode(tagInfo.year)).append('*');
			added = true;
		}
		return query.toString();
	}

	private String getMBID(Tag tag, boolean externalNetwork) {
		if (tag == null) {
			return null;
		}

		// No need to look up MBID if it's already in the tag
		String mBID = null;
		if (AudioUtils.tagSupportsFieldKey(tag, FieldKey.MUSICBRAINZ_RELEASEID)) {
			mBID = tag.getFirst(FieldKey.MUSICBRAINZ_RELEASEID);
			if (StringUtil.hasValue(mBID)) {
				return mBID;
			}
		}

		DocumentBuilder builder = null;
		try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			LOGGER.error("Error initializing XML parser: {}", e.getMessage());
			LOGGER.trace("", e);
			return null;
		}

		final CoverArtArchiveTagInfo tagInfo = new CoverArtArchiveTagInfo(tag);
		if (!tagInfo.hasInfo()) {
			LOGGER.trace("Tag has no information - aborting search");
			return null;
		}

		// Secure exclusive access to search for this tag
		CoverArtArchiveTagLatch latch = reserveTagLatch(tagInfo);
		if (latch == null) {
			// Couldn't reserve exclusive access, giving up
			LOGGER.error("Could not reserve tag latch for MBID search for \"{}\"", tagInfo);
			return null;
		}
		try {
			// Check if it's cached first
			MusicBrainzReleasesResult result = TableMusicBrainzReleases.findMBID(tagInfo);
			if (result.found) {
				if (StringUtil.hasValue(result.mBID)) {
					return result.mBID;
				} else if (System.currentTimeMillis() - result.modified.getTime() < expireTime) {
					// If a lookup has been done within expireTime and no result,
					// return null. Do another lookup after expireTime has passed
					return null;
				}
			}

			if (!externalNetwork) {
				LOGGER.warn("Can't look up cover MBID from MusicBrainz since external network is disabled");
				LOGGER.info("Either enable external network or disable cover download");
				return null;
			}

			/*
			 * Rounds are defined as this:
			 *
			 *   1 - Exact release search
			 *   2 - Fuzzy release search
			 *   3 - Exact track search
			 *   4 - Fuzzy track search
			 *   5 - Give up
			 */

			int round;
			if (StringUtil.hasValue(tagInfo.album) || StringUtil.hasValue(tagInfo.artist) || StringUtil.hasValue(tagInfo.artistId)) {
				round = 1;
			} else {
				round = 3;
			}

			while (round < 5 && !StringUtil.hasValue(mBID)) {
				String query;

				if (round < 3) {
					query = buildMBReleaseQuery(tagInfo, round > 1);
				} else {
					query = buildMBRecordingQuery(tagInfo, round > 3);
				}

				if (query != null) {
					final String url = "http://musicbrainz.org/ws/2/" + query + "&fmt=xml";
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Performing release MBID lookup at musicbrainz: \"{}\"", url);
					}

					try {
						HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
						connection.setRequestProperty("Accept-Charset", StandardCharsets.UTF_8.name());
						int status = connection.getResponseCode();
						if (status != 200) {
							LOGGER.error("Could not lookup audio cover for \"{}\": musicbrainz.org replied with status code {}", tagInfo.title, status);
							return null;
						}

						Document document;
						try {
							document = builder.parse(connection.getInputStream());
						} catch (SAXException e) {
							LOGGER.error("Failed to parse XML for \"{}\": {}", url, e.getMessage());
							LOGGER.trace("", e);
							return null;
						} finally {
							connection.getInputStream().close();
						}

						ArrayList<ReleaseRecord> releaseList;
						if (round < 3) {
							releaseList = parseRelease(document, tagInfo);
						} else {
							releaseList = parseRecording(document, tagInfo);
						}

						if (releaseList != null && !releaseList.isEmpty()) {
							// Try to find the best match - this logic can be refined if
							// matching quality turns out to be to low
							int maxScore = 0;
							for (ReleaseRecord release : releaseList) {
								if (StringUtil.hasValue(tagInfo.artist)) {
									boolean found = false;
									for (String s : release.artists) {
										if (s.equalsIgnoreCase(tagInfo.artist)) {
											found = true;
											break;
										}
									}
									if (found) {
										release.score += 30;
									}
								}
								if (StringUtil.hasValue(tagInfo.album)) {
									if (release.type == ReleaseType.Album) {
										release.score += 20;
										if (release.title.equalsIgnoreCase(tagInfo.album)) {
											release.score += 30;
										}
									}
								} else if (StringUtil.hasValue(tagInfo.title)) {
									if ((round > 2 || release.type == ReleaseType.Single) && release.title.equalsIgnoreCase(tagInfo.title)) {
										release.score += 40;
									}
								}
								if (StringUtil.hasValue(tagInfo.year) && StringUtil.hasValue(release.year)) {
									if (tagInfo.year.equals(release.year)) {
										release.score += 20;
									}
								}
								maxScore = Math.max(maxScore, release.score);
							}

							for (ReleaseRecord release : releaseList) {
								if (release.score == maxScore) {
									mBID = release.id;
									break;
								}
							}
						}

						if (StringUtil.hasValue(mBID)) {
							LOGGER.trace("Music release \"{}\" found with \"{}\"", mBID, url);
						} else {
							LOGGER.trace("No music release found with \"{}\"", url);
						}

					} catch (IOException e) {
						LOGGER.debug("Failed to find MBID for \"{}\": {}", query, e.getMessage());
						LOGGER.trace("", e);
						return null;
					}
				}
				round++;
			}
			if (StringUtil.hasValue(mBID)) {
				LOGGER.debug("MusicBrainz release ID \"{}\" found for \"{}\"", mBID, tagInfo);
				TableMusicBrainzReleases.writeMBID(mBID, tagInfo);
				return mBID;
			} else {
				LOGGER.debug("No MusicBrainz release found for \"{}\"", tagInfo);
				TableMusicBrainzReleases.writeMBID(null, tagInfo);
				return null;
			}
		} finally {
			releaseTagLatch(latch);
		}
	}

	private ArrayList<ReleaseRecord> parseRelease(final Document document, final CoverArtArchiveTagInfo tagInfo) {
		NodeList nodeList = document.getDocumentElement().getElementsByTagName("release-list");
		if (nodeList.getLength() < 1) {
			return null;
		}
		Element listElement = (Element) nodeList.item(0); // release-list
		nodeList = listElement.getElementsByTagName("release");
		if (nodeList.getLength() < 1) {
			return null;
		}

		Pattern pattern = Pattern.compile("\\d{4}");
		ArrayList<ReleaseRecord> releaseList = new ArrayList<>(nodeList.getLength());
		int nodeListLength = nodeList.getLength();
		for (int i = 0; i < nodeListLength; i++) {
			if (nodeList.item(i) instanceof Element) {
				Element releaseElement = (Element) nodeList.item(i);
				ReleaseRecord release = new ReleaseRecord();
				release.id = releaseElement.getAttribute("id");
				try {
					release.score = Integer.parseInt(releaseElement.getAttribute("ext:score"));
				} catch (NumberFormatException e) {
					release.score = 0;
				}
				try {
					release.title = getChildElement(releaseElement, "title").getTextContent();
				} catch (NullPointerException e) {
					release.title = null;
				}
				Element releaseGroup = getChildElement(releaseElement, "release-group");
				if (releaseGroup != null) {
					try {
						release.type = ReleaseType.valueOf(getChildElement(releaseGroup, "primary-type").getTextContent());
					} catch (IllegalArgumentException | NullPointerException e) {
						release.type = null;
					}
				}
				Element releaseYear = getChildElement(releaseElement, "date");
				if (releaseYear != null) {
					release.year = releaseYear.getTextContent();
					Matcher matcher = pattern.matcher(release.year);
					if (matcher.find()) {
						release.year = matcher.group();
					} else {
						release.year = null;
					}
				} else {
					release.year = null;
				}
				Element artists = getChildElement(releaseElement, "artist-credit");
				if (artists != null && artists.getChildNodes().getLength() > 0) {
					NodeList artistList = artists.getChildNodes();
					for (int j = 0; j < artistList.getLength(); j++) {
						Node node = artistList.item(j);
						if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals("name-credit") && node instanceof Element) {
							Element artistElement = getChildElement((Element) node, "artist");
							if (artistElement != null) {
								Element artistNameElement = getChildElement(artistElement, "name");
								if (artistNameElement != null) {
									release.artists.add(artistNameElement.getTextContent());
								}
							}

						}
					}
				}
				if (StringUtil.hasValue(release.id)) {
					releaseList.add(release);
				}
			}
		}
		return releaseList;
	}

	private ArrayList<ReleaseRecord> parseRecording(final Document document, final CoverArtArchiveTagInfo tagInfo) {
		NodeList nodeList = document.getDocumentElement().getElementsByTagName("recording-list");
		if (nodeList.getLength() < 1) {
			return null;
		}
		Element listElement = (Element) nodeList.item(0); // recording-list
		nodeList = listElement.getElementsByTagName("recording");
		if (nodeList.getLength() < 1) {
			return null;
		}

		Pattern pattern = Pattern.compile("\\d{4}");
		ArrayList<ReleaseRecord> releaseList = new ArrayList<>(nodeList.getLength());
		for (int i = 0; i < nodeList.getLength(); i++) {
			if (nodeList.item(i) instanceof Element) {
				Element recordingElement = (Element) nodeList.item(i);
				ReleaseRecord releaseTemplate = new ReleaseRecord();

				try {
					releaseTemplate.score = Integer.parseInt(recordingElement.getAttribute("ext:score"));
				} catch (NumberFormatException e) {
					releaseTemplate.score = 0;
				}

				// A slight misuse of release.title here, we store the track name
				// here. It is accounted for in the matching logic.
				try {
					releaseTemplate.title = getChildElement(recordingElement, "title").getTextContent();
				} catch (NullPointerException e) {
					releaseTemplate.title = null;
				}

				Element artists = getChildElement(recordingElement, "artist-credit");
				if (artists != null && artists.getChildNodes().getLength() > 0) {
					NodeList artistList = artists.getChildNodes();
					for (int j = 0; j < artistList.getLength(); j++) {
						Node node = artistList.item(j);
						if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals("name-credit") && node instanceof Element) {
							Element artistElement = getChildElement((Element) node, "artist");
							if (artistElement != null) {
								Element artistNameElement = getChildElement(artistElement, "name");
								if (artistNameElement != null) {
									releaseTemplate.artists.add(artistNameElement.getTextContent());
								}
							}

						}
					}
				}

				Element releaseListElement = getChildElement(recordingElement, "release-list");
				if (releaseListElement != null) {
					NodeList releaseNodeList = releaseListElement.getElementsByTagName("release");
					int releaseNodeListLength = releaseNodeList.getLength();
					for (int j = 0; j < releaseNodeListLength; j++) {
						ReleaseRecord release = new ReleaseRecord(releaseTemplate);
						Element releaseElement = (Element) releaseNodeList.item(j);
						release.id = releaseElement.getAttribute("id");
						Element releaseGroup = getChildElement(releaseElement, "release-group");
						if (releaseGroup != null) {
							try {
								release.type = ReleaseType.valueOf(getChildElement(releaseGroup, "primary-type").getTextContent());
							} catch (IllegalArgumentException | NullPointerException e) {
								release.type = null;
							}
						}
						Element releaseYear = getChildElement(releaseElement, "date");
						if (releaseYear != null) {
							release.year = releaseYear.getTextContent();
							Matcher matcher = pattern.matcher(release.year);
							if (matcher.find()) {
								release.year = matcher.group();
							} else {
								release.year = null;
							}
						} else {
							release.year = null;
						}

						if (StringUtil.hasValue(release.id)) {
							releaseList.add(release);
						}
					}
				}
			}
		}
		return releaseList;
	}
}
