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
package net.pms.dlna;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.Messages;
import net.pms.database.MediaDatabase;
import net.pms.dlna.virtual.VirtualFolderDbId;

public class DbIdResourceLocator {

	private static final Logger LOGGER = LoggerFactory.getLogger(DbIdResourceLocator.class);


	public DbIdResourceLocator() {
	}

	public DLNAResource locateResource(String id) {
		DLNAResource resource = getDLNAResourceByDBID(DbIdMediaType.getTypeIdentByDbid(id));
		return resource;
	}

	public String encodeDbid(DbIdTypeAndIdent typeIdent) {
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
	 *         created an populated.
	 */
	public DLNAResource getDLNAResourceByDBID(DbIdTypeAndIdent typeAndIdent) {
		DLNAResource res = null;
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				try (Statement statement = connection.createStatement()) {
					String sql;
					switch (typeAndIdent.type) {
						case TYPE_AUDIO:
						case TYPE_VIDEO:
						case TYPE_IMAGE:
							sql = String.format("select FILENAME, TYPE from files where id = %s", typeAndIdent.ident);
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace(String.format("SQL AUDIO/VIDEO/IMAGE : %s", sql));
							}
							try (ResultSet resultSet = statement.executeQuery(sql)) {
								if (resultSet.next()) {
									res = new RealFileDbId(new File(resultSet.getString("FILENAME")));
									res.resolve();
								}
							}
							break;

						case TYPE_PLAYLIST:
							sql = String.format("select FILENAME, TYPE from files where id = %s", typeAndIdent.ident);
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace(String.format("SQL PLAYLIST : %s", sql));
							}
							try (ResultSet resultSet = statement.executeQuery(sql)) {
								if (resultSet.next()) {
									res = new PlaylistFolder(new File(resultSet.getString("FILENAME")));
									res.resolve();
									res.refreshChildren();
								}
							}
							break;

						case TYPE_ALBUM:
							sql = String.format(
								"select FILENAME, F.ID as FID, MODIFIED from FILES as F left outer join AUDIOTRACKS as A on F.ID = A.FILEID " +
									"where (  F.FORMAT_TYPE = 1  and  A.ALBUM  = '%s')",
								typeAndIdent.ident);
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace(String.format("SQL AUDIO-ALBUM : %s", sql));
							}
							try (ResultSet resultSet = statement.executeQuery(sql)) {
								res = new VirtualFolderDbId(typeAndIdent.ident,
									new DbIdTypeAndIdent(DbIdMediaType.TYPE_ALBUM, typeAndIdent.ident), "");
								while (resultSet.next()) {
									DLNAResource item = new RealFileDbId(
										new DbIdTypeAndIdent(DbIdMediaType.TYPE_AUDIO, resultSet.getString("FID")),
										new File(resultSet.getString("FILENAME")));
									item.resolve();
									res.addChild(item);
								}
							}
							break;

						case TYPE_PERSON_ALL_FILES:
							sql = String.format(
								"select FILENAME, F.ID as FID, MODIFIED from FILES as F left outer join AUDIOTRACKS as A on F.ID = A.FILEID " +
									"where (A.ALBUMARTIST = '%s' or A.ARTIST = '%s')",
								typeAndIdent.ident, typeAndIdent.ident);
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace(String.format("SQL PERSON : %s", sql));
							}
							try (ResultSet resultSet = statement.executeQuery(sql)) {
								res = new VirtualFolderDbId(typeAndIdent.ident,
									new DbIdTypeAndIdent(DbIdMediaType.TYPE_ALBUM, typeAndIdent.ident), "");
								while (resultSet.next()) {
									DLNAResource item = new RealFileDbId(
										new DbIdTypeAndIdent(DbIdMediaType.TYPE_AUDIO, resultSet.getString("FID")),
										new File(resultSet.getString("FILENAME")));
									item.resolve();
									res.addChild(item);
								}
							}
							res.setFakeParentId(encodeDbid(new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON, typeAndIdent.ident)));
							break;

						case TYPE_PERSON:
							res = new VirtualFolderDbId(typeAndIdent.ident, new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON, typeAndIdent.ident),
								"");
							DLNAResource allFiles = new VirtualFolderDbId(Messages.getString("Search.AllFiles"),
								new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON_ALL_FILES, typeAndIdent.ident), "");
							res.addChild(allFiles);
							DLNAResource albums = new VirtualFolderDbId(Messages.getString("Search.ByAlbum"),
								new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON_ALBUM, typeAndIdent.ident), "");
							res.addChild(albums);
							break;

						case TYPE_PERSON_ALBUM:
							sql = String.format("SELECT DISTINCT(album) FROM AUDIOTRACKS A where COALESCE(A.ALBUMARTIST, A.ARTIST) = '%s'",
								typeAndIdent.ident);
							res = new VirtualFolderDbId(
								typeAndIdent.ident,
								new DbIdTypeAndIdent(DbIdMediaType.TYPE_ALBUM, typeAndIdent.ident),
								""
							);
							try (ResultSet resultSet = statement.executeQuery(sql)) {
								while (resultSet.next()) {
									String album = resultSet.getString(1);
									res.addChild(new VirtualFolderDbId(album, new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON_ALBUM_FILES,
										typeAndIdent.ident + DbIdMediaType.SPLIT_CHARS + album), ""));
								}
							}
							res.setFakeParentId(encodeDbid(new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON, typeAndIdent.ident)));
							break;

						case TYPE_PERSON_ALBUM_FILES:
							String[] identSplitted = typeAndIdent.ident.split(DbIdMediaType.SPLIT_CHARS);
							sql = String.format(
								"select FILENAME, F.ID as FID, MODIFIED from FILES as F left outer join AUDIOTRACKS as A on F.ID = A.FILEID " +
									"where (A.ALBUM = '%s') and (A.ALBUMARTIST = '%s' or A.ARTIST = '%s')",
								identSplitted[1], identSplitted[0], identSplitted[0]);
							try (ResultSet resultSet = statement.executeQuery(sql)) {
								res = new VirtualFolderDbId(identSplitted[1],
									new DbIdTypeAndIdent(DbIdMediaType.TYPE_ALBUM, typeAndIdent.ident), "");
								while (resultSet.next()) {
									DLNAResource item = new RealFileDbId(
										new DbIdTypeAndIdent(DbIdMediaType.TYPE_AUDIO, resultSet.getString("FID")),
										new File(resultSet.getString("FILENAME")));
									item.resolve();
									res.addChild(item);
								}
							}
							res.setFakeParentId(encodeDbid(new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON_ALBUM, identSplitted[0])));
							break;
						default:
							throw new RuntimeException("Unknown Type");
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.trace("", e);
		} finally {
			MediaDatabase.close(connection);
		}
		return res;
	}
}
