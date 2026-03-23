package net.pms.network.mediaserver.handlers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.configuration.UmsConfiguration;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableAudioMetadata;
import net.pms.dlna.DidlHelper;
import net.pms.formats.Format;
import net.pms.media.audio.metadata.AlbumMetadata;
import net.pms.network.mediaserver.HTTPXMLHelper;
import net.pms.network.mediaserver.handlers.message.SearchRequest;
import net.pms.renderers.Renderer;
import net.pms.store.DbIdLibrary;
import net.pms.store.DbIdMediaType;
import net.pms.store.DbIdResourceLocator;
import net.pms.store.DbIdTypeAndIdent;
import net.pms.store.MediaStoreIds;
import net.pms.store.StoreResource;
import net.pms.store.container.MusicAlbumFolder;
import net.pms.store.container.MusicBrainzPersonFolder;

public abstract class BaseSearchRequestHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(LucenseSearchRequestHandler.class);

	protected static final String CRLF = "\r\n";
	protected static final Pattern CLASS_PATTERN = Pattern.compile("upnp:class\\s(\\bderivedfrom\\b|=)\\s+\"(?<val>.*?)\"",
		Pattern.CASE_INSENSITIVE);
	protected static final Pattern ARTIST_ROLE = Pattern.compile("upnp:class.*role\\s*=\\s*\"(?<val>.*?)\".*", Pattern.CASE_INSENSITIVE);

	public record SearchToken(String attr, String op, String val) {
	}

	private SearchRequest requestMessage = null;
	private DbIdMediaType requestType = null;
	private List<SearchToken> tokens = null;

	private static UmsConfiguration umsConfiguration;

	public BaseSearchRequestHandler(SearchRequest requestMessage) {
		this.requestMessage = requestMessage;
		requestType = calcRequestType(requestMessage.getSearchCriteria());
		SearchRequestTokenizer tokenizer = new SearchRequestTokenizer(requestMessage);
		tokens = tokenizer.getSearchTokens();
	}


	private DbIdMediaType calcRequestType(String searchCriteria) {
		Matcher matcher = CLASS_PATTERN.matcher(searchCriteria);
		if (matcher.find()) {
			String propertyValue = matcher.group("val");
			LOGGER.trace("upnp:class is {}", propertyValue);
			if (propertyValue != null) {
				propertyValue = propertyValue.toLowerCase();
				// More specific types must be checked first
				if (propertyValue.startsWith("object.item.audioitem")) {
					return DbIdMediaType.TYPE_AUDIO;
				} else if (propertyValue.startsWith("object.item.videoitem")) {
					return DbIdMediaType.TYPE_VIDEO;
				} else if (propertyValue.startsWith("object.item.imageitem")) {
					return DbIdMediaType.TYPE_IMAGE;
				} else if (propertyValue.startsWith("object.container.person")) {
					return resolveRolePerson(searchCriteria);
				} else if (propertyValue.startsWith("object.container.album")) {
					return DbIdMediaType.TYPE_ALBUM;
				} else if (propertyValue.startsWith("object.container.playlistcontainer")) {
					return DbIdMediaType.TYPE_PLAYLIST;
				} else if (propertyValue.startsWith("object.container")) {
					return DbIdMediaType.TYPE_FOLDER;
				}
			}
		}
		throw new RuntimeException("Unknown type : " + (searchCriteria != null ? searchCriteria : "NULL"));
	}

	protected DbIdMediaType resolveRolePerson(String searchCriteria) {
		Matcher matcher = ARTIST_ROLE.matcher(searchCriteria);
		if (matcher.find()) {
			String roleValue = matcher.group("val");
			if ("composer".equalsIgnoreCase(roleValue)) {
				LOGGER.debug("looking up artist composer");
				return DbIdMediaType.TYPE_PERSON_COMPOSER;
			} else if ("conductor".equalsIgnoreCase(roleValue)) {
				LOGGER.debug("looking up artist conductor");
				return DbIdMediaType.TYPE_PERSON_CONDUCTOR;
			} else if ("AlbumArtist".equalsIgnoreCase(roleValue)) {
				LOGGER.debug("looking up artist AlbumArtist");
				return DbIdMediaType.TYPE_PERSON_ALBUMARTIST;
			}
			LOGGER.warn("unknown artist role {}. Fallback to artist search ... ", roleValue);
			return DbIdMediaType.TYPE_PERSON;
		} else {
			LOGGER.trace("artist without role. Regular artist search.");
			return DbIdMediaType.TYPE_PERSON;
		}
	}

	protected String escapeH2dbSql(String val) {
		val = val.replaceAll("'", "''");

		// Unicode #2018 is send by iOS (since iOS11) if "Smart Punctuation" is
		// active.
		val = val.replaceAll("‘", "''");
		return val;
	}

	protected String getField(String prop, DbIdMediaType requestType) {
		String property = prop.toLowerCase();
		if ("dc:title".equalsIgnoreCase(property)) {
			// handle title by return type.
			return getTitlePropertyMapping(requestType);
		} else if (property.startsWith("upnp:artist")) {
			// check for @role=composer, @role=conductor or @role=albumartist
			if (property.contains("albumartist")) {
				return " A.ALBUMARTIST ";
			} else if (property.contains("composer")) {
				return " A." + MediaTableAudioMetadata.COL_COMPOSER + " ";
			} else if (property.contains("conductor")) {
				return " A." + MediaTableAudioMetadata.COL_CONDUCTOR + " ";
			}
			// no role, just the artist
			return " A.ARTIST ";
		} else if ("upnp:genre".equals(property)) {
			return " A.GENRE ";
		} else if ("dc:creator".equals(property)) {
			return " A.ALBUMARTIST ";
		} else if ("upnp:album".equals(property)) {
			return " A.ALBUM ";
		} else if ("upnp:rating".equals(property)) {
			return " rating ";
		} else if ("ums:likedalbum".equals(property)) {
			// Makes less sense in a  score based search. Maybe we can use this property in a future implementation to mark albums as
			// liked in the database and then use this information to boost the score of liked albums in the search results.
			return " ";
		} else if ("ums:score".equals(property)) {
			return " score ";
		}

		throw new RuntimeException("unknown or unimplemented property: >" + property + "<");
	}

	protected String getTitlePropertyMapping(DbIdMediaType requestType) {
		switch (requestType) {
			case TYPE_AUDIO -> {
				return " A.SONGNAME ";
			}
			case TYPE_ALBUM -> {
				return " A.ALBUM ";
			}
			case TYPE_PERSON -> {
				return " A.ARTIST ";
			}
			case TYPE_PERSON_COMPOSER -> {
				return " A.COMPOSER ";
			}
			case TYPE_PERSON_ALBUMARTIST -> {
				return " A.ALBUMARTIST ";
			}
			case TYPE_PERSON_CONDUCTOR -> {
				return " A.CONDUCTOR ";
			}
			case TYPE_PLAYLIST, TYPE_VIDEO, TYPE_IMAGE -> {
				return " F.FILENAME ";
			}
			case TYPE_FOLDER -> {
				return " child.name ";
			}
			default -> {
				// nothing to do
			}
		}
		throw new RuntimeException("Unknown type : " + requestType);
	}

	/**
	 * unpn:class filetype mapping
	 *
	 * @param val
	 * @return
	 */
	protected int getFileType(DbIdMediaType mediaFolderType) {
		// album and persons titles are stored within the RealFile and have
		// therefore no unique id.
		switch (mediaFolderType) {
			case TYPE_AUDIO, TYPE_ALBUM, TYPE_PERSON, TYPE_PERSON_COMPOSER, TYPE_PERSON_CONDUCTOR, TYPE_PERSON_ALBUMARTIST -> {
				return Format.AUDIO;
			}
			case TYPE_VIDEO -> {
				return Format.VIDEO;
			}
			case TYPE_IMAGE -> {
				return Format.IMAGE;
			}
			case TYPE_PLAYLIST -> {
				return Format.PLAYLIST;
			}
			case TYPE_FOLDER -> {
				// do nothing, where not in the FILES table, but in STORE_IDS
			}
			default -> {
				// nothing to do
			}
		}
		throw new RuntimeException("unknown or unimplemented mediafolder type : >" + mediaFolderType + "<");
	}

	/**
	 * Wraps the payload around soap Envelope / Body tags.
	 *
	 * @param payload Soap body as a XML String
	 * @return Soap message as a XML string
	 */
	protected StringBuilder createResponse(String payload) {
		StringBuilder response = new StringBuilder();
		response.append(HTTPXMLHelper.XML_HEADER).append(CRLF);
		response.append(HTTPXMLHelper.SOAP_ENCODING_HEADER).append(CRLF);
		response.append(payload).append(CRLF);
		response.append(HTTPXMLHelper.SOAP_ENCODING_FOOTER).append(CRLF);
		return response;
	}

	protected StringBuilder buildEnvelope(int foundNumberReturned, int totalMatches, long updateID, StringBuilder dlnaItems) {
		StringBuilder response = new StringBuilder();
		response.append(HTTPXMLHelper.SEARCHRESPONSE_HEADER);
		response.append(CRLF);
		response.append(HTTPXMLHelper.RESULT_HEADER);
		response.append(HTTPXMLHelper.DIDL_HEADER);
		response.append(dlnaItems.toString());
		response.append(HTTPXMLHelper.DIDL_FOOTER);
		response.append(HTTPXMLHelper.RESULT_FOOTER);
		response.append(CRLF);
		response.append("<NumberReturned>").append(foundNumberReturned).append("</NumberReturned>");
		response.append(CRLF);
		response.append("<TotalMatches>").append(totalMatches).append("</TotalMatches>");
		response.append(CRLF);
		response.append("<UpdateID>");
		response.append(updateID);
		response.append("</UpdateID>");
		response.append(CRLF);
		response.append(HTTPXMLHelper.SEARCHRESPONSE_FOOTER);
		return response;
	}


	protected SearchRequest getRequestMessage() {
		return requestMessage;
	}

	protected DbIdMediaType getRequestType() {
		return requestType;
	}

	protected List<SearchToken> getTokens() {
		return tokens;
	}

	protected static UmsConfiguration getUmsConfiguration() {
		return umsConfiguration;
	}


	/**
	 * Delivers the total count of elements matching the search criteria. This is needed to deliver the correct totalMatches value in the SearchResult.
	 *
	 * @param searchRequest
	 * @return
	 */
	public int getSearchCountElements(SearchRequest searchRequest) {
		int match = getLibraryResourceCountFromSQL();
		LOGGER.debug("{}", searchRequest.getSearchCriteria());
		LOGGER.debug("  -> count TOTAL MATCHES : {}", match);
		return match;
	}

	/**
	 * Not used at this time. UmsContentDirectoryService creates the response object.
	 * @param renderer
	 * @return
	 */
	protected StringBuilder createSearchResponse(Renderer renderer) {
		int numberReturned = 0;
		StringBuilder dlnaItems = new StringBuilder();

		int totalMatches = getLibraryResourceCountFromSQL();

		for (StoreResource resource : getLibraryResourceFromSQL(renderer)) {
			numberReturned++;
			dlnaItems.append(DidlHelper.getDidlString(resource));
		}

		// Build response message
		StringBuilder response = buildEnvelope(numberReturned, totalMatches, MediaStoreIds.getSystemUpdateId().getValue(), dlnaItems);
		return createResponse(response.toString());
	}

	/**
	 *  Makes a logical DB search and converts result set to items or container.
	 *
	 * @param query
	 * @return
	 *
	 * List of discovered CDS items and containers from the database.
	 */
	public List<StoreResource> getLibraryResourceFromSQL(Renderer renderer) {
		ArrayList<StoreResource> result = new ArrayList<>();

		// SQL statements having 'FILENAME' as a result identifier.
		String query = convertToFilesSql();

		LOGGER.debug("RequestType {} : {}", getRequestType().dbidPrefix, query);
		try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
			if (connection != null) {
				try (Statement statement = connection.createStatement()) {
					try (ResultSet resultSet = statement.executeQuery(query)) {
						Set<String> foundAlbums = new HashSet<>();
						while (resultSet.next()) {
							String filenameField = extractDisplayName(resultSet);
							switch (getRequestType()) {
								case TYPE_ALBUM -> {
									String mbid = resultSet.getString("MBID_RECORD");
									Long discogs = resultSet.getObject("DISCOGS_RELEASE_ID", Long.class);
									if (StringUtils.isBlank(mbid) && discogs == null) {
										// Regular albums can be discovered in the media library
										StoreResource sr = DbIdResourceLocator.getAlbumFromMediaLibrary(renderer, filenameField);
										if (sr != null) {
											result.add(sr);
										}
									} else {
										String identToMatch = null;
										if (StringUtils.isNotBlank(mbid)) {
											identToMatch = mbid;
										} else if (discogs != null) {
											identToMatch = discogs.toString();
										}
										if (!(foundAlbums.contains(identToMatch))) {
											AlbumMetadata album = new AlbumMetadata(mbid, discogs, resultSet.getString("album"), resultSet.getString("artist"),
												Integer.toString(resultSet.getInt("media_year")), resultSet.getString("genre"));
											MusicAlbumFolder folder = DbIdResourceLocator.getLibraryResourceMusicBrainzFolder(
												renderer, album.getTypeIdent());
											if (folder == null) {
												folder = DbIdLibrary.addLibraryResourceMusicAlbum(renderer, album);
											}
											result.add(folder);
											foundAlbums.add(identToMatch);
										}
									}
								}
								case TYPE_PERSON, TYPE_PERSON_COMPOSER, TYPE_PERSON_CONDUCTOR, TYPE_PERSON_ALBUMARTIST -> {
									DbIdTypeAndIdent ti = new DbIdTypeAndIdent(getRequestType(), filenameField);
									MusicBrainzPersonFolder personFolder = DbIdResourceLocator.getLibraryResourcePersonFolder(renderer, ti);
									if (personFolder == null) {
										personFolder = DbIdLibrary.addLibraryResourcePerson(renderer, ti);
									}
									result.add(personFolder);
								}
								case TYPE_PLAYLIST -> {
									String realFileName = resultSet.getString("FILENAME");
									if (realFileName != null) {
										StoreResource res = DbIdResourceLocator.getLibraryResourcePlaylist(renderer, realFileName);
										if (res != null) {
											result.add(res);
										}
									}
								}
								case TYPE_FOLDER -> {
									if (filenameField != null) {
										StoreResource res = DbIdResourceLocator.getLibraryResourceFolder(renderer, filenameField);
										if (res != null) {
											result.add(res);
										}
									}
								}
								default -> {
									String realFileName = resultSet.getString("FILENAME");
									if (realFileName != null) {
										StoreResource res = DbIdResourceLocator.getLibraryResourceRealFile(renderer, realFileName);
										if (res != null) {
											res.resolve();
											result.add(res);
										}
									}
								}
							}
						}
					}
				}
			} else {
				LOGGER.warn("No database connection available to execute getLibraryResourceFromSQL query.");
			}
		} catch (SQLException e) {
			LOGGER.warn("getLibraryResourceFromSQL", e);
		}
		LOGGER.debug("  -> elements found : {}", result.size());
		return result;
	}

	private String extractDisplayName(ResultSet resultSet) throws SQLException {
		switch (getRequestType()) {
			case TYPE_VIDEO, TYPE_PLAYLIST, TYPE_IMAGE, TYPE_AUDIO -> {
				return FilenameUtils.getBaseName(resultSet.getString("FILENAME"));
			}
			case TYPE_FOLDER -> {
				return resultSet.getString("name");
			}
			default -> {
				// artificial field 'filename' of a person or similar type is
				// already the final display name.
				return resultSet.getString("FILENAME");
			}
		}
	}

	protected int getLibraryResourceCountFromSQL() {
		String query = convertToCountSql();

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(String.format("SQL count : %s", query));
		}

		try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
			if (connection != null) {
				try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(query)) {
					if (resultSet.next()) {
						return resultSet.getInt(1);
					}
				} catch (SQLException e) {
					LOGGER.trace("getLibraryResourceCountFromSQL", e);
				}
			} else {
				LOGGER.warn("No database connection available to execute count query.");
				return 0;
			}
		} catch (Exception e) {
			LOGGER.warn("getLibraryResourceCountFromSQL", e);
		}
		return 0;
	}


	protected abstract String convertToFilesSql();

	protected abstract String convertToCountSql();

	static {
		try {
			umsConfiguration = new UmsConfiguration();
		} catch (ConfigurationException | InterruptedException e) {
			LOGGER.error("Error while initializing SearchRequestHandler : ", e);
			throw new RuntimeException(e);
		}
	}

}
