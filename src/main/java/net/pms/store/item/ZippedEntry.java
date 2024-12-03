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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.pms.renderers.Renderer;
import net.pms.util.ArchiveFileInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZippedEntry extends ArchiveEntry {

	private static final Logger LOGGER = LoggerFactory.getLogger(ZippedEntry.class);

	public ZippedEntry(Renderer renderer, File file, String entryName, long length) {
		super(renderer, file, entryName, length);
	}

	@Override
	public InputStream getInputStream() {
		return ArchiveFileInputStream.getZipEntryInputStream(file, entryName);
	}

	@Override
	public void push(final OutputStream out) throws IOException {
		Runnable r = () -> {
			try (InputStream in = ArchiveFileInputStream.getZipEntryInputStream(file, entryName)) {
				in.transferTo(out);
			} catch (IOException e) {
				if (!"Pipe closed".equals(e.getMessage())) {
					LOGGER.error("UnZip error. Possibly harmless.", e);
				}
			} finally {
				try {
					out.close();
				} catch (IOException e) {
					LOGGER.debug("Caught exception", e);
				}
			}
		};
		new Thread(r, "Zip Extractor").start();
	}

}
