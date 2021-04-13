package net.pms.dlna;

import java.io.File;
import net.pms.configuration.RendererConfiguration;
import net.pms.formats.Format;
import net.pms.formats.PLAYLIST;
import net.pms.network.DbIdResourceLocator;
import net.pms.network.DbIdResourceLocator.DbidMediaType;

/**
 * This RealFile implementation uses database IDs as unique identifiers and does
 * not rely on any cache.
 */
public class RealFileDbId extends RealFile {

	private DbIdResourceLocator dbid = new DbIdResourceLocator();

	public RealFileDbId(File file) {
		super(file);
	}

	public RealFileDbId(DbidMediaType mediaType, File file, String id) {
		super(file);
		setId(dbid.encodeDbid(id, mediaType));
	}

	public RealFileDbId(DbidMediaType mediaType, File file, String name, String id) {
		super(file, name);
		setId(dbid.encodeDbid(id, mediaType));
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
