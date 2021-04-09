package net.pms.dlna;

import java.io.File;
import net.pms.configuration.RendererConfiguration;
import net.pms.formats.Format;
import net.pms.formats.PLAYLIST;

/**
 * This RealFile implementation uses database IDs as unique identifiers and does
 * not rely on any cache.
 */
public class RealFileDbId extends RealFile {

	public RealFileDbId(File file) {
		super(file);
	}

	public RealFileDbId(File file, String id) {
		super(file);
		setId("$DBID$" + id);
	}

	public RealFileDbId(File file, String name, String id) {
		super(file, name);
		setId(id);
	}

	@Override
	public void addChild(DLNAResource child) {
		addChild(child, false, false);
	}

	@Override
	public DLNAResource getParent() {
		DLNAResource parent = super.getParent();
		if (parent == null) {
			parent = RendererConfiguration.getDefaultConf().getRootFolder();
			setParent(parent);
		}
		return parent;
	}

	@Override
	public boolean isFolder() {
		Format f = getFormat();
		return f instanceof PLAYLIST ? true : false;
	}
}
