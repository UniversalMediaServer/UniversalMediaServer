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
import net.pms.configuration.RendererConfiguration;
import net.pms.formats.Format;
import net.pms.formats.PLAYLIST;

/**
 * This RealFile implementation uses database IDs as unique identifiers and does
 * not rely on any cache.
 */
public final class RealFileDbId extends RealFile {

	private final DbIdResourceLocator dbid = new DbIdResourceLocator();

	public RealFileDbId(File file) {
		super(file);
	}

	public RealFileDbId(DbIdTypeAndIdent typeIdent, File file) {
		super(file);
		setId(dbid.encodeDbid(typeIdent));
	}

	public RealFileDbId(DbIdTypeAndIdent typeIdent, File file, String name) {
		super(file, name);
		setId(dbid.encodeDbid(typeIdent));
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
		return f instanceof PLAYLIST;
	}
}
