package net.pms.network;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.dlna.DLNAMediaDatabase;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.DbidTypeAndIdent;
import net.pms.dlna.PlaylistFolder;
import net.pms.dlna.RealFileDbId;
import net.pms.dlna.virtual.VirtualFolderDbId;

public class DbIdResourceLocator {

	private static final Logger LOGGER = LoggerFactory.getLogger(DbIdResourceLocator.class);

	private DLNAMediaDatabase database;

	public enum DbidMediaType {

		//@formatter:off
		TYPE_AUDIO("FID$", "object.item.audioItem"),
		TYPE_FOLDER("FOLDER$", "object.container.storageFolder"),
		TYPE_ALBUM("ALBUM$", "object.container.album.musicAlbum"),
		TYPE_PERSON("PERSON$", "object.container.person.musicArtist"),
		TYPE_PERSON_ALBUM_FILES("PERSON_ALBUM_FILES$", "object.container.storageFolder"),
		TYPE_PERSON_ALBUM("PERSON_ALBUM$", "object.container.album.musicAlbum"),
		TYPE_PERSON_ALL_FILES("PERSON_ALL_FILES$", "object.container.storageFolder"),
		TYPE_PLAYLIST("PLAYLIST$", "object.container.playlistContainer"),
		TYPE_VIDEO("VIDEO$", "object.item.videoItem"),
		TYPE_IMAGE("IMAGE$", "object.item.imageItem");
		//@formatter:on

		public final static String GENERAL_PREFIX = "$DBID$";
		public static final String SPLIT_CHARS = "___";
		public final String dbidPrefix;
		public final String uclass;

		DbidMediaType(String dbidPrefix, String uclass) {
			this.dbidPrefix = dbidPrefix;
			this.uclass = uclass;
		}

		public static DbidTypeAndIdent getTypeIdentByDbid(String id) {
			String strType = id.substring(DbidMediaType.GENERAL_PREFIX.length());
			for (DbidMediaType type : values()) {
				if (strType.startsWith(type.dbidPrefix)) {
					String ident = strType.substring(type.dbidPrefix.length());
					try {
						return new DbidTypeAndIdent(type, URLDecoder.decode(ident, StandardCharsets.UTF_8.toString()));
					} catch (UnsupportedEncodingException e) {
						LOGGER.warn("decode error", e);
						return new DbidTypeAndIdent(type, ident);
					}
				}
			}
			throw new RuntimeException("Unknown DBID type : " + id);
		}
	}

	public DbIdResourceLocator() {
		this.database = PMS.get().getDatabase();
	}

	public DLNAResource locateResource(String id) {
		DLNAResource resource = getDLNAResourceByDBID(DbidMediaType.getTypeIdentByDbid(id));
		return resource;
	}

