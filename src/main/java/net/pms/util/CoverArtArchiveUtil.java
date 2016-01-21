/*
 * Universal Media Server, for streaming any medias to DLNA
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
import net.pms.database.TableMusicBrainzReleases;
import net.pms.database.TableMusicBrainzReleases.MusicBrainzReleasesResult;
import org.apache.commons.io.IOUtils;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import fm.last.musicbrainz.coverart.CoverArt;
import fm.last.musicbrainz.coverart.CoverArtException;
import fm.last.musicbrainz.coverart.CoverArtImage;
import fm.last.musicbrainz.coverart.impl.DefaultCoverArtArchiveClient;

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
	private static Object builderLock = new Object();
	private static DocumentBuilder builder;
	private static long expireTime = 24 * 60 * 60 * 1000; // 24 hours

	static {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		synchronized (builderLock) {
			try {
				builder = factory.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				LOGGER.error("Error initializing CoverUtil: {}", e.getMessage());
				LOGGER.trace("", e);
				builder = null;
			}
		}
	}

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

	}

	/**
	 * This class is a container to hold information used by
	 * {@link CoverArtArchiveUtil} to look up covers.
	 */
	public static class CoverArtArchiveInfo {
		final String album;
		final String artist;
		final String title;
		final String year;
		final String artistId;
		final String trackId;

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
			if (!(obj instanceof CoverArtArchiveInfo)) {
				return false;
			}
			CoverArtArchiveInfo other = (CoverArtArchiveInfo) obj;
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

		public CoverArtArchiveInfo(Tag tag) {
			album = tag.getFirst(FieldKey.ALBUM);
			artist = tag.getFirst(FieldKey.ARTIST);
			title = tag.getFirst(FieldKey.TITLE);
			year = tag.getFirst(FieldKey.YEAR);
			artistId = tag.getFirst(FieldKey.MUSICBRAINZ_ARTISTID);
			trackId = tag.getFirst(FieldKey.MUSICBRAINZ_TRACK_ID);
		}
	}

	private static class CoverArtArchiveLatch {
		final CoverArtArchiveInfo info;
		final CountDownLatch latch = new CountDownLatch(1);

		public CoverArtArchiveLatch(CoverArtArchiveInfo info) {
			this.info = info;
		}
	}

	/**
	 * Do not instantiate this class, use {@link CoverUtil#get()}
	 */
	protected CoverArtArchiveUtil() {
	}

	private static final Object tagLatchListLock = new Object();
	private static final List<CoverArtArchiveLatch> tagLatchList = new ArrayList<>();

	/**
	 * Used to serialize search on a per {@link Tag} basis. Every thread doing
	 * a search much hold a {@link CoverArtArchiveLatch} and release it when
	 * the search are done and the results are written. Any other threads
	 * attempting to search for the same {@link Tag} will wait for the existing
	 * {@link CoverArtArchiveLatch} to be released, and can then use the
	 * results from the previous thread instead of conducting it's own search.
	 */
	private static CoverArtArchiveLatch reserveTagLatch(final CoverArtArchiveInfo tagInfo) {
		CoverArtArchiveLatch tagLatch = null;

		boolean owner = false;
		long startTime = System.currentTimeMillis();

		while (!owner && !Thread.currentThread().isInterrupted()) {

			// Find if any other tread is currently searching the same tag
			synchronized (tagLatchListLock) {
				for (CoverArtArchiveLatch latch : tagLatchList) {
					if (latch.info.equals(tagInfo)) {
						tagLatch = latch;
						break;
					}
				}
				// None found, our turn
				if (tagLatch == null) {
					tagLatch = new CoverArtArchiveLatch(tagInfo);
					tagLatchList.add(tagLatch);
					owner = true;
				}
			}

			// Check for timeout here instead of in the while loop make logging
			// it easier.
			if (System.currentTimeMillis() - startTime > WAIT_TIMEOUT_MS) {
				LOGGER.debug("A Cover Art Achive search times out while waiting it's turn");
				return null;
			}

			if (!owner) {
				try {
					tagLatch.latch.await();
				} catch (InterruptedException e) {
					LOGGER.debug("A Cover Art Archive search was interrupted while waiting it's turn");
					Thread.currentThread().interrupt();
					return null;
				} finally {
					tagLatch = null;
				}
			}
		}

		return tagLatch;
	}

	private static void releaseTagLatch(CoverArtArchiveLatch tagLatch) {
		synchronized (tagLatchListLock) {
			if (!tagLatchList.remove(tagLatch)) {
				LOGGER.error("Concurrency error: Held tagLatch not found in latchList");
			}
		}
		tagLatch.latch.countDown();
	}

	@Override
	protected byte[] doGetThumbnail(Tag tag, boolean externalNetwork) {
		String mBID = getMBID(tag, externalNetwork);
		if (mBID != null) {
			// TODO: Check cache
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
				return null;
			}
			CoverArtImage image = coverArt.getFrontImage();
			if (image == null) {
				image = coverArt.getImages().get(0);
			}
			try (InputStream is = image.getLargeThumbnail()) {
				return IOUtils.toByteArray(is);
			} catch (IOException e) {
				LOGGER.error("An error occurred while downloading cover for MBID \"{}\": {}", mBID, e.getMessage());
				LOGGER.trace("", e);
				return null;
			}
		}
		return null;
	}

	private String getMBID(Tag tag, boolean externalNetwork) {
		if (tag == null) {
			return null;
		}

		// No need to look up MBID if it's already in the tag
		String mBID = tag.getFirst(FieldKey.MUSICBRAINZ_RELEASEID);
		if (StringUtil.hasValue(mBID)) {
			return mBID;
		}

		final CoverArtArchiveInfo tagInfo = new CoverArtArchiveInfo(tag);

		// Secure exclusive access to search for this tag
		CoverArtArchiveLatch latch = reserveTagLatch(tagInfo);
		try {

			// Check if it's cached first
			MusicBrainzReleasesResult result = TableMusicBrainzReleases.findMBID(tag);
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

			String query = null;
			if (StringUtil.hasValue(tagInfo.album)) {
				query = urlEncode("\"" + tagInfo.album + "\"");
			}
			if (StringUtil.hasValue(tagInfo.artistId)) {
				query = (query != null ? query + "%20AND%20" : "") + "artist:" + tagInfo.artistId;
			} else if (StringUtil.hasValue(tagInfo.artist)) {
				query = (query != null ? query + "%20AND%20" : "") + "artist:" + urlEncode("\"" + tagInfo.artist + "\"");
			}
			if (
				StringUtil.hasValue(tagInfo.trackId) &&
				(!StringUtil.hasValue(tagInfo.album) ||
					!(StringUtil.hasValue(tagInfo.artist) ||
						StringUtil.hasValue(tagInfo.artistId)))
			) {
				query = (query != null ? query + "%20AND%20" : "") + "tid:" + tagInfo.trackId;
			} else if (
				StringUtil.hasValue(tagInfo.title) &&
				(!StringUtil.hasValue(tagInfo.album) ||
					!(StringUtil.hasValue(tagInfo.artist) ||
						StringUtil.hasValue(tagInfo.artistId)))
			) {
				query = (query != null ? query + "%20AND%20" : "") + "recording:" + urlEncode("\"" + tagInfo.title + "\"");
			}

			if (query != null) {
				final String url = "http://musicbrainz.org/ws/2/release/?query=" + query;
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Performing audio lookup at musicbrainz: \"{}\"", url);
				}
				synchronized (builderLock) {
					if (builder == null) {
						LOGGER.error("Cannot initialize XML parser");
						return null;
					}
				}
				try {
					HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
					connection.setRequestProperty("Accept-Charset", StandardCharsets.UTF_8.name());
					int status = connection.getResponseCode();
					if (status != 200) {
						LOGGER.error("Could not lookup audio cover for \"{}\": musicbrainz.com replied with status code {}", tagInfo.title, status);
						return null;
					}

					Document document;
					try {
						synchronized (builderLock) {
							document = builder.parse(connection.getInputStream());
						}
					} catch (SAXException e) {
						LOGGER.error("Failed to parse XML for \"{}\": {}", url, e.getMessage());
						LOGGER.trace("", e);
						return null;
					}

					NodeList nodeList = document.getDocumentElement().getElementsByTagName("release-list");
					if (nodeList.getLength() < 1) {
						LOGGER.debug("No music release found with \"{}\"", url);
						TableMusicBrainzReleases.writeMBID(null, tag);
						return null;
					}
					Element listElement = (Element) nodeList.item(0); // release-list
					nodeList = listElement.getElementsByTagName("release");
					if (nodeList.getLength() < 1) {
						LOGGER.debug("No music release found with \"{}\"", url);
						TableMusicBrainzReleases.writeMBID(null, tag);
						return null;
					}

					Pattern pattern = Pattern.compile("\\d{4}");
					ArrayList<ReleaseRecord> releaseList = new ArrayList<>(nodeList.getLength());
					for (int i = 0; i < nodeList.getLength(); i++) {
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
					if (releaseList.isEmpty()) {
						LOGGER.debug("No music release found with \"{}\"", url);
						TableMusicBrainzReleases.writeMBID(null, tag);
						return null;
					}

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
							if (release.type == ReleaseType.Single && release.title.equalsIgnoreCase(tagInfo.title)) {
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
							TableMusicBrainzReleases.writeMBID(release.id, tag);
							return release.id;
						}
					}
				} catch (IOException e) {
					LOGGER.debug("Failed to find MBID for \"{}\": {}", query, e.getMessage());
					LOGGER.trace("", e);
					return null;
				}
			}
			return null;
		} finally {
			releaseTagLatch(latch);
		}
	}
}
