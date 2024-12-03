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
package net.pms.store.container;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableAudioMetadata;
import net.pms.database.MediaTableFiles;
import net.pms.database.MediaTableMusicBrainzReleaseLike;
import net.pms.database.MediaTableStoreIds;
import net.pms.media.audio.metadata.DoubleRecordFilter;
import net.pms.media.audio.metadata.MusicBrainzAlbum;
import net.pms.renderers.Renderer;
import net.pms.store.DbIdMediaType;
import net.pms.store.DbIdResourceLocator;
import net.pms.store.DbIdTypeAndIdent;
import net.pms.store.StoreResource;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This StoreContainer implements support for RealFileDbId's database backed
 * IDs.
 */
public class VirtualFolderDbId extends LocalizedStoreContainer {

	private static final Logger LOGGER = LoggerFactory.getLogger(VirtualFolderDbId.class);

	private final DbIdTypeAndIdent typeIdent;

	public VirtualFolderDbId(Renderer renderer, String i18nName, DbIdTypeAndIdent typeIdent) {
		super(renderer, i18nName, null);
		this.typeIdent = typeIdent;
		setId(typeIdent.toString());
	}

	@Override
	public boolean isDiscovered() {
		return false;
	}

	@Override
	public String getSystemName() {
		return this.typeIdent.toString();
	}

	public String getMediaIdent() {
		return typeIdent.ident;
	}

	public DbIdMediaType getMediaType() {
		return typeIdent.type;
	}

	public DbIdTypeAndIdent getMediaTypeIdent() {
		return typeIdent;
	}

	public String getMediaTypeUclass() {
		return typeIdent.type.uclass;
	}

	@Override
	public void discoverChildren() {
		doRefreshChildren();
	}

