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
import net.pms.dlna.DLNAImageProfile;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.formats.Format;
import net.pms.media.MediaInfo;
import net.pms.parsers.Parser;
import net.pms.renderers.Renderer;
import net.pms.store.StoreItem;
import net.pms.util.FileUtil;
import net.pms.util.IPushOutput;
import net.pms.util.InputFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RarredEntry extends StoreItem implements IPushOutput {
	private static final Logger LOGGER = LoggerFactory.getLogger(RarredEntry.class);
	private final String name;
	private final File file;
	private final String fileHeaderName;
	private final long length;

	public RarredEntry(Renderer renderer, String name, File file, String fileHeaderName, long length) {
		super(renderer);
		this.fileHeaderName = fileHeaderName;
		this.name = name;
		this.file = file;
		this.length = length;
	}

	@Override
	public String getThumbnailURL(DLNAImageProfile profile) {
		if (getType() == Format.IMAGE || getType() == Format.AUDIO) { // no thumbnail support for now for rarred videos
			return null;
		}

		return super.getThumbnailURL(profile);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return null;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public long length() {
		if (isTranscoded() && getTranscodingSettings().getEngine().type() != Format.IMAGE) {
			return TRANS_SIZE;
		}

		return length;
	}

	@Override
	public String getSystemName() {
		return FileUtil.getFileNameWithoutExtension(file.getAbsolutePath()) + "." + FileUtil.getExtension(name);
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
			Archive rarFile = null;
			try {
				rarFile = new Archive(new FileVolumeManager(file), null, null);
				FileHeader header = null;
				for (FileHeader fh : rarFile.getFileHeaders()) {
					if (fh.getFileName().equals(fileHeaderName)) {
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
					if (rarFile != null) {
						rarFile.close();
					}
					out.close();
				} catch (IOException e) {
					LOGGER.debug("Caught exception", e);
				}
			}
		};

		new Thread(r, "Rar Extractor").start();
	}

	@Override
	protected void resolveOnce() {
		if (getFormat() == null || !getFormat().isVideo()) {
			return;
		}

		if (getMediaInfo() == null) {
			setMediaInfo(new MediaInfo());
		}

		if (getFormat() != null) {
			InputFile input = new InputFile();
			input.setPush(this);
			input.setSize(length());
			Parser.parse(getMediaInfo(), input, getFormat(), getType());
			if (getMediaInfo() != null && getMediaInfo().isSLS()) {
				setFormat(getMediaInfo().getAudioVariantFormat());
			}
		}
	}

	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		if (getMediaInfo() != null && getMediaInfo().getThumbnail() != null) {
			return getMediaInfo().getThumbnailInputStream();
		} else {
			return super.getThumbnailInputStream();
		}
	}
}
