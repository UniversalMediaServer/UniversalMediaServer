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

import com.sun.jna.Platform;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.platform.PlatformUtils;
import net.pms.renderers.Renderer;
import net.pms.store.SystemFileResource;
import net.pms.store.SystemFilesHelper;
import net.pms.util.ProcessUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealFolder extends VirtualFolder implements SystemFileResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(RealFolder.class);
	private final File directory;

	public RealFolder(Renderer renderer, File directory) {
		this(renderer, directory, null);
	}

	public RealFolder(Renderer renderer, File directory, String name) {
		super(renderer);
		this.directory = directory;
		setName(name);
		addFile(directory);
		setLastModified(directory.lastModified());
		setSortable(true);
	}

	@Override
	public boolean isValid() {
		boolean valid = directory.exists() && directory.isDirectory();
		if (valid && getParent() != null && getParent().getDefaultRenderer() != null && getParent().getDefaultRenderer().isUseMediaInfo()) {
			// we need to resolve the store resource now
			run();
		}
		return valid;
	}

	@Override
	public boolean isRendererAllowed() {
		return renderer.hasShareAccess(directory);
	}

	@Override
	public String getSystemName() {
		return ProcessUtil.getShortFileNameIfWideChars(directory.getAbsolutePath());
	}

	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		File cachedThumbnail = SystemFilesHelper.getFolderThumbnail(directory);

		DLNAThumbnailInputStream result = null;
		try {
			if (cachedThumbnail != null) {
				result = DLNAThumbnailInputStream.toThumbnailInputStream(new FileInputStream(cachedThumbnail));
			} else if (getMediaInfo() != null && getMediaInfo().getThumbnail() != null) {
				result = getMediaInfo().getThumbnailInputStream();
			}
		} catch (IOException e) {
			LOGGER.debug("An error occurred while getting thumbnail for \"{}\", using generic thumbnail instead: {}", getName(), e.getMessage());
			LOGGER.trace("", e);
		}
		return result != null ? result : super.getThumbnailInputStream();
	}

	@Override
	public boolean isSubSelectable() {
		return true;
	}

	@Override
	public String write() {
		return getName() + ">" + directory.getAbsolutePath();
	}

	@Override
	public String getName() {
		if (super.getName() == null) {
			if (directory == null) {
				return null;
			}

			if (directory.getName().trim().isEmpty()) {
				if (Platform.isWindows()) {
					setName(PlatformUtils.INSTANCE.getDiskLabel(directory));
				}
				if (super.getName() != null && super.getName().length() > 0) {
					setName(directory.getAbsolutePath().substring(0, 1) + ":\\ [" + super.getName() + "]");
				} else {
					setName(directory.getAbsolutePath().substring(0, 1));
				}
			} else {
				setName(directory.getName());
			}
		}
		return super.getName().replaceAll("_imdb([^_]+)_", "");
	}

	@Override
	public File getSystemFile() {
		return directory;
	}

}
