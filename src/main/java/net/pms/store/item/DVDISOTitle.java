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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import net.pms.Messages;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.formats.FormatFactory;
import net.pms.formats.ISOVOB;
import net.pms.media.MediaInfo;
import net.pms.parsers.MPlayerParser;
import net.pms.renderers.Renderer;
import net.pms.store.StoreItem;
import net.pms.util.FileUtil;
import net.pms.util.StringUtil;
import org.apache.commons.lang3.StringUtils;

public class DVDISOTitle extends StoreItem {

	private final File file;
	private final int title;
	private final String parentName;

	private long length;

	public DVDISOTitle(Renderer renderer, File file, String parentName, int title) {
		super(renderer);
		this.file = file;
		this.title = title;
		this.parentName = parentName;
		setLastModified(file.lastModified());
	}

	@Override
	protected void resolveOnce() {
		if (getMediaInfo() == null) {
			MediaInfo media = new MediaInfo();
			MPlayerParser.parseDvdTitle(media, file, title);
			setMediaInfo(media);
			length = media.getSize();
		}
	}

	public long getLength() {
		return length;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return null;
	}

	@Override
	public String getName() {
		return (StringUtils.isBlank(parentName) ? "" : parentName + " ") + Messages.getString("Title") + " " + title;
	}

	@Override
	public String getSystemName() {
		return file.getAbsolutePath() + File.separator + "Title " + title;
	}

	@Override
	public String getFileName() {
		return file.getAbsolutePath();
	}

	@Override
	public boolean isValid() {
		if (getFormat() == null) {
			setFormat(FormatFactory.getFormat(ISOVOB.class));
		}
		return true;
	}

	@Override
	public long length() {
		// WDTV Live at least, needs a realistic size for stop/resume to work properly. 2030879 = ((15000 + 256) * 1024 / 8 * 1.04) : 1.04 = overhead
		int cbrVideoBitrate = getDefaultRenderer().getCBRVideoBitrate();
		return (cbrVideoBitrate > 0) ? (long) (((cbrVideoBitrate + 256) * 1024 / (double) 8 * 1.04) * getMediaInfo().getDurationInSeconds()) : TRANS_SIZE;
	}

	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		File cachedThumbnail = null;
		File thumbFolder = null;
		boolean alternativeCheck = false;
		while (cachedThumbnail == null) {
			if (thumbFolder == null) {
				thumbFolder = file.getParentFile();
			}

			cachedThumbnail = FileUtil.replaceExtension(thumbFolder, file, "jpg", true, true);

			if (cachedThumbnail == null) {
				cachedThumbnail = FileUtil.replaceExtension(thumbFolder, file, "png", true, true);
			}

			if (cachedThumbnail == null) {
				cachedThumbnail = FileUtil.getFileNameWithAddedExtension(thumbFolder, file, ".cover.jpg");
			}

			if (cachedThumbnail == null) {
				cachedThumbnail = FileUtil.getFileNameWithAddedExtension(thumbFolder, file, ".cover.png");
			}

			if (alternativeCheck) {
				break;
			}

			if (StringUtils.isNotBlank(renderer.getUmsConfiguration().getAlternateThumbFolder())) {
				thumbFolder = new File(renderer.getUmsConfiguration().getAlternateThumbFolder());

				if (!thumbFolder.isDirectory()) {
					break;
				}
			}

			alternativeCheck = true;
		}

		if (cachedThumbnail != null) {
			return DLNAThumbnailInputStream.toThumbnailInputStream(new FileInputStream(cachedThumbnail));
		} else if (getMediaInfo() != null && getMediaInfo().getThumbnail() != null) {
			return getMediaInfo().getThumbnailInputStream();
		} else {
			return getGenericThumbnailInputStream(null);
		}
	}

	@Override
	public String getDisplayNameSuffix() {
		String nameSuffix = super.getDisplayNameSuffix();
		if (
			getMediaInfo() != null &&
			getMediaInfo().getDurationInSeconds() > 0 &&
			renderer.isShowDVDTitleDuration()
		) {
			nameSuffix += " (" + StringUtil.convertTimeToString(getMediaInfo().getDurationInSeconds(), "%01d:%02d:%02.0f") + ")";
		}

		return nameSuffix;
	}
}
