/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.dlna;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.pms.formats.Format;
import net.pms.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZippedEntry extends DLNAResource implements IPushOutput {
	private static final Logger LOGGER = LoggerFactory.getLogger(ZippedEntry.class);
	private File file;
	private String zeName;
	private long length;
	private ZipFile zipFile;

	@Override
	protected String getThumbnailURL(DLNAImageProfile profile) {
		if (getType() == Format.IMAGE || getType() == Format.AUDIO) {
			// no thumbnail support for now for zipped videos
			return null;
		}

		return super.getThumbnailURL(profile);
	}

	public ZippedEntry(File file, String zeName, long length) {
		this.zeName = zeName;
		this.file = file;
		this.length = length;
	}

	@Override
	public InputStream getInputStream() {
		return null;
	}

	@Override
	public String getName() {
		return zeName;
	}

	@Override
	public long length() {
		if (getPlayer() != null && getPlayer().type() != Format.IMAGE) {
			return DLNAMediaInfo.TRANS_SIZE;
		}

		return length;
	}

	@Override
	public boolean isFolder() {
		return false;
	}

	// XXX unused
	@Deprecated
	public long lastModified() {
		return 0;
	}

	@Override
	public String getSystemName() {
		return FileUtil.getFileNameWithoutExtension(file.getAbsolutePath()) + "." + FileUtil.getExtension(zeName);
	}

	@Override
	public boolean isValid() {
		resolveFormat();
		setHasExternalSubtitles(FileUtil.isSubtitlesExists(file, null));
		return getFormat() != null;
	}

	@Override
	public boolean isUnderlyingSeekSupported() {
		return length() < MAX_ARCHIVE_SIZE_SEEK;
	}

	@Override
	public void push(final OutputStream out) throws IOException {
		Runnable r = new Runnable() {
			InputStream in = null;

			@Override
			public void run() {
				try {
					int n = -1;
					byte[] data = new byte[65536];
					zipFile = new ZipFile(file);
					ZipEntry ze = zipFile.getEntry(zeName);
					in = zipFile.getInputStream(ze);

					while ((n = in.read(data)) > -1) {
						out.write(data, 0, n);
					}

					in.close();
					in = null;
				} catch (Exception e) {
					LOGGER.error("Unpack error. Possibly harmless.", e);
				} finally {
					try {
						if (in != null) {
							in.close();
						}
						zipFile.close();
						out.close();
					} catch (IOException e) {
						LOGGER.debug("Caught exception", e);
					}
				}
			}
		};

		new Thread(r, "Zip Extractor").start();
	}

	@Override
	protected void resolveOnce() {
		if (getFormat() == null || !getFormat().isVideo()) {
			return;
		}

		boolean found = false;

		if (!found) {
			if (getMedia() == null) {
				setMedia(new DLNAMediaInfo());
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
