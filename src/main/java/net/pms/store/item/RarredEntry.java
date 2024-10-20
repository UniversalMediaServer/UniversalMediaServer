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
package net.pms.store.item;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import com.github.junrar.volume.FileVolumeManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.pms.renderers.Renderer;
import net.pms.util.ArchiveFileInputStream;
import net.pms.util.IPushOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RarredEntry extends ArchiveEntry implements IPushOutput {

	private static final Logger LOGGER = LoggerFactory.getLogger(RarredEntry.class);

	public RarredEntry(Renderer renderer, File file, String entryName, long length) {
		super(renderer, file, entryName, length);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return ArchiveFileInputStream.getRarEntryInputStream(file, entryName);
	}

	@Override
	public void push(final OutputStream out) throws IOException {
		Runnable r = () -> {
			try (Archive rarFile = new Archive(new FileVolumeManager(file), null, null)) {
				FileHeader header = null;
				for (FileHeader fh : rarFile.getFileHeaders()) {
					if (fh.getFileName().equals(entryName)) {
						header = fh;
						break;
					}
				}
				if (header != null) {
					LOGGER.trace("Starting the extraction of " + header.getFileName());
					rarFile.extractFile(header, out);
				}
			} catch (RarException | IOException e) {
				LOGGER.debug("Unpack error, maybe it's normal, as backend can be terminated: " + e.getMessage());
			} finally {
				try {
					out.close();
				} catch (IOException e) {
					LOGGER.debug("Caught exception", e);
				}
			}
		};

		new Thread(r, "Rar Extractor").start();
	}

}
