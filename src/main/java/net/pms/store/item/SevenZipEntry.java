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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import net.pms.renderers.Renderer;
import net.pms.util.ArchiveFileInputStream;
import net.pms.util.IPushOutput;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SevenZipEntry extends ArchiveEntry implements IPushOutput {

	private static final Logger LOGGER = LoggerFactory.getLogger(SevenZipEntry.class);

	public SevenZipEntry(Renderer renderer, File file, String entryName, long length) {
		super(renderer, file, entryName, length);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return ArchiveFileInputStream.getSevenZipEntryInputStream(file, entryName);
	}

	@Override
	public void push(final OutputStream out) throws IOException {
		Runnable r = () -> {
			try (RandomAccessFile rf = new RandomAccessFile(file, "r");
					IInArchive arc = SevenZip.openInArchive(null, new RandomAccessFileInStream(rf))) {
				ISimpleInArchive simpleInArchive = arc.getSimpleInterface();
				ISimpleInArchiveItem realItem = null;

				for (ISimpleInArchiveItem item : simpleInArchive.getArchiveItems()) {
					if (item.getPath().equals(entryName)) {
						realItem = item;
						break;
					}
				}

				if (realItem == null) {
					LOGGER.trace("No such item " + entryName + " found in archive");
					return;
				}

				realItem.extractSlow((byte[] data) -> {
					try {
						out.write(data);
					} catch (IOException e) {
						LOGGER.debug("Caught exception", e);
						throw new SevenZipException();
					}
					return data.length;
				});
			} catch (FileNotFoundException | SevenZipException e) {
				LOGGER.debug("Unpack error. Possibly harmless.", e.getMessage());
			} catch (IOException e) {
				LOGGER.error("Unpack error. Possibly harmless.", e.getMessage());
			} finally {
				try {
					out.close();
				} catch (IOException e) {
					LOGGER.debug("Caught exception", e);
				}
			}
		};

		new Thread(r, "7Zip Extractor").start();
	}

}
