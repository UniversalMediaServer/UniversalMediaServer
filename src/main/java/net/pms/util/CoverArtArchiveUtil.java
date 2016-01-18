package net.pms.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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


public class CoverArtArchiveUtil extends CoverUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(CoverArtArchiveUtil.class);
	private static Object builderLock = new Object();
	private static DocumentBuilder builder;

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
	 * Do not instantiate this class, use {@link CoverUtil#get()}
	 */
	protected CoverArtArchiveUtil() {
	}

	public byte[] doGetThumbnail(Tag tag) {
		String mbId = getMBId(tag);
		if (mbId != null) {
			DefaultCoverArtArchiveClient client = new DefaultCoverArtArchiveClient();

			CoverArt coverArt;
			try {
				coverArt = client.getByMbid(UUID.fromString(mbId));
			} catch (CoverArtException e) {
				LOGGER.debug("Could not get cover with MBID \"{}\": {}", mbId, e.getMessage());
				LOGGER.trace("", e);
				return null;
			}
			if (coverArt == null || coverArt.getImages().isEmpty()) {
				LOGGER.debug("MBID \"{}\" has no cover at CoverArtArchive", mbId);
				return null;
			}
			CoverArtImage image = coverArt.getFrontImage();
			if (image == null) {
				image = coverArt.getImages().get(0);
			}
			try (InputStream is = image.getLargeThumbnail()) {
				return IOUtils.toByteArray(is);
			} catch (IOException e) {
				LOGGER.error("An error occurred while downloading cover for MBID \"{}\": {}", mbId, e.getMessage());
				LOGGER.trace("", e);
				return null;
			}
		}
		return null;
	}

	private String getMBId(Tag tag) {
		if (tag == null) {
			return null;
		}

		// No need to look up MBID if it's already in the tag
		String MBID = tag.getFirst(FieldKey.MUSICBRAINZ_RELEASEID);
		if (hasValue(MBID)) {
			return MBID;
		}

		final String album = tag.getFirst(FieldKey.ALBUM);
		final String artist = tag.getFirst(FieldKey.ARTIST);
		final String title = tag.getFirst(FieldKey.TITLE);
		final String year = tag.getFirst(FieldKey.YEAR);
		final String artistId = tag.getFirst(FieldKey.MUSICBRAINZ_ARTISTID);
		final String trackId = tag.getFirst(FieldKey.MUSICBRAINZ_TRACK_ID);

		String query = null;
		if (hasValue(album)) {
			query = urlEncode("\"" + album + "\"");
		}
		if (hasValue(artistId)) {
			query = (query != null ? query + "%20AND%20" : "") + "artist:" + artistId;
		} else if (hasValue(artist)) {
			query = (query != null ? query + "%20AND%20" : "") + "artist:" + urlEncode("\"" + artist + "\"");
		}
		if (hasValue(trackId) && (!hasValue(album) || !(hasValue(artist) || hasValue(artistId)))) {
			query = (query != null ? query + "%20AND%20" : "") + "tid:" + trackId;
		} else if (hasValue(title) && (!hasValue(album) || !(hasValue(artist) || hasValue(artistId)))) {
			query = (query != null ? query + "%20AND%20" : "") + "recording:" + urlEncode("\"" + title + "\"");
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
					LOGGER.error("Could not lookup audio cover for \"{}\": musicbrainz.com replied with status code {}", title, status);
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
					return null;
				}
				Element listElement = (Element) nodeList.item(0); // release-list
				nodeList = listElement.getElementsByTagName("release");
				if (nodeList.getLength() < 1) {
					LOGGER.debug("No music release found with \"{}\"", url);
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
						if (hasValue(release.id)) {
							releaseList.add(release);
						}
					}
				}
				if (releaseList.isEmpty()) {
					LOGGER.debug("No music release found with \"{}\"", url);
					return null;
				}

				// Try to find the best match - this logic can be refined if
				// matching quality turns out to be to low
				int maxScore = 0;
				for (ReleaseRecord release : releaseList) {
					if (hasValue(artist)) {
						boolean found = false;
						for (String s : release.artists) {
							if (s.equalsIgnoreCase(artist)) {
								found = true;
								break;
							}
						}
						if (found) {
							release.score += 30;
						}
					}
					if (hasValue(album)) {
						if (release.type == ReleaseType.Album) {
							release.score += 20;
							if (release.title.equalsIgnoreCase(album)) {
								release.score += 30;
							}
						}
					} else if (hasValue(title)) {
						if (release.type == ReleaseType.Single && release.title.equalsIgnoreCase(title)) {
							release.score += 40;
						}
					}
					if (hasValue(year) && hasValue(release.year)) {
						if (year.equals(release.year)) {
							release.score += 20;
						}
					}
					maxScore = Math.max(maxScore, release.score);
				}

				for (ReleaseRecord release : releaseList) {
					if (release.score == maxScore) {
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
	}
}
