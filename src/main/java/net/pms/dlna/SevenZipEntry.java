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
package net.pms.dlna;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import net.pms.formats.Format;
import net.pms.media.MediaInfo;
import net.pms.util.FileUtil;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SevenZipEntry extends DLNAResource implements IPushOutput {
	private static final Logger LOGGER = LoggerFactory.getLogger(SevenZipEntry.class);
	private final File file;
	private final String zeName;
	private final long length;
	private IInArchive arc;

	@Override
	protected String getThumbnailURL(DLNAImageProfile profile) {
		if (getType() == Format.IMAGE || getType() == Format.AUDIO) {
			// no thumbnail support for now for zipped videos
			return null;
		}

		return super.getThumbnailURL(profile);
	}

	public SevenZipEntry(File file, String zeName, long length) {
		this.zeName = zeName;
		this.file = file;
		this.length = length;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return null;
	}

	@Override
	public String getName() {
		return zeName;
	}

	@Override
	public long length() {
		if (getEngine() != null && getEngine().type() != Format.IMAGE) {
			return MediaInfo.TRANS_SIZE;
		}

		return length;
	}

	@Override
	public boolean isFolder() {
		return false;
	}

	@Override
	public String getSystemName() {
		return FileUtil.getFileNameWithoutExtension(file.getAbsolutePath()) + "." + FileUtil.getExtension(zeName);
	}

	@Override
	public boolean isValid() {
		resolveFormat();
		return getFormat() != null;
	}

	@Override
	public boolean isUnderlyingSeekSupported() {
		return length() < MAX_ARCHIVE_SIZE_SEEK;
	}

	@Override
	public void push(final OutputStream out) throws IOException {
		Runnable r = () -> {
			try {
				// byte data[] = new byte[65536];
				RandomAccessFile rf = new RandomAccessFile(file, "r");

				arc = SevenZip.openInArchive(null, new RandomAccessFileInStream(rf));
				ISimpleInArchive simpleInArchive = arc.getSimpleInterface();
				ISimpleInArchiveItem realItem = null;

				for (ISimpleInArchiveItem item : simpleInArchive.getArchiveItems()) {
					if (item.getPath().equals(zeName)) {
						realItem = item;
						break;
					}
				}

				if (realItem == null) {
					LOGGER.trace("No such item " + zeName + " found in archive");
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
			} finally {
				try {
					arc.close();
					out.close();
				} catch (IOException e) {
					LOGGER.debug("Caught exception", e);
				}
			}
		};

		new Thread(r, "7Zip Extractor").start();
	}

	@Override
	public synchronized void resolve() {
		if (getFormat() == null || !getFormat().isVideo()) {
			return;
		}

		// TODO: found seems not used here
		boolean found = false;

		if (!found) {
			if (getMedia() == null) {
				setMedia(new MediaInfo());
			}

			found = !getMedia().isMediaparsed() && !getMedia().isParsing();

			if (getFormat() != null) {
				InputFile input = new InputFile();
				input.setPush(this);
				input.setSize(length());
				getFormat().parse(getMedia(), input, getType(), null);
				if (getMedia() != null && getMedia().isSLS()) {
					setFormat(getMedia().getAudioVariantFormat());
				}
			}
		}

		super.resolve();
	}

	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		if (getMedia() != null && getMedia().getThumb() != null) {
			return getMedia().getThumbnailInputStream();
		} else {
			return super.getThumbnailInputStream();
		}
	}

	@Override
	public String write() {
		return getName() + ">" + file.getAbsolutePath() + ">" + length;
	}
}
