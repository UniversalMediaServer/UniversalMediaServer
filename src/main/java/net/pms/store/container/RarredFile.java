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

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import com.github.junrar.volume.FileVolumeManager;
import java.io.File;
import java.io.IOException;
import java.util.List;
import net.pms.renderers.Renderer;
import net.pms.store.StoreContainer;
import net.pms.store.item.RarredEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RarredFile extends StoreContainer {
	private static final Logger LOGGER = LoggerFactory.getLogger(RarredFile.class);
	private File f;
	private Archive rarFile;

	public RarredFile(Renderer renderer, File f) {
		super(renderer, f.getName(), null);
		this.f = f;
		setLastModified(f.lastModified());

		try {
			rarFile = new Archive(new FileVolumeManager(f), null, null);
			List<FileHeader> headers = rarFile.getFileHeaders();

			for (FileHeader fh : headers) {
				// if (fh.getFullUnpackSize() < MAX_ARCHIVE_ENTRY_SIZE && fh.getFullPackSize() < MAX_ARCHIVE_ENTRY_SIZE)
				addChild(new RarredEntry(renderer, fh.getFileName(), f, fh.getFileName(), fh.getFullUnpackSize()));
			}

			rarFile.close();
		} catch (RarException | IOException e) {
			LOGGER.error(null, e);
		}
	}

	@Override
	public long length() {
		return f.length();
	}

	@Override
	public String getSystemName() {
		return f.getAbsolutePath();
	}

	@Override
	public boolean isValid() {
		boolean t = false;

		try {
			t = f.exists() && !rarFile.isEncrypted();
		} catch (RarException th) {
			LOGGER.debug("Caught exception", th);
		}

		return t;
	}
}
