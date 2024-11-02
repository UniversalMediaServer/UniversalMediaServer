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
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.formats.Format;
import net.pms.media.MediaInfo;
import net.pms.parsers.Parser;
import net.pms.renderers.Renderer;
import net.pms.store.StoreItem;
import net.pms.util.IPushOutput;
import net.pms.util.InputFile;

abstract class ArchiveEntry extends StoreItem implements IPushOutput {

	protected final File file;
	protected final String entryName;
	protected final long length;
	protected final String name;

	protected ArchiveEntry(Renderer renderer, File file, String entryName, long length) {
		super(renderer);
		this.file = file;
		this.entryName = entryName;
		this.length = length;
		this.name = getName(entryName);
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
		return file.getAbsolutePath() + "#" + entryName;
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
	protected void resolveOnce() {
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
		return entryName + ">" + file.getAbsolutePath() + ">" + length;
	}

	private static String getName(String entryName) {
		String name = entryName;
		while (name.endsWith("/") || name.endsWith("\\")) {
			name = name.substring(0, name.length() - 1);
		}
		if (name.contains("/")) {
			name = name.substring(name.lastIndexOf("/") + 1);
		}
		if (name.contains("\\")) {
			name = name.substring(name.lastIndexOf("\\") + 1);
		}
		return name;
	}

}