	public String encodeDbid(DbidTypeAndIdent typeIdent) {
		try {
			return String.format("%s%s%s", DbidMediaType.GENERAL_PREFIX, typeIdent.type.dbidPrefix,
				URLEncoder.encode(typeIdent.ident, StandardCharsets.UTF_8.toString()));
		} catch (UnsupportedEncodingException e) {
			LOGGER.warn("encode error", e);
			return String.format("%s%s%s", DbidMediaType.GENERAL_PREFIX, typeIdent.type.dbidPrefix, typeIdent.ident);
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
	public DLNAResource getDLNAResourceByDBID(DbidTypeAndIdent typeAndIdent) {
		DLNAResource res = null;
		try (Connection connection = database.getConnection()) {
			try (Statement statement = connection.createStatement()) {
				String sql = null;
				switch (typeAndIdent.type) {
					case TYPE_AUDIO:
					case TYPE_VIDEO:
					case TYPE_IMAGE:
						sql = String.format("select FILENAME, TYPE from files where id = %s", typeAndIdent.ident);
						try (ResultSet resultSet = statement.executeQuery(sql)) {
							if (resultSet.next()) {
								res = new RealFileDbId(new File(resultSet.getString("FILENAME")));
								res.resolve();
							}
						}
						break;

					case TYPE_PLAYLIST:
						sql = String.format("select FILENAME, TYPE from files where id = %s", typeAndIdent.ident);
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
								"where (  F.TYPE = 1  and  A.ALBUM  = '%s')",
							typeAndIdent.ident);
						try (ResultSet resultSet = statement.executeQuery(sql)) {
							res = new VirtualFolderDbId(typeAndIdent.ident,
								new DbidTypeAndIdent(DbidMediaType.TYPE_ALBUM, typeAndIdent.ident), "");
							while (resultSet.next()) {
								DLNAResource item = new RealFileDbId(
									new DbidTypeAndIdent(DbidMediaType.TYPE_AUDIO, resultSet.getString("FID")),
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
						try (ResultSet resultSet = statement.executeQuery(sql)) {
							res = new VirtualFolderDbId(typeAndIdent.ident,
								new DbidTypeAndIdent(DbidMediaType.TYPE_ALBUM, typeAndIdent.ident), "");
							while (resultSet.next()) {
								DLNAResource item = new RealFileDbId(
									new DbidTypeAndIdent(DbidMediaType.TYPE_AUDIO, resultSet.getString("FID")),
									new File(resultSet.getString("FILENAME")));
								item.resolve();
								res.addChild(item);
							}
						}
						res.setFakeParentId(encodeDbid(new DbidTypeAndIdent(DbidMediaType.TYPE_PERSON, typeAndIdent.ident)));
						break;

					case TYPE_PERSON:
						res = new VirtualFolderDbId(typeAndIdent.ident, new DbidTypeAndIdent(DbidMediaType.TYPE_PERSON, typeAndIdent.ident),
							"");
						DLNAResource allFiles = new VirtualFolderDbId(Messages.getString("Search.AllFiles"),
							new DbidTypeAndIdent(DbidMediaType.TYPE_PERSON_ALL_FILES, typeAndIdent.ident), "");
						res.addChild(allFiles);
						DLNAResource albums = new VirtualFolderDbId(Messages.getString("Search.ByAlbum"),
							new DbidTypeAndIdent(DbidMediaType.TYPE_PERSON_ALBUM, typeAndIdent.ident), "");
						res.addChild(albums);
						break;

					case TYPE_PERSON_ALBUM:
						sql = String.format("SELECT DISTINCT(album) FROM AUDIOTRACKS A where COALESCE(A.ALBUMARTIST, A.ARTIST) = '%s'",
							typeAndIdent.ident);
						res = new VirtualFolderDbId(typeAndIdent.ident, new DbidTypeAndIdent(DbidMediaType.TYPE_ALBUM, typeAndIdent.ident),
							"");
						try (ResultSet resultSet = statement.executeQuery(sql)) {
							while (resultSet.next()) {
								String album = resultSet.getString(1);
								res.addChild(new VirtualFolderDbId(album, new DbidTypeAndIdent(DbidMediaType.TYPE_PERSON_ALBUM_FILES,
									typeAndIdent.ident + DbidMediaType.SPLIT_CHARS + album), ""));
							}
						}
						res.setFakeParentId(encodeDbid(new DbidTypeAndIdent(DbidMediaType.TYPE_PERSON, typeAndIdent.ident)));
						break;

					case TYPE_PERSON_ALBUM_FILES:
						String[] identSplitted = typeAndIdent.ident.split(DbidMediaType.SPLIT_CHARS);
						sql = String.format(
							"select FILENAME, F.ID as FID, MODIFIED from FILES as F left outer join AUDIOTRACKS as A on F.ID = A.FILEID " +
								"where (A.ALBUM = '%s') and (A.ALBUMARTIST = '%s' or A.ARTIST = '%s')",
							identSplitted[1], identSplitted[0], identSplitted[0]);
						try (ResultSet resultSet = statement.executeQuery(sql)) {
							res = new VirtualFolderDbId(identSplitted[1],
								new DbidTypeAndIdent(DbidMediaType.TYPE_ALBUM, typeAndIdent.ident), "");
							while (resultSet.next()) {
								DLNAResource item = new RealFileDbId(
									new DbidTypeAndIdent(DbidMediaType.TYPE_AUDIO, resultSet.getString("FID")),
									new File(resultSet.getString("FILENAME")));
								item.resolve();
								res.addChild(item);
							}
						}
						res.setFakeParentId(encodeDbid(new DbidTypeAndIdent(DbidMediaType.TYPE_PERSON_ALBUM, identSplitted[0])));
						break;
					default:
						throw new RuntimeException("Unknown Type");
				}
			}
		} catch (SQLException e) {
			LOGGER.trace("", e);
		}
		return res;
	}
}
