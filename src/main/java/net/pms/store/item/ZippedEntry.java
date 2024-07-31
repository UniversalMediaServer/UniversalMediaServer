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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
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

public class ZippedEntry extends StoreItem implements IPushOutput {
	private static final Logger LOGGER = LoggerFactory.getLogger(ZippedEntry.class);
	private final File file;
	private final String zeName;
	private final long length;
	private ZipFile zipFile;

	public ZippedEntry(Renderer renderer, File file, String zeName, long length) {
		super(renderer);
		this.zeName = zeName;
		this.file = file;
		this.length = length;
	}

	@Override
	public String getThumbnailURL(DLNAImageProfile profile) {
		if (getType() == Format.IMAGE || getType() == Format.AUDIO) {
			// no thumbnail support for now for zipped videos
			return null;
		}

		return super.getThumbnailURL(profile);
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
		if (isTranscoded() && getTranscodingSettings().getEngine().type() != Format.IMAGE) {
			return TRANS_SIZE;
		}

		return length;
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
				int n;
				byte[] data = new byte[65536];
				zipFile = new ZipFile(file);
				ZipEntry ze = zipFile.getEntry(zeName);
				try (InputStream in = zipFile.getInputStream(ze)) {
					while ((n = in.read(data)) > -1) {
						out.write(data, 0, n);
					}
				}
			} catch (IOException e) {
				LOGGER.error("Unpack error. Possibly harmless.", e);
			} finally {
				try {
					zipFile.close();
					out.close();
				} catch (IOException e) {
					LOGGER.debug("Caught exception", e);
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

	@Override
	public String write() {
		return getName() + ">" + file.getAbsolutePath() + ">" + length;
	}
}
