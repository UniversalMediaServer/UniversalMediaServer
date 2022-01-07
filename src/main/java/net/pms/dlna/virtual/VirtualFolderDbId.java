package net.pms.dlna.virtual;

import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.DbidTypeAndIdent;
import net.pms.network.DbIdResourceLocator;
import net.pms.network.DbIdResourceLocator.DbidMediaType;

/**
 * This VirtualFolder implements support for RealFileDbId's database backed IDs.
 */
public class VirtualFolderDbId extends VirtualFolder {

	private DbIdResourceLocator dbIdResourceLocator = new DbIdResourceLocator();

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
}
