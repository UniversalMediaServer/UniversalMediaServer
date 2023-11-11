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
import net.pms.database.MediaTableCoverArtArchive;
import net.pms.database.MediaTableCoverArtArchive.CoverArtArchiveResult;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.renderers.Renderer;
import net.pms.store.DbIdMediaType;
import net.pms.store.DbIdTypeAndIdent;

/**
 * This StoreContainer implements support for RealFileDbId's database backed
 * IDs.
 */
public class VirtualFolderDbId extends LocalizedStoreContainer {

	private final DbIdTypeAndIdent typeIdent;

	public VirtualFolderDbId(Renderer renderer, String i18nName, DbIdTypeAndIdent typeIdent) {
		super(renderer, i18nName, null);
		this.typeIdent = typeIdent;
	}

	@Override
	public String getSystemName() {
		return this.typeIdent.toString();
	}

	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		//fixme : only if ident is mbid.
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

	public DbIdMediaType getMediaType() {
		return typeIdent.type;
	}

	public String getMediaTypeUclass() {
		return typeIdent.type.uclass;
	}

}
