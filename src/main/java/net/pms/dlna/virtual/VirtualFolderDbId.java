package net.pms.dlna.virtual;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.DbidTypeAndIdent;
import net.pms.network.DbIdResourceLocator;
import net.pms.network.DbIdResourceLocator.DbidMediaType;

/**
 * This VirtualFolder implements support for RealFileDbId's database backed IDs.
 */
public class VirtualFolderDbId extends VirtualFolder {

	private static final Logger LOG = LoggerFactory.getLogger(VirtualFolderDbId.class.getName());

	private DbIdResourceLocator dbIdResourceLocator = new DbIdResourceLocator();

	private String id2;

	private DbidMediaType mediaType;

	public VirtualFolderDbId(String folderName, DbidTypeAndIdent typeIdent, String thumbnailIcon) {
		super(folderName, thumbnailIcon);
		this.mediaType = typeIdent.type;
		String id = dbIdResourceLocator.encodeDbid(typeIdent);
		setId(id);
		setDefaultRenderer(RendererConfiguration.getDefaultConf());
	}

	@Override
	public void addChild(DLNAResource child) {
		addChild(child, false, false);
		getChildren().add(child);
		child.setParent(this);
	}

	public DbidMediaType getMediaType() {
		return mediaType;
	}

	public String getMediaTypeUclass() {
		return mediaType.uclass;
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
