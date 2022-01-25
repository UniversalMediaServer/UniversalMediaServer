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
package net.pms.dlna.virtual;

import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.DbIdTypeAndIdent;
import net.pms.dlna.DbIdResourceLocator;
import net.pms.dlna.DbIdMediaType;

/**
 * This VirtualFolder implements support for RealFileDbId's database backed IDs.
 */
public class VirtualFolderDbId extends VirtualFolder {

	private final DbIdResourceLocator dbIdResourceLocator = new DbIdResourceLocator();

	private final DbIdMediaType mediaType;

	public VirtualFolderDbId(String folderName, DbIdTypeAndIdent typeIdent, String thumbnailIcon) {
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

	public DbIdMediaType getMediaType() {
		return mediaType;
	}

	public String getMediaTypeUclass() {
		return mediaType.uclass;
	}
}
