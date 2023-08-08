/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.dlna;

import java.io.File;
import net.pms.configuration.RendererConfigurations;
import net.pms.formats.Format;
import net.pms.formats.PLAYLIST;
import net.pms.renderers.Renderer;

/**
 * This RealFile implementation uses database IDs as unique identifiers and does
 * not rely on any cache.
 */
public final class RealFileDbId extends RealFile {

	public RealFileDbId(Renderer renderer, File file) {
		super(renderer, file);
	}

	public RealFileDbId(Renderer renderer, DbIdTypeAndIdent typeIdent, File file) {
		super(renderer, file);
		setId(DbIdResourceLocator.encodeDbid(typeIdent));
	}

	public RealFileDbId(Renderer renderer, DbIdTypeAndIdent typeIdent, File file, String name) {
		super(renderer, file, name);
		setId(DbIdResourceLocator.encodeDbid(typeIdent));
	}

	@Override
	public void addChild(MediaResource child) {
		addChild(child, false, false);
	}

	@Override
	public MediaResource getParent() {
		MediaResource parent = super.getParent();
		if (parent == null) {
			parent = RendererConfigurations.getDefaultRenderer().getRootFolder();
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
