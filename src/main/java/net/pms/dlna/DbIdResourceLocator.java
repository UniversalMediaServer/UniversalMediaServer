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
package net.pms.dlna;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableAudiotracks;
import net.pms.database.MediaTableFiles;
import net.pms.database.MediaTableMusicBrainzReleaseLike;
import net.pms.dlna.api.DoubleRecordFilter;
import net.pms.dlna.api.MusicBrainzAlbum;
import net.pms.dlna.virtual.VirtualFolderDbId;
import net.pms.renderers.Renderer;

/**
 * This class resolves DLNA objects identified by databaseID's.
 */
public class DbIdResourceLocator {
	private static final Logger LOGGER = LoggerFactory.getLogger(DbIdResourceLocator.class);

	/**
	 * This class is not meant to be instantiated.
	 */
	private DbIdResourceLocator() {
	}

	public static DLNAResource locateResource(String id, Renderer renderer) {
		DLNAResource resource = getDLNAResourceByDBID(DbIdMediaType.getTypeIdentByDbid(id), renderer);
		return resource;
	}

	public static String encodeDbid(DbIdTypeAndIdent typeIdent) {
		try {
			return String.format("%s%s%s", DbIdMediaType.GENERAL_PREFIX, typeIdent.type.dbidPrefix,
				URLEncoder.encode(typeIdent.ident, StandardCharsets.UTF_8.toString()));
		} catch (UnsupportedEncodingException e) {
			LOGGER.warn("encode error", e);
			return String.format("%s%s%s", DbIdMediaType.GENERAL_PREFIX, typeIdent.type.dbidPrefix, typeIdent.ident);
		}
	}

