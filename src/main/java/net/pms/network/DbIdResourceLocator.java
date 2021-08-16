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

		TYPE_AUDIO("FID$", "object.item.audioItem"),
		TYPE_FOLDER("FOLDER$", "object.container.storageFolder"),
		TYPE_ALBUM("ALBUM$", "object.container.album.musicAlbum"),
		TYPE_PERSON("PERSON$", "object.container.person.musicArtist"),
		TYPE_PLAYLIST("PLAYLIST$", "object.container.playlistContainer"),
		TYPE_VIDEO("VIDEO$", "object.item.videoItem"),
		TYPE_IMAGE("IMAGE$", "object.item.imageItem");

		public final static String GENERAL_PREFIX = "$DBID$";
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

	public String encodeDbid(String ident, DbidMediaType mediaType) {
		try {
			return String.format("%s%s%s", DbidMediaType.GENERAL_PREFIX, mediaType.dbidPrefix,
				URLEncoder.encode(ident, StandardCharsets.UTF_8.toString()));
		} catch (UnsupportedEncodingException e) {
			LOGGER.warn("encode error", e);
			return String.format("%s%s%s", DbidMediaType.GENERAL_PREFIX, mediaType.dbidPrefix, ident);
		}
	}

	/**
	 * Navigates into a DbidResource.
	 *
	 * @param typeAndIdent Resource identified by typeand database id.
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
							"select FILENAME, F.ID as FID, MODIFIED from FILES as F left outer join AUDIOTRACKS as A on F.ID = A.FILEID where (  F.TYPE = 1  and  A.ALBUM  = '%s')",
							typeAndIdent.ident);
						try (ResultSet resultSet = statement.executeQuery(sql)) {
							res = new VirtualFolderDbId(DbidMediaType.TYPE_ALBUM, typeAndIdent.ident, "");
							while (resultSet.next()) {
								DLNAResource item = new RealFileDbId(DbidMediaType.TYPE_AUDIO, new File(resultSet.getString("FILENAME")),
									resultSet.getString("FID"));
								item.resolve();
								res.addChild(item);
							}
						}
						break;
					case TYPE_PERSON:
						sql = String.format(
							"select FILENAME, F.ID as FID, MODIFIED from FILES as F left outer join AUDIOTRACKS as A on F.ID = A.FILEID where (  F.TYPE = 1  and  (A.ALBUMARTIST = '%s' or A.ARTIST = '%s'))",
							typeAndIdent.ident, typeAndIdent.ident);
						try (ResultSet resultSet = statement.executeQuery(sql)) {
							res = new VirtualFolderDbId(DbidMediaType.TYPE_ALBUM, typeAndIdent.ident, "");
							while (resultSet.next()) {
								DLNAResource item = new RealFileDbId(DbidMediaType.TYPE_AUDIO, new File(resultSet.getString("FILENAME")),
									resultSet.getString("FID"));
								item.resolve();
								res.addChild(item);
							}
						}
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
