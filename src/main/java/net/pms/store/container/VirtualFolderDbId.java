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
package net.pms.store.container;

import java.io.IOException;
import java.util.Objects;
import net.pms.database.MediaTableCoverArtArchive;
import net.pms.database.MediaTableCoverArtArchive.CoverArtArchiveResult;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.renderers.Renderer;
import net.pms.store.DbIdMediaType;
import net.pms.store.DbIdResourceLocator;
import net.pms.store.DbIdTypeAndIdent;
import net.pms.store.StoreContainer;
import net.pms.store.StoreResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This StoreContainer implements support for RealFileDbId's database backed
 * IDs.
 */
public class VirtualFolderDbId extends StoreContainer {

	private static final Logger LOG = LoggerFactory.getLogger(VirtualFolderDbId.class.getName());

	private final DbIdTypeAndIdent typeIdent;

	public VirtualFolderDbId(Renderer renderer, String folderName, DbIdTypeAndIdent typeIdent, String thumbnailIcon) {
		super(renderer, folderName, thumbnailIcon);
		this.typeIdent = typeIdent;
		String id = DbIdResourceLocator.encodeDbid(typeIdent);
		setId(id);
	}

	@Override
	public String getSystemName() {
		return this.typeIdent.ident;
	}

	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		CoverArtArchiveResult res = MediaTableCoverArtArchive.findMBID(typeIdent.ident);
		if (!res.isFound()) {
			try {
				return super.getThumbnailInputStream();
			} catch (IOException e) {
				return null;
			}
		}
		return DLNAThumbnailInputStream.toThumbnailInputStream(res.getCoverBytes());
	}

	@Override
	public void addChild(StoreResource child) {
		addChild(child, false, false);
		getChildren().add(child);
		child.setParent(this);
	}

	public DbIdMediaType getMediaType() {
		return typeIdent.type;
	}

	public String getMediaTypeUclass() {
		return typeIdent.type.uclass;
	}

	@Override
	public final void setId(String id) {
		if (id != null && id.startsWith(DbIdMediaType.GENERAL_PREFIX)) {
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