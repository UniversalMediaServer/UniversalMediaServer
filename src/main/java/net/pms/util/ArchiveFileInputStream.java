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
package net.pms.util;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import com.github.junrar.volume.FileVolumeManager;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.RandomAccessFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class close the archive file on InputStream close.
 *
 * @author SurfaceS
 */
public class ArchiveFileInputStream extends InputStream {

	private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveFileInputStream.class);

	protected final File file;
	protected final String name;
	protected final InputStream inputStream;
	protected final Closeable closeable;

	protected ArchiveFileInputStream(File file, String name, InputStream inputStream, Closeable closeable) {
		this.file = file;
		this.name = name;
		this.inputStream = inputStream;
		this.closeable = closeable;
	}

	@Override
	public void close() throws IOException {
		if (inputStream != null) {
			inputStream.close();
		}
		if (closeable != null) {
			closeable.close();
		}
	}

	@Override
	public int read() throws IOException {
		return inputStream.read();
	}

	@Override
	public int read(byte[] b) throws IOException {
		return inputStream.read(b);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return inputStream.read(b, off, len);
	}

	@Override
	public byte[] readAllBytes() throws IOException {
		return inputStream.readAllBytes();
	}

	@Override
	public byte[] readNBytes(int len) throws IOException {
		return inputStream.readNBytes(len);
	}

	@Override
	public int readNBytes(byte[] b, int off, int len) throws IOException {
		return inputStream.readNBytes(b, off, len);
	}

	@Override
	public long skip(long n) throws IOException {
		return inputStream.skip(n);
	}

	@Override
	public void skipNBytes(long n) throws IOException {
		inputStream.skipNBytes(n);
	}

	@Override
	public int available() throws IOException {
		return inputStream.available();
	}

	@Override
	public synchronized void mark(int readlimit) {
		inputStream.mark(readlimit);
	}

	@Override
	public synchronized void reset() throws IOException {
		inputStream.reset();
	}

	@Override
	public boolean markSupported() {
		return inputStream.markSupported();
	}

	@Override
	public long transferTo(OutputStream out) throws IOException {
		return inputStream.transferTo(out);
	}

	public static ArchiveFileInputStream getZipEntryInputStream(File file, String name) {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file);
			ZipEntry ze = zipFile.getEntry(name);
			if (ze != null) {
				return new ArchiveFileInputStream(file, name, zipFile.getInputStream(ze), zipFile);
			} else {
				LOGGER.error("Zip entry '{}' not found.", name);
			}
		} catch (IOException e) {
			LOGGER.error("ZipEntryInputStream error.", e);
		}
		if (zipFile != null) {
			try {
				zipFile.close();
			} catch (IOException e) {
				//ignore
			}
		}
		return null;
	}

	public static ArchiveFileInputStream getSevenZipEntryInputStream(File file, String name) {
		IInArchive arc = null;
		try {
			RandomAccessFile rf = new RandomAccessFile(file, "r");
			arc = SevenZip.openInArchive(null, new RandomAccessFileInStream(rf));
			ISimpleInArchive simpleInArchive = arc.getSimpleInterface();
			ISimpleInArchiveItem realItem = null;

			for (ISimpleInArchiveItem item : simpleInArchive.getArchiveItems()) {
				if (item.getPath().equals(name)) {
					realItem = item;
					break;
				}
			}

			if (realItem != null) {
				final ISimpleInArchiveItem archiveItem = realItem;
				final int bufferSize = (int) Math.max(Math.min(realItem.getSize(), 65536), 1);
				final PipedInputStream in = new PipedInputStream(bufferSize);
				Runnable r = () -> {
					try (PipedOutputStream out = new PipedOutputStream(in)) {
						archiveItem.extractSlow((byte[] data) -> {
							try {
								out.write(data);
							} catch (IOException e) {
								LOGGER.debug("Caught exception", e);
								throw new SevenZipException();
							}
							return data.length;
						});
					} catch (IOException e) {
						//ignore
					}
				};
				new Thread(r).start();
				return new ArchiveFileInputStream(file, name, in, arc);
			} else {
				LOGGER.error("SevenZip entry '{}' not found.", name);
			}
		} catch (FileNotFoundException | SevenZipException e) {
			LOGGER.debug("Unpack error. Possibly harmless.", e.getMessage());
		}
		if (arc != null) {
			try {
				arc.close();
			} catch (IOException e) {
				//ignore
			}
		}
		return null;
	}

	public static ArchiveFileInputStream getRarEntryInputStream(File file, String name) {
		Archive rarFile = null;
		try {
			rarFile = new Archive(new FileVolumeManager(file), null, null);
			FileHeader header = null;
			for (FileHeader fh : rarFile.getFileHeaders()) {
				if (fh.getFileName().equals(name)) {
					header = fh;
					break;
				}
			}
			if (header != null) {
				return new ArchiveFileInputStream(file, name, rarFile.getInputStream(header), rarFile);
			} else {
				LOGGER.error("Rar entry '{}' not found.", name);
			}
		} catch (RarException | IOException e) {
			LOGGER.error("RarEntryInputStream error: " + e.getMessage());
		}
		if (rarFile != null) {
			try {
				rarFile.close();
			} catch (IOException e) {
				//ignore
			}
		}
		return null;
	}

}
