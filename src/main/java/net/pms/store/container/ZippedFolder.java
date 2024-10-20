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
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import net.pms.renderers.Renderer;
import net.pms.store.StoreContainer;
import net.pms.store.item.ZippedEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZippedFolder extends StoreContainer {

	private static final Logger LOGGER = LoggerFactory.getLogger(ZippedFolder.class);

	private final String entryName;
	protected final File file;

	public ZippedFolder(Renderer renderer, File file, String entryName) {
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
		try (ZipFile zip = new ZipFile(file)) {
			Enumeration<? extends ZipEntry> enm = zip.entries();
			while (enm.hasMoreElements()) {
				ZipEntry entry = enm.nextElement();
				if (entry.getName().equals(entryName)) {
					setLastModified(entry.getTime());
				} else if (isDirectChild(entry)) {
					if (entry.isDirectory()) {
						addChild(new ZippedFolder(renderer, file, entry.getName()));
					} else {
						ZippedEntry child = new ZippedEntry(renderer, file, entry.getName(), entry.getSize());
						if (child.isValid()) {
							addChild(child);
						}
					}
				}
			}
		} catch (ZipException e) {
			LOGGER.error("Error reading zip file", e);
		} catch (IOException e) {
			LOGGER.error("Error reading zip file", e);
		}
	}

	private boolean isDirectChild(ZipEntry entry) {
		if (entry.getName().startsWith(entryName)) {
			String childName = entry.getName().substring(entryName.length());
			if (entry.isDirectory()) {
				childName = childName.substring(0, childName.length() - 1);
			}
			return !childName.contains("/");
		}
		return false;
	}

}
