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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import net.pms.renderers.Renderer;
import net.pms.store.StoreContainer;
import net.pms.store.item.SevenZipEntry;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SevenZipFolder extends StoreContainer {

	private static final Logger LOGGER = LoggerFactory.getLogger(SevenZipFolder.class);

	protected final File file;

	private final String entryName;

	public SevenZipFolder(Renderer renderer, File file, String entryName) {
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
		try (RandomAccessFile rf = new RandomAccessFile(file, "r"); IInArchive arc = SevenZip.openInArchive(null, new RandomAccessFileInStream(rf))) {
			ISimpleInArchive simpleInArchive = arc.getSimpleInterface();
			for (ISimpleInArchiveItem item : simpleInArchive.getArchiveItems()) {
				if (item.getPath().equals(entryName) && item.getCreationTime() != null) {
					setLastModified(item.getCreationTime().getTime());
				} else if (isDirectChild(item)) {
					if (item.isFolder()) {
						addChild(new SevenZipFolder(renderer, file, item.getPath()));
					} else {
						SevenZipEntry child = new SevenZipEntry(renderer, file, item.getPath(), item.getSize());
						if (child.isValid()) {
							addChild(child);
						}
					}
				}
			}
		} catch (IOException e) {
			LOGGER.error("Error reading archive file", e);
		}
	}

	private boolean isDirectChild(ISimpleInArchiveItem item) throws SevenZipException {
		if (item.getPath().startsWith(entryName)) {
			String childName = item.getPath().substring(entryName.length() + 1);
			return !childName.contains("/") && !childName.contains("\\");
		}
		return false;
	}

}
