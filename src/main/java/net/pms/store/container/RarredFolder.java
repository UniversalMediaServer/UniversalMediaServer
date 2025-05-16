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

public class RarredFolder extends StoreContainer {

	private static final Logger LOGGER = LoggerFactory.getLogger(RarredFolder.class);

	protected final File file;

	private final String entryName;

	public RarredFolder(Renderer renderer, File file, String entryName) {
		super(renderer, null, null);
		this.file = file;
		if (entryName == null || "".equals(entryName)) {
			this.entryName = "";
		} else {
			this.entryName = entryName;
			this.name = entryName;
			if (this.name.endsWith("/")) {
				this.name = this.name.substring(0, this.name.length() - 1);
			}
			if (this.name.contains("/")) {
				this.name = this.name.substring(this.name.lastIndexOf("/"));
			}
		}
	}

	@Override
	public String getSystemName() {
		return file.getAbsolutePath() + "#" + entryName;
	}

	@Override
	public boolean isValid() {
		return file.exists();
	}

	@Override
	public void discoverChildren() {
		getChildren().clear();
		try (Archive rarFile = new Archive(new FileVolumeManager(file), null, null)) {
			List<FileHeader> headers = rarFile.getFileHeaders();
			for (FileHeader fh : headers) {
				if (fh.getFileName().equals(entryName) && fh.getCreationTime() != null) {
					setLastModified(fh.getCreationTime().toMillis());
				} else if (isDirectChild(fh)) {
					if (fh.isDirectory()) {
						addChild(new SevenZipFolder(renderer, file, fh.getFileName()));
					} else {
						RarredEntry child = new RarredEntry(renderer, file, fh.getFileName(), fh.getFullUnpackSize());
						if (child.isValid()) {
							addChild(child);
						}
					}
				}
			}
		} catch (RarException | IOException e) {
			LOGGER.error("Error reading archive file", e);
		}
	}

	private boolean isDirectChild(FileHeader fh) {
		if (fh.getFileName().startsWith(entryName)) {
			String childName = fh.getFileName().substring(entryName.length() + 1);
			return !childName.contains("/") && !childName.contains("\\");
		}
		return false;
	}

}
