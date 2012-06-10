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

import net.pms.formats.Format;
import net.pms.util.FileUtil;
import de.innosystec.unrar.Archive;
import de.innosystec.unrar.rarfile.FileHeader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RarredEntry extends DLNAResource implements IPushOutput {
	private static final Logger LOGGER = LoggerFactory.getLogger(RarredEntry.class);
	private String name;
	private File pere;
	private String fileheadername;
	private long length;

	@Override
	protected String getThumbnailURL() {
		if (getType() == Format.IMAGE || getType() == Format.AUDIO) // no thumbnail support for now for real based disk images
		{
			return null;
		}
		return super.getThumbnailURL();
	}

	public RarredEntry(String name, File pere, String fileheadername, long length) {
		this.fileheadername = fileheadername;
		this.name = name;
		this.pere = pere;
		this.length = length;
	}

	public InputStream getInputStream() throws IOException {
		return null;
	}

	public String getName() {
		return name;
	}

	public long length() {
		if (getPlayer() != null && getPlayer().type() != Format.IMAGE) {
			return DLNAMediaInfo.TRANS_SIZE;
		}
		return length;
	}

	public boolean isFolder() {
		return false;
	}

	public long lastModified() {
		return 0;
	}

	@Override
	public String getSystemName() {
		return FileUtil.getFileNameWithoutExtension(pere.getAbsolutePath()) + "." + FileUtil.getExtension(name);
	}

	@Override
	public boolean isValid() {
		checktype();
		setSrtFile(FileUtil.doesSubtitlesExists(pere, null));
		return getExt() != null;
	}

	@Override
	public boolean isUnderlyingSeekSupported() {
		return length() < MAX_ARCHIVE_SIZE_SEEK;
	}

	@Override
	public void push(final OutputStream out) throws IOException {
		Runnable r = new Runnable() {

			public void run() {
				Archive rarFile = null;
				try {
					rarFile = new Archive(pere);
					FileHeader header = null;
					for (FileHeader fh : rarFile.getFileHeaders()) {
						if (fh.getFileNameString().equals(fileheadername)) {
							header = fh;
							break;
						}
					}
					if (header != null) {
						LOGGER.trace("Starting the extraction of " + header.getFileNameString());
						rarFile.extractFile(header, out);
					}
				} catch (Exception e) {
					LOGGER.debug("Unpack error, maybe it's normal, as backend can be terminated: " + e.getMessage());
				} finally {
					try {
						rarFile.close();
						out.close();
					} catch (IOException e) {
						LOGGER.debug("Caught exception", e);
					}
				}
			}
		};
		new Thread(r, "Rar Extractor").start();
	}

	@Override
	public void resolve() {
		if (getExt() == null || !getExt().isVideo()) {
			return;
		}
		boolean found = false;
		if (!found) {
			if (getMedia() == null) {
				setMedia(new DLNAMediaInfo());
			}
			found = !getMedia().isMediaparsed() && !getMedia().isParsing();
			if (getExt() != null) {
				InputFile input = new InputFile();
				input.setPush(this);
				input.setSize(length());
				getExt().parse(getMedia(), input, getType());
			}
		}
		super.resolve();
	}

	@Override
	public InputStream getThumbnailInputStream() throws IOException {
		if (getMedia() != null && getMedia().getThumb() != null) {
			return getMedia().getThumbnailInputStream();
		} else {
			return super.getThumbnailInputStream();
		}
	}
}