	@Override
	public synchronized void doRefreshChildren() {
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				try (Statement statement = connection.createStatement()) {
					List<File> filesListFromDb = null;
					getChildren().clear();
					String sql;
					switch (typeIdent.type) {
						case TYPE_ALBUM -> {
							sql = String.format("SELECT " + MediaTableFiles.TABLE_COL_FILENAME + ", " + MediaTableFiles.TABLE_COL_ID +
									", " + MediaTableFiles.TABLE_COL_MODIFIED + " FROM " + MediaTableFiles.TABLE_NAME + " LEFT OUTER JOIN " +
									MediaTableAudioMetadata.TABLE_NAME + " ON " + MediaTableFiles.TABLE_COL_ID + " = " +
									MediaTableAudioMetadata.TABLE_COL_FILEID + " WHERE ( " + MediaTableFiles.TABLE_COL_FORMAT_TYPE +
									" = 1  AND  " + MediaTableAudioMetadata.TABLE_COL_ALBUM + " = '%s')", StringEscapeUtils.escapeSql(typeIdent.ident));
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace(String.format("SQL AUDIO-ALBUM : %s", sql));
							}
							try (ResultSet resultSet = statement.executeQuery(sql)) {
								filesListFromDb = new ArrayList<>();
								while (resultSet.next()) {
									filesListFromDb.add(new File(resultSet.getString("FILENAME")));
								}
							}
						}
						case TYPE_MUSICBRAINZ_RECORDID -> {
							if (StringUtils.isAllBlank(typeIdent.ident)) {
								LOGGER.debug("collecting all music albums having a musicBrainzId identifier ...");
								sql = "SELECT DISTINCT ON (MBID_RECORD) MBID_RECORD, " +
									MediaTableAudioMetadata.TABLE_COL_ALBUM + ", " + MediaTableAudioMetadata.TABLE_COL_GENRE + ", " +
									MediaTableAudioMetadata.TABLE_COL_ARTIST + ", " + MediaTableAudioMetadata.TABLE_COL_MEDIA_YEAR + " FROM " +
									MediaTableAudioMetadata.TABLE_NAME + " WHERE MBID_RECORD IS NOT NULL";
								if (LOGGER.isTraceEnabled()) {
									LOGGER.trace(String.format("SQL TYPE_MUSICBRAINZ_RECORDID : %s", sql));
								}
								try (ResultSet resultSet = statement.executeQuery(sql)) {
									while (resultSet.next()) {
										MusicBrainzAlbum mbAlbum = new MusicBrainzAlbum(resultSet.getString("MBID_RECORD"), resultSet.getString("ALBUM"),
											resultSet.getString("ARTIST"), Integer.toString(resultSet.getInt("MEDIA_YEAR")), resultSet.getString("GENRE"));
										addChild(new MusicBrainzAlbumFolder(renderer, mbAlbum));
									}
								} catch (Exception e) {
									LOGGER.error("Error in SQL : " + sql, e);
								}
							} else {
								LOGGER.debug("collecting musicBrainz album {}", typeIdent.toString());
								sql = String
									.format("SELECT " + MediaTableFiles.TABLE_COL_FILENAME + ", " + MediaTableAudioMetadata.TABLE_COL_MBID_TRACK +
											", " + MediaTableFiles.TABLE_COL_ID + ", " + MediaTableAudioMetadata.TABLE_COL_ALBUM + " FROM " +
											MediaTableFiles.TABLE_NAME + " LEFT OUTER JOIN " + MediaTableAudioMetadata.TABLE_NAME + " ON " +
											MediaTableFiles.TABLE_COL_ID + " = " + MediaTableAudioMetadata.TABLE_COL_FILEID + " " + "WHERE ( " +
											MediaTableFiles.TABLE_COL_FORMAT_TYPE + " = 1 and " + MediaTableAudioMetadata.TABLE_COL_MBID_RECORD +
											" = '%s' ) ORDER BY " + MediaTableAudioMetadata.TABLE_COL_MBID_TRACK, StringEscapeUtils.escapeSql(typeIdent.ident));
								if (LOGGER.isTraceEnabled()) {
									LOGGER.trace(String.format("SQL TYPE_MUSICBRAINZ_RECORDID : %s", sql));
								}
								try (ResultSet resultSet = statement.executeQuery(sql)) {
									filesListFromDb = new ArrayList<>();
									String lastUuidTrack = "";
									while (resultSet.next()) {
										// Find "best track" logic should be
										// optimized !!
										setName(resultSet.getString(MediaTableAudioMetadata.TABLE_COL_ALBUM));
										String currentUuidTrack = resultSet.getString("MBID_TRACK");
										if (!currentUuidTrack.equals(lastUuidTrack)) {
											lastUuidTrack = currentUuidTrack;
											filesListFromDb.add(new File(resultSet.getString("FILENAME")));
										}
									}
								} catch (Exception e) {
									LOGGER.error("Error in SQL : " + sql, e);
								}
							}
						}
						case TYPE_MYMUSIC_ALBUM -> {
							clearChildren();
							sql = "SELECT DISTINCT ON (MBID_RELEASE) " + MediaTableMusicBrainzReleaseLike.TABLE_COL_MBID_RELEASE + ", " +
									MediaTableAudioMetadata.TABLE_COL_ALBUM + ", " + MediaTableAudioMetadata.TABLE_COL_GENRE + ", " +
									MediaTableAudioMetadata.TABLE_COL_ARTIST + ", " + MediaTableAudioMetadata.TABLE_COL_MEDIA_YEAR + " FROM " +
									MediaTableMusicBrainzReleaseLike.TABLE_NAME + " JOIN " + MediaTableAudioMetadata.TABLE_NAME + " ON " +
									MediaTableMusicBrainzReleaseLike.TABLE_COL_MBID_RELEASE + " = " +
									MediaTableAudioMetadata.TABLE_COL_MBID_RECORD + ";";
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace(String.format("SQL TYPE_MYMUSIC_ALBUM : %s", sql));
							}
							DoubleRecordFilter filter = new DoubleRecordFilter();
							try (ResultSet resultSet = statement.executeQuery(sql)) {
								while (resultSet.next()) {
									filter.addAlbum(generateMusicBrainzAlbum(resultSet));
								}
								for (MusicBrainzAlbum album : filter.getUniqueAlbumSet()) {
									MusicBrainzAlbumFolder albumFolder = new MusicBrainzAlbumFolder(renderer, album);
									addChild(albumFolder);
								}
							}
						}
						case TYPE_PERSON_ALL_FILES -> {
							sql = personAllFilesSql(typeIdent);
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace(String.format("SQL PERSON : %s", sql));
							}
							try (ResultSet resultSet = statement.executeQuery(sql)) {
								filesListFromDb = new ArrayList<>();
								while (resultSet.next()) {
									filesListFromDb.add(new File(resultSet.getString("FILENAME")));
								}
							}
						}
						case TYPE_PERSON_ALBUM -> {
							// Add all albums by a person
							sql = personAlbumSql(typeIdent);
							try (ResultSet resultSet = statement.executeQuery(sql)) {
								while (resultSet.next()) {
									if (resultSet.getString("MBID_RECORD") != null) {
										addChild(new MusicBrainzAlbumFolder(renderer, generateMusicBrainzAlbum(resultSet)));
									} else {
										StoreResource sr = DbIdResourceLocator.getAlbumFromMediaLibrary(renderer, typeIdent.ident);
										if (sr != null) {
											addChild(sr);
										}
									}
								}
							}
						}
						case TYPE_PERSON_ALBUM_FILES -> {
							sql = personAlbumFileSql(typeIdent);
							try (ResultSet resultSet = statement.executeQuery(sql)) {
								filesListFromDb = new ArrayList<>();
								while (resultSet.next()) {
									filesListFromDb.add(new File(resultSet.getString("FILENAME")));
								}
							}
						}
						case TYPE_PERSON, TYPE_PERSON_COMPOSER, TYPE_PERSON_CONDUCTOR, TYPE_PERSON_ALBUMARTIST -> {
							if (StringUtils.isAllBlank(typeIdent.ident)) {
								//
								// If no ident is given means we seek all known person in respect to its role.
								// This folder is filled lazy. Entries will be created after a user submits a search request.
								// Without any submitted search requests with hits this folder will stay empty.
								//
								sql = String.format("SELECT ID, NAME FROM " + MediaTableStoreIds.TABLE_NAME + " WHERE PARENT_ID = %s " +
									"AND name like '%s%s%%'", getLongId(), DbIdMediaType.GENERAL_PREFIX, typeIdent.type.dbidPrefix);
								LOGGER.debug("All person Folder sql : {}", sql);
								try (ResultSet resultSet = statement.executeQuery(sql)) {
									while (resultSet.next()) {
										String name = resultSet.getString("NAME");
										LOGGER.debug("person name is : {}", name);
										DbIdTypeAndIdent tiPerson = new DbIdTypeAndIdent(typeIdent.type, name.substring(name.lastIndexOf("$") + 1));
										MusicBrainzPersonFolder person = new MusicBrainzPersonFolder(renderer, tiPerson.getIdentUnprefixed(), tiPerson);
										addChild(person);
										person.discoverChildren();
									}
								} catch (Exception e) {
									LOGGER.error("TYPE_PERSON, TYPE_PERSON_COMPOSER, TYPE_PERSON_CONDUCTOR, TYPE_PERSON_ALBUMARTIST", e);
								}
							} else {
								// We have a person folder which means, we need to add the two virtual folders "all files" and "by album".
								LOGGER.debug("Person {}", typeIdent.ident);
								if (this instanceof MusicBrainzPersonFolder person) {
									person.discoverChildren();
								} else {
									LOGGER.warn("unknown folder type.");
								}
							}
						}
						case TYPE_FOLDER -> {
							StoreResource res = DbIdResourceLocator.getLibraryResourceFolder(renderer, typeIdent.toString());
							if (res != null) {
								addChild(res);
							}
						}
						default -> throw new RuntimeException("Unknown Type");
					}
					if (filesListFromDb != null) {
						for (File file : filesListFromDb) {
							if (renderer.hasShareAccess(file)) {
								StoreResource sr = renderer.getMediaStore().createResourceFromFile(file);
								if (sr != null) {
									addChild(sr);
								} else {
									LOGGER.trace("createResourceFromFile has failed for {}", file);
								}
							} else {
								LOGGER.debug("renderer has no share access to resource {}", file.getAbsolutePath());
							}
						}
					}
				}
			} else {
				LOGGER.error("database not available !");
			}
		} catch (SQLException e) {
			LOGGER.warn("getLibraryResourceByDBID", e);
		} finally {
			MediaDatabase.close(connection);
		}
		sortChildrenIfNeeded();
	}

	private MusicBrainzAlbum generateMusicBrainzAlbum(ResultSet resultSet) throws SQLException {
		return new MusicBrainzAlbum(resultSet.getString("MBID_RELEASE"), resultSet.getString("ALBUM"),
				resultSet.getString("ARTIST"), Integer.toString(resultSet.getInt("MEDIA_YEAR")), resultSet.getString("GENRE"));
	}

	private static String personAlbumFileSql(DbIdTypeAndIdent typeAndIdent) {
		StringBuilder sb = new StringBuilder();

		sb.append("SELECT ").append(MediaTableFiles.TABLE_COL_FILENAME).append(", ").append(MediaTableFiles.TABLE_COL_ID).append(", ")
				.append(MediaTableFiles.TABLE_COL_MODIFIED).append(" FROM ").append(MediaTableFiles.TABLE_NAME).append(" LEFT OUTER JOIN ")
				.append(MediaTableAudioMetadata.TABLE_NAME).append(" ON ").append(MediaTableFiles.TABLE_COL_ID).append(" = ")
				.append(MediaTableAudioMetadata.TABLE_COL_FILEID).append(" ").append("WHERE (").append(MediaTableAudioMetadata.TABLE_COL_ALBUM)
				.append(" = '").append(StringEscapeUtils.escapeSql(typeAndIdent.getIdentUnprefixed())).append("') AND ( ");
		wherePartPersonByType(typeAndIdent, sb);
		sb.append(")");
		LOGGER.debug("personAlbumFilesSql : {}", sb.toString());
		return sb.toString();
	}

	private static String personAlbumSql(DbIdTypeAndIdent typeAndIdent) {
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT DISTINCT ON (").append(MediaTableAudioMetadata.TABLE_COL_ALBUM).append(")" + MediaTableAudioMetadata.TABLE_COL_ALBUM +
			", MBID_RECORD as MBID_RELEASE, " + MediaTableAudioMetadata.TABLE_COL_GENRE + ", " + MediaTableAudioMetadata.TABLE_COL_ARTIST +
			", " + MediaTableAudioMetadata.TABLE_COL_MEDIA_YEAR + " FROM ").append(MediaTableAudioMetadata.TABLE_NAME).append(" WHERE (");
		wherePartPersonByType(typeAndIdent, sb);
		sb.append(")");
		LOGGER.debug("personAlbumSql : {}", sb.toString());
		return sb.toString();
	}

	private static void wherePartPersonByType(DbIdTypeAndIdent typeAndIdent, StringBuilder sb) {
		String ident = StringEscapeUtils.escapeSql(typeAndIdent.getIdentUnprefixed());
		if (typeAndIdent.ident.startsWith(DbIdMediaType.PERSON_COMPOSER_PREFIX)) {
			sb.append(MediaTableAudioMetadata.TABLE_COL_COMPOSER);
			LOGGER.trace("WHERE PERSON COMPOSER");
		} else if (typeAndIdent.ident.startsWith(DbIdMediaType.PERSON_CONDUCTOR_PREFIX)) {
			sb.append(MediaTableAudioMetadata.TABLE_COL_CONDUCTOR);
			LOGGER.trace("WHERE PERSON CONDUCTOR");
		} else if (typeAndIdent.ident.startsWith(DbIdMediaType.PERSON_ALBUMARTIST_PREFIX)) {
			sb.append(MediaTableAudioMetadata.TABLE_COL_ALBUMARTIST);
			LOGGER.trace("WHERE PERSON ALBUMARTIST");
		} else {
			sb.append(MediaTableAudioMetadata.TABLE_COL_ARTIST);
			LOGGER.trace("WHERE PERSON ARTIST");
		}
		sb.append(" = '").append(ident).append("'");
	}

	private static String personAllFilesSql(DbIdTypeAndIdent typeAndIdent) {
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT ").append(MediaTableFiles.TABLE_COL_FILENAME).append(", ").append(MediaTableFiles.TABLE_COL_ID).append(", ")
				.append(MediaTableFiles.TABLE_COL_MODIFIED).append(" FROM ").append(MediaTableFiles.TABLE_NAME).append(" LEFT OUTER JOIN ")
				.append(MediaTableAudioMetadata.TABLE_NAME).append(" ON ").append(MediaTableFiles.TABLE_COL_ID).append(" = ")
				.append(MediaTableAudioMetadata.TABLE_COL_FILEID).append(" WHERE ( ");
		wherePartPersonByType(typeAndIdent, sb);
		sb.append(")");
		LOGGER.debug("personAllFilesSql : {}", sb.toString());
		return sb.toString();
	}

}
