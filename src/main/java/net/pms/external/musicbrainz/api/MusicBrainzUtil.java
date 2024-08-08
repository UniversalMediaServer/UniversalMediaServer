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
package net.pms.external.musicbrainz.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableMusicBrainzReleases;
import net.pms.util.StringUtil;
import net.pms.util.XmlUtils;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
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
 * This class is responsible for fetching music data from MusicBrainz.
 *
 * It handles database caching and http lookup of both MusicBrainz ID's (MBID)
 * from MusicBrainz API.
 */
public class MusicBrainzUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(MusicBrainzUtil.class);
	private static final long WAIT_TIMEOUT_MS = 30000;
	private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000L; // 24 hours
	private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = XmlUtils.xxeDisabledDocumentBuilderFactory();
	private static final String ENCODING = StandardCharsets.UTF_8.name();

	/**
	 * This class is not meant to be instantiated.
	 */
	private MusicBrainzUtil() {
	}

	private static final Object TAG_LATCHES_LOCK = new Object();
	private static final List<MusicBrainzTagLatch> TAG_LATCHES = new ArrayList<>();

	/**
	 * Used to serialize search on a per {@link Tag} basis. Every thread doing
	 * a search much hold a {@link MusicBrainzTagLatch} and release it when
	 * the search is done and the result is written. Any other threads
	 * attempting to search for the same {@link Tag} will wait for the existing
	 * {@link MusicBrainzTagLatch} to be released, and can then use the
	 * results from the previous thread instead of conducting it's own search.
	 */
	private static MusicBrainzTagLatch reserveTagLatch(final MusicBrainzTagInfo tagInfo) {
		MusicBrainzTagLatch tagLatch = null;

		boolean owner = false;
		long startTime = System.currentTimeMillis();

		while (!owner && !Thread.currentThread().isInterrupted()) {

			// Find if any other thread is currently searching the same tag
			synchronized (TAG_LATCHES_LOCK) {
				for (MusicBrainzTagLatch latch : TAG_LATCHES) {
					if (latch.info.equals(tagInfo)) {
						tagLatch = latch;
						break;
					}
				}
				// None found, our turn
				if (tagLatch == null) {
					tagLatch = new MusicBrainzTagLatch(tagInfo);
					TAG_LATCHES.add(tagLatch);
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

	private static void releaseTagLatch(MusicBrainzTagLatch tagLatch) {
		synchronized (TAG_LATCHES_LOCK) {
			if (!TAG_LATCHES.remove(tagLatch)) {
				LOGGER.error("Concurrency error: Held tagLatch not found in latchList");
			}
		}
		tagLatch.latch.countDown();
	}

	private static String fuzzString(String s) {
		String[] words = s.split(" ");
		StringBuilder sb = new StringBuilder("(");
		for (String word : words) {
			sb.append(StringUtil.luceneEscape(word)).append("~ ");
		}
		sb.append(')');
		return sb.toString();
	}

	private static String buildMBReleaseQuery(final MusicBrainzTagInfo tagInfo, final boolean fuzzy) {
		final String and = urlEncode(" AND ");
		StringBuilder query = new StringBuilder("release/?query=");
		boolean added = false;

		if (isNotBlank(tagInfo.album)) {
			if (fuzzy) {
				query.append(urlEncode(fuzzString(tagInfo.album)));
			} else {
				query.append(urlEncode("\"" + StringUtil.luceneEscape(tagInfo.album) + "\""));
			}
			added = true;
		}

		/*
		 * Release (album) artist is usually the music director of the album.
		 * Track (Recording) artist is usually the singer. Searching release
		 * with artist here is likely to return no result.
		 */

		if (
			isNotBlank(tagInfo.trackId) &&
			(
				isBlank(tagInfo.album) ||
				!(
					isNotBlank(tagInfo.artist) ||
					isNotBlank(tagInfo.artistId)
				)
			)
		) {
			if (added) {
				query.append(and);
			}
			query.append("tid:").append(tagInfo.trackId);
			added = true;
		} else if (
			isNotBlank(tagInfo.title) &&
			(
				isBlank(tagInfo.album) ||
				!(
					isNotBlank(tagInfo.artist) ||
					isNotBlank(tagInfo.artistId)
				)
			)
		) {
			if (added) {
				query.append(and);
			}
			query.append("recording:");
			if (fuzzy) {
				query.append(urlEncode(fuzzString(tagInfo.title)));
			} else {
				query.append(urlEncode("\"" + StringUtil.luceneEscape(tagInfo.title) + "\""));
			}
			added = true;
		}

		if (!fuzzy && isNotBlank(tagInfo.year) && tagInfo.year.trim().length() > 3) {
			if (added) {
				query.append(and);
			}
			query.append("date:").append(urlEncode(tagInfo.year)).append('*');
		}
		return query.toString();
	}

	private static String buildMBRecordingQuery(final MusicBrainzTagInfo tagInfo, final boolean fuzzy) {
		final String and = urlEncode(" AND ");
		StringBuilder query = new StringBuilder("recording/?query=");
		boolean added = false;

		if (isNotBlank(tagInfo.title)) {
			if (fuzzy) {
				query.append(urlEncode(fuzzString(tagInfo.title)));
			} else {
				query.append(urlEncode("\"" + StringUtil.luceneEscape(tagInfo.title) + "\""));
			}
			added = true;
		}

		if (isNotBlank(tagInfo.trackId)) {
			if (added) {
				query.append(and);
			}
			query.append("tid:").append(tagInfo.trackId);
			added = true;
		}

		if (isNotBlank(tagInfo.artistId)) {
			if (added) {
				query.append(and);
			}
			query.append("arid:").append(tagInfo.artistId);
			added = true;
		} else if (isNotBlank(tagInfo.artist)) {
			if (added) {
				query.append(and);
			}
			query.append("artistname:");
			if (fuzzy) {
				query.append(urlEncode(fuzzString(tagInfo.artist)));
			} else {
				query.append(urlEncode("\"" + StringUtil.luceneEscape(tagInfo.artist) + "\""));
			}
		}

		if (!fuzzy && isNotBlank(tagInfo.year) && tagInfo.year.trim().length() > 3) {
			if (added) {
				query.append(and);
			}
			query.append("date:").append(urlEncode(tagInfo.year)).append('*');
		}
		return query.toString();
	}

	public static String getMBID(Tag tag, boolean externalNetwork) {
		if (tag == null) {
			return null;
		}

		// No need to look up MBID if it's already in the tag
		String mBID = null;
		if (tagSupportsFieldKey(tag, FieldKey.MUSICBRAINZ_RELEASEID)) {
			mBID = tag.getFirst(FieldKey.MUSICBRAINZ_RELEASEID);
			if (isNotBlank(mBID)) {
				return mBID;
			}
		}

		DocumentBuilder builder;
		try {
			builder = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			LOGGER.error("Error initializing XML parser: {}", e.getMessage());
			LOGGER.trace("", e);
			return null;
		}

		final MusicBrainzTagInfo tagInfo = new MusicBrainzTagInfo(tag);
		if (!tagInfo.hasInfo()) {
			LOGGER.trace("Tag has no information - aborting search");
			return null;
		}

		// Secure exclusive access to search for this tag
		MusicBrainzTagLatch latch = reserveTagLatch(tagInfo);
		if (latch == null) {
			// Couldn't reserve exclusive access, giving up
			LOGGER.error("Could not reserve tag latch for MBID search for \"{}\"", tagInfo);
			return null;
		}
		Connection dbconn = MediaDatabase.getConnectionIfAvailable();
		try {
			// Check if it's cached first
			if (dbconn != null) {
				MediaTableMusicBrainzReleases.MusicBrainzReleasesResult result = MediaTableMusicBrainzReleases.findMBID(dbconn, tagInfo);
				if (result.isFound()) {
					if (result.hasMusicBrainzId()) {
						return result.getMusicBrainzId();
					} else if (System.currentTimeMillis() - result.getModifiedTime() < EXPIRATION_TIME) {
						// If a lookup has been done within expireTime and no result,
						// return null. Do another lookup after expireTime has passed
						return null;
					}
				}
			}

			if (!externalNetwork) {
				LOGGER.warn("Can't look up data MBID from MusicBrainz since external network is disabled");
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
			if (isNotBlank(tagInfo.album) || isNotBlank(tagInfo.artist) || isNotBlank(tagInfo.artistId)) {
				round = 1;
			} else {
				round = 3;
			}

			while (round < 5 && isBlank(mBID)) {
				String query;

				if (round < 3) {
					query = buildMBReleaseQuery(tagInfo, round > 1);
				} else {
					query = buildMBRecordingQuery(tagInfo, round > 3);
				}

				if (isNotBlank(query)) {
					final String url = "http://musicbrainz.org/ws/2/" + query + "&fmt=xml";
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Performing release MBID lookup at musicbrainz: \"{}\"", url);
					}

					try {
						HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
						connection.setRequestProperty("Accept-Charset", StandardCharsets.UTF_8.name());
						int status = connection.getResponseCode();
						if (status != 200) {
							LOGGER.error(
								"Could not lookup audio data for \"{}\": musicbrainz.org replied with status code {}",
								tagInfo.title,
								status
							);
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
							releaseList = parseRelease(document);
						} else {
							releaseList = parseRecording(document);
						}

						if (releaseList != null && !releaseList.isEmpty()) {
							// Try to find the best match - this logic can be refined if
							// matching quality turns out to be to low
							int maxScore = 0;
							for (ReleaseRecord release : releaseList) {
								boolean found = false;
								if (isNotBlank(tagInfo.artist)) {
									String[] tagArtists = tagInfo.artist.split("[,&]");
									for (String artist : release.artists) {
										for (String tagArtist : tagArtists) {
											if (StringUtil.isEqual(tagArtist, artist, false, true, true, null)) {
												release.score += 30;
												found = true;
												break;
											}
										}
									}
								}
								if (isNotBlank(tagInfo.album)) {
									if (StringUtil.isEqual(tagInfo.album, release.album, false, true, true, null)) {
											release.score += 30;
											found = true;
									}
								}
								if (isNotBlank(tagInfo.title)) {
									if (StringUtil.isEqual(tagInfo.title, release.title, false, true, true, null)) {
										release.score += 40;
										found = true;
									}
								}
								if (isNotBlank(tagInfo.year) && isNotBlank(release.year)) {
									if (StringUtil.isSameYear(tagInfo.year, release.year)) {
										release.score += 20;
									}
								}
								// Prefer Single > Album > Compilation
								if (found) {
									if (release.type == ReleaseType.Single) {
										release.score += 20;
									} else if (release.type == null || release.type == ReleaseType.Album) {
										release.score += 10;
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

						if (isNotBlank(mBID)) {
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
			if (isNotBlank(mBID)) {
				LOGGER.debug("MusicBrainz release ID \"{}\" found for \"{}\"", mBID, tagInfo);
				if (dbconn != null) {
					MediaTableMusicBrainzReleases.writeMBID(dbconn, mBID, tagInfo);
				}
				return mBID;
			}
			LOGGER.debug("No MusicBrainz release found for \"{}\"", tagInfo);
			if (dbconn != null) {
				MediaTableMusicBrainzReleases.writeMBID(dbconn, null, tagInfo);
			}
			return null;
		} finally {
			MediaDatabase.close(dbconn);
			releaseTagLatch(latch);
		}
	}

	private static ArrayList<ReleaseRecord> parseRelease(final Document document) {
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
			if (nodeList.item(i) instanceof Element releaseElement) {
				ReleaseRecord release = new ReleaseRecord();
				release.id = releaseElement.getAttribute("id");
				try {
					release.score = Integer.parseInt(releaseElement.getAttribute("ext:score"));
				} catch (NumberFormatException e) {
					release.score = 0;
				}
				try {
					release.album = getChildElement(releaseElement, "title").getTextContent();
				} catch (NullPointerException e) {
					release.album = null;
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
						if (
							node.getNodeType() == Node.ELEMENT_NODE &&
							node.getNodeName().equals("name-credit") &&
							node instanceof Element
						) {
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
				if (isNotBlank(release.id)) {
					releaseList.add(release);
				}
			}
		}
		return releaseList;
	}

	private static ArrayList<ReleaseRecord> parseRecording(final Document document) {
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
			if (nodeList.item(i) instanceof Element recordingElement) {
				ReleaseRecord releaseTemplate = new ReleaseRecord();

				try {
					releaseTemplate.score = Integer.parseInt(recordingElement.getAttribute("ext:score"));
				} catch (NumberFormatException e) {
					releaseTemplate.score = 0;
				}

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
						if (
							node.getNodeType() == Node.ELEMENT_NODE &&
							node.getNodeName().equals("name-credit") &&
							node instanceof Element
						) {
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
						try {
							release.album = getChildElement(releaseElement, "title").getTextContent();
						} catch (NullPointerException e) {
							release.album = null;
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

						if (isNotBlank(release.id)) {
							releaseList.add(release);
						}
					}
				}
			}
		}
		return releaseList;
	}

	/**
	 * Checks if a given {@link Tag} supports a given {@link FieldKey}.
	 *
	 * @param tag the {@link Tag} to check for support
	 * @param key the {@link FieldKey} to check for support for
	 *
	 * @return The result
	 */
	protected static boolean tagSupportsFieldKey(Tag tag, FieldKey key) {
		try {
			tag.getFirst(key);
			return true;
		} catch (UnsupportedOperationException e) {
			return false;
		}
	}

	/**
	 * Convenience method to find the first child {@link Element} of the given
	 * name.
	 *
	 * @param element the {@link Element} to search
	 * @param name the name of the child {@link Element}
	 * @return The found {@link Element} or null if not found
	 */
	protected static Element getChildElement(Element element, String name) {
		NodeList list = element.getElementsByTagName(name);
		int listLength = list.getLength();
		for (int i = 0; i < listLength; i++) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(name) && node instanceof Element) {
				return (Element) node;
			}
		}
		return null;
	}

	/**
	 * Convenience method to URL encode a string with {@link #encoding} without
	 * handling the hypothetical {@link UnsupportedEncodingException}
	 * @param url {@link String} to encode
	 * @return The encoded {@link String}
	 */
	protected static String urlEncode(String url) {
		try {
			return URLEncoder.encode(url, ENCODING);
		} catch (UnsupportedEncodingException e) {
			LOGGER.error("UTF-8 is unsupported :O", e);
			return "";
		}
	}

}