	/**
	 * Navigates into a DbidResource.
	 *
	 * @param typeAndIdent Resource identified by type and database id.
	 * @return In case typeAndIdent is an item, the RealFile resource is located
	 *         and resolved. In case of a container, the container will be
	 *         created and populated.
	 */
	public static DLNAResource getDLNAResourceByDBID(DbIdTypeAndIdent typeAndIdent, Renderer renderer) {
		DLNAResource res = null;
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				try (Statement statement = connection.createStatement()) {
					String sql;
					switch (typeAndIdent.type) {
						case TYPE_AUDIO, TYPE_VIDEO, TYPE_IMAGE -> {
							sql = String.format("SELECT " + MediaTableFiles.TABLE_COL_FILENAME + " FROM " + MediaTableFiles.TABLE_NAME + " WHERE " + MediaTableFiles.TABLE_COL_ID + " = %s", typeAndIdent.ident);
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace(String.format("SQL AUDIO/VIDEO/IMAGE : %s", sql));
							}
							try (ResultSet resultSet = statement.executeQuery(sql)) {
								if (resultSet.next()) {
									res = new RealFileDbId(new File(resultSet.getString("FILENAME")));
									res.setDefaultRenderer(renderer);
									res.resolve();
								}
							}
						}
						case TYPE_PLAYLIST -> {
							sql = String.format("SELECT " + MediaTableFiles.TABLE_COL_FILENAME + " FROM " + MediaTableFiles.TABLE_NAME + " WHERE " + MediaTableFiles.TABLE_COL_ID + " = %s", typeAndIdent.ident);
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace(String.format("SQL PLAYLIST : %s", sql));
							}
							try (ResultSet resultSet = statement.executeQuery(sql)) {
								if (resultSet.next()) {
									res = new PlaylistFolder(new File(resultSet.getString("FILENAME")));
									res.setDefaultRenderer(renderer);
									res.setId(String.format("$DBID$PLAYLIST$%s", typeAndIdent.ident));
									res.resolve();
									res.refreshChildren();
								}
							}
						}
						case TYPE_ALBUM -> {
							sql = String.format(
									"SELECT " + MediaTableFiles.TABLE_COL_FILENAME + ", " + MediaTableFiles.TABLE_COL_ID + ", " + MediaTableFiles.TABLE_COL_MODIFIED + " FROM " + MediaTableFiles.TABLE_NAME + " LEFT OUTER JOIN " + MediaTableAudiotracks.TABLE_NAME + " ON " + MediaTableFiles.TABLE_COL_ID + " = " + MediaTableAudiotracks.TABLE_COL_FILEID + " " +
											"WHERE ( " + MediaTableFiles.TABLE_COL_FORMAT_TYPE + " = 1  AND  " + MediaTableAudiotracks.TABLE_COL_ALBUM + " = '%s')",
									typeAndIdent.ident);
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace(String.format("SQL AUDIO-ALBUM : %s", sql));
							}
							try (ResultSet resultSet = statement.executeQuery(sql)) {
								res = new VirtualFolderDbId(typeAndIdent.ident,
									new DbIdTypeAndIdent(DbIdMediaType.TYPE_ALBUM, typeAndIdent.ident), "");
								res.setDefaultRenderer(renderer);
								while (resultSet.next()) {
									DLNAResource item = new RealFileDbId(
											new DbIdTypeAndIdent(DbIdMediaType.TYPE_AUDIO, resultSet.getString("ID")),
											new File(resultSet.getString("FILENAME")));
									item.resolve();
									res.addChild(item);
								}
							}
						}
						case TYPE_MUSICBRAINZ_RECORDID -> {
							sql = String.format(
									"SELECT " + MediaTableFiles.TABLE_COL_FILENAME + ", " + MediaTableAudiotracks.TABLE_COL_MBID_TRACK + ", " + MediaTableFiles.TABLE_COL_ID + ", " + MediaTableAudiotracks.TABLE_COL_ALBUM + " FROM " + MediaTableFiles.TABLE_NAME + " LEFT OUTER JOIN " + MediaTableAudiotracks.TABLE_NAME + " ON " + MediaTableFiles.TABLE_COL_ID + " = " + MediaTableAudiotracks.TABLE_COL_FILEID + " " +
											"WHERE ( " + MediaTableFiles.TABLE_COL_FORMAT_TYPE + " = 1 and " + MediaTableAudiotracks.TABLE_COL_MBID_RECORD + " = '%s' ) ORDER BY " + MediaTableAudiotracks.TABLE_COL_MBID_TRACK,
									typeAndIdent.ident);
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace(String.format("SQL TYPE_MUSICBRAINZ_RECORDID : %s", sql));
							}
							try (ResultSet resultSet = statement.executeQuery(sql)) {
								if (resultSet.next()) {
									res = new VirtualFolderDbId(resultSet.getString("ALBUM"),
										new DbIdTypeAndIdent(DbIdMediaType.TYPE_MUSICBRAINZ_RECORDID, typeAndIdent.ident), "");
									res.setDefaultRenderer(renderer);
									res.setFakeParentId(encodeDbid(new DbIdTypeAndIdent(DbIdMediaType.TYPE_MYMUSIC_ALBUM, Messages.getString("MyAlbums"))));
									// Find "best track" logic should be optimized !!
									String lastUuidTrack = "";
									do {
										String currentUuidTrack = resultSet.getString("MBID_TRACK");
										if (!currentUuidTrack.equals(lastUuidTrack)) {
											lastUuidTrack = currentUuidTrack;
											DLNAResource item = new RealFileDbId(
													new DbIdTypeAndIdent(DbIdMediaType.TYPE_AUDIO, resultSet.getString("ID")),
													new File(resultSet.getString("FILENAME")));
											item.resolve();
											res.addChild(item);
										}
									} while (resultSet.next());
								}
							}
						}
						case TYPE_MYMUSIC_ALBUM -> {
							sql = "SELECT " + MediaTableMusicBrainzReleaseLike.TABLE_COL_MBID_RELEASE + ", " + MediaTableAudiotracks.TABLE_COL_ALBUM + ", " + MediaTableAudiotracks.TABLE_COL_ARTIST + ", " + MediaTableAudiotracks.TABLE_COL_MEDIA_YEAR + " FROM " + MediaTableMusicBrainzReleaseLike.TABLE_NAME + " JOIN " + MediaTableAudiotracks.TABLE_NAME + " ON " + MediaTableMusicBrainzReleaseLike.TABLE_COL_MBID_RELEASE + " = " + MediaTableAudiotracks.TABLE_COL_MBID_RECORD + ";";
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace(String.format("SQL TYPE_MYMUSIC_ALBUM : %s", sql));
							}
							DoubleRecordFilter filter = new DoubleRecordFilter();
							res = new VirtualFolderDbId(
								Messages.getString("MyAlbums"),
								new DbIdTypeAndIdent(DbIdMediaType.TYPE_MYMUSIC_ALBUM, Messages.getString("MyAlbums")),
								"");
							res.setDefaultRenderer(renderer);
							if (PMS.getConfiguration().displayAudioLikesInRootFolder()) {
								res.setFakeParentId("0");
							} else if (PMS.get().getLibrary().isEnabled()) {
								res.setFakeParentId(PMS.get().getLibrary().getAudioFolder().getId());
							} else {
								LOGGER.debug("couldn't add 'My Music' folder because the media library is not initialized.");
								return null;
							}
							try (ResultSet resultSet = statement.executeQuery(sql)) {
								while (resultSet.next()) {
									filter.addAlbum(new MusicBrainzAlbum(
											resultSet.getString("MBID_RELEASE"),
											resultSet.getString("ALBUM"),
											resultSet.getString("ARTIST"),
											resultSet.getInt("MEDIA_YEAR")));
								}
								for (MusicBrainzAlbum album : filter.getUniqueAlbumSet()) {
									VirtualFolderDbId albumFolder = new VirtualFolderDbId(album.getAlbum(), new DbIdTypeAndIdent(
											DbIdMediaType.TYPE_MUSICBRAINZ_RECORDID,
											album.getMbReleaseid()), "");
									appendAlbumInformation(album, albumFolder);
									res.addChild(albumFolder);
								}
							}
						}
						case TYPE_PERSON_ALL_FILES -> {
							sql = String.format(
									"SELECT " + MediaTableFiles.TABLE_COL_FILENAME + ", " + MediaTableFiles.TABLE_COL_ID + ", " + MediaTableFiles.TABLE_COL_MODIFIED + " FROM " + MediaTableFiles.TABLE_NAME + " LEFT OUTER JOIN " + MediaTableAudiotracks.TABLE_NAME + " ON " + MediaTableFiles.TABLE_COL_ID + " = " + MediaTableAudiotracks.TABLE_COL_FILEID + " " +
											"WHERE (" + MediaTableAudiotracks.TABLE_COL_ALBUMARTIST + " = '%s' OR " + MediaTableAudiotracks.TABLE_COL_ARTIST + " = '%s')",
									typeAndIdent.ident, typeAndIdent.ident);
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace(String.format("SQL PERSON : %s", sql));
							}
							try (ResultSet resultSet = statement.executeQuery(sql)) {
								res = new VirtualFolderDbId(typeAndIdent.ident,
									new DbIdTypeAndIdent(DbIdMediaType.TYPE_ALBUM, typeAndIdent.ident), "");
								res.setDefaultRenderer(renderer);
								while (resultSet.next()) {
									DLNAResource item = new RealFileDbId(
											new DbIdTypeAndIdent(DbIdMediaType.TYPE_AUDIO, resultSet.getString("ID")),
											new File(resultSet.getString("FILENAME")));
									item.resolve();
									res.addChild(item);
								}
							}
							res.setFakeParentId(encodeDbid(new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON, typeAndIdent.ident)));
						}
						case TYPE_PERSON -> {
							res = new VirtualFolderDbId(typeAndIdent.ident, new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON, typeAndIdent.ident),
								"");
							res.setDefaultRenderer(renderer);
							DLNAResource allFiles = new VirtualFolderDbId(Messages.getString("AllFiles"),
									new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON_ALL_FILES, typeAndIdent.ident), "");
							res.addChild(allFiles);
							DLNAResource albums = new VirtualFolderDbId(Messages.getString("ByAlbum_lowercase"),
									new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON_ALBUM, typeAndIdent.ident), "");
							res.addChild(albums);
						}
						case TYPE_PERSON_ALBUM -> {
							sql = String.format("SELECT DISTINCT(" + MediaTableAudiotracks.TABLE_COL_ALBUM + ") FROM " + MediaTableAudiotracks.TABLE_NAME + " WHERE COALESCE(" + MediaTableAudiotracks.TABLE_COL_ALBUMARTIST + ", " + MediaTableAudiotracks.TABLE_COL_ARTIST + ") = '%s'",
									typeAndIdent.ident);
							res = new VirtualFolderDbId(
									typeAndIdent.ident,
									new DbIdTypeAndIdent(DbIdMediaType.TYPE_ALBUM, typeAndIdent.ident),
									""
							);
							res.setDefaultRenderer(renderer);
							try (ResultSet resultSet = statement.executeQuery(sql)) {
								while (resultSet.next()) {
									String album = resultSet.getString(1);
									res.addChild(new VirtualFolderDbId(album, new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON_ALBUM_FILES,
											typeAndIdent.ident + DbIdMediaType.SPLIT_CHARS + album), ""));
								}
							}
							res.setFakeParentId(encodeDbid(new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON, typeAndIdent.ident)));
						}
						case TYPE_PERSON_ALBUM_FILES -> {
							String[] identSplitted = typeAndIdent.ident.split(DbIdMediaType.SPLIT_CHARS);
							sql = String.format(
									"SELECT " + MediaTableFiles.TABLE_COL_FILENAME + ", " + MediaTableFiles.TABLE_COL_ID + ", " + MediaTableFiles.TABLE_COL_MODIFIED + " FROM " + MediaTableFiles.TABLE_NAME + " LEFT OUTER JOIN " + MediaTableAudiotracks.TABLE_NAME + " ON " + MediaTableFiles.TABLE_COL_ID + " = " + MediaTableAudiotracks.TABLE_COL_FILEID + " " +
											"WHERE (" + MediaTableAudiotracks.TABLE_COL_ALBUM + " = '%s') AND (" + MediaTableAudiotracks.TABLE_COL_ALBUMARTIST + " = '%s' OR " + MediaTableAudiotracks.TABLE_COL_ARTIST + " = '%s')",
									identSplitted[1], identSplitted[0], identSplitted[0]);
							try (ResultSet resultSet = statement.executeQuery(sql)) {
								res = new VirtualFolderDbId(identSplitted[1],
									new DbIdTypeAndIdent(DbIdMediaType.TYPE_ALBUM, typeAndIdent.ident), "");
								res.setDefaultRenderer(renderer);
								while (resultSet.next()) {
									DLNAResource item = new RealFileDbId(
											new DbIdTypeAndIdent(DbIdMediaType.TYPE_AUDIO, resultSet.getString("ID")),
											new File(resultSet.getString("FILENAME")));
									item.resolve();
									res.addChild(item);
								}
							}
							res.setFakeParentId(encodeDbid(new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON_ALBUM, identSplitted[0])));
						}
						default -> throw new RuntimeException("Unknown Type");
					}
				}
			} else {
				LOGGER.error("database not available !");
			}
		} catch (SQLException e) {
			LOGGER.warn("getDLNAResourceByDBID", e);
		} finally {
			MediaDatabase.close(connection);
		}
		return res;
	}

	/**
	 * Adds album information
	 * @param album
	 * @param albumFolder
	 */
	public static void appendAlbumInformation(MusicBrainzAlbum album, VirtualFolderDbId albumFolder) {
		DLNAMediaAudio audioInf =  new DLNAMediaAudio();
		audioInf.setAlbum(album.getAlbum());
		audioInf.setArtist(album.getArtist());
		audioInf.setYear(album.getYear());
		List<DLNAMediaAudio> audios = new ArrayList<>();
		audios.add(audioInf);
		DLNAMediaInfo mi = new DLNAMediaInfo();
		mi.setAudioTracks(audios);
		albumFolder.setMedia(mi);
	}
}
