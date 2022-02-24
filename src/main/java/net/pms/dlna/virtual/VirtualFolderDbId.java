package net.pms.dlna.virtual;

import java.io.IOException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.configuration.RendererConfiguration;
import net.pms.database.MediaTableCoverArtArchive;
import net.pms.database.MediaTableCoverArtArchive.CoverArtArchiveResult;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.dlna.DbidTypeAndIdent;
import net.pms.network.DbIdResourceLocator;
import net.pms.network.DbIdResourceLocator.DbidMediaType;

/**
 * This VirtualFolder implements support for RealFileDbId's database backed IDs.
 */
public class VirtualFolderDbId extends VirtualFolder {

	private static final Logger LOG = LoggerFactory.getLogger(VirtualFolderDbId.class.getName());

	private DbIdResourceLocator dbIdResourceLocator = new DbIdResourceLocator();

	private final DbidTypeAndIdent typeIdent;

	public VirtualFolderDbId(String folderName, DbidTypeAndIdent typeIdent, String thumbnailIcon) {
		super(folderName, thumbnailIcon);
		this.typeIdent = typeIdent;
		String id = dbIdResourceLocator.encodeDbid(typeIdent);
		setId(id);
		setDefaultRenderer(RendererConfiguration.getDefaultConf());
	}

	/**
	@Override
	protected String getThumbnailURL(DLNAImageProfile profile) {
		StringBuilder sb = new StringBuilder(MediaServer.getURL());
		sb.append("/get/").append(getResourceId()).append("/thumbnail0000");
		if (profile != null) {
			if (DLNAImageProfile.JPEG_RES_H_V.equals(profile)) {
				sb.append("JPEG_RES").append(profile.getH()).append("x");
				sb.append(profile.getV()).append("_");
			} else {
				sb.append(profile).append("_");
			}
		}
		sb.append(encode(typeIdent.ident)).append(".");
		if (profile != null) {
			sb.append(profile.getDefaultExtension());
		} else {
			LOG.debug("Warning: Thumbnail without DLNA image profile requested, resulting URL is: \"{}\"", sb.toString());
		}
		return sb.toString();
	}
*/
	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		CoverArtArchiveResult res = MediaTableCoverArtArchive.findMBID(typeIdent.ident);
		if (!res.found) {
			try {
				return super.getThumbnailInputStream();
			} catch (IOException e) {
				return null;
			}
		}
		return DLNAThumbnailInputStream.toThumbnailInputStream(res.cover);
	}

	@Override
	public void addChild(DLNAResource child) {
		addChild(child, false, false);
		getChildren().add(child);
		child.setParent(this);
	}

	public DbidMediaType getMediaType() {
		return typeIdent.type;
	}

	public String getMediaTypeUclass() {
		return typeIdent.type.uclass;
	}

	@Override
	protected void setId(String id) {
		if (id.startsWith(DbidMediaType.GENERAL_PREFIX)) {
			super.setId(id);
		} else {
			LOG.trace("Attention. ID doesn't match DBID general prefix : " + id != null ? id : "NULL");
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		VirtualFolderDbId other = (VirtualFolderDbId) obj;
		return Objects.equals(getId(), other.getId());
	}
}
