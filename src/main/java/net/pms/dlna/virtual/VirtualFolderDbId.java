package net.pms.dlna.virtual;

import java.util.concurrent.atomic.AtomicLong;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAResource;

/**
 * This VirtualFolder implements support for RealFileDbId's database backed IDs.
 */
public class VirtualFolderDbId extends VirtualFolder {

	private static AtomicLong idGen = new AtomicLong(Integer.MAX_VALUE);

	public VirtualFolderDbId(String name, String thumbnailIcon) {
		this(name, thumbnailIcon, Long.toString(idGen.getAndIncrement()));
	}

	public VirtualFolderDbId(String name, String thumbnailIcon, String id) {
		super(name, thumbnailIcon);
		setDefaultRenderer(RendererConfiguration.getDefaultConf());
		setId("$DBID$" + id);
	}

	@Override
	public void addChild(DLNAResource child) {
		addChild(child, false, false);
		getChildren().add(child);
		child.setParent(this);
	}
}
