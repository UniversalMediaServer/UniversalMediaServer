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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import net.pms.Messages;
import net.pms.configuration.UmsConfiguration;
import net.pms.formats.FormatFactory;
import net.pms.formats.ISOVOB;
import net.pms.media.MediaInfo;
import net.pms.parsers.MPlayerParser;
import net.pms.renderers.Renderer;
import net.pms.util.FileUtil;
import net.pms.util.StringUtil;
import org.apache.commons.lang3.StringUtils;

public class DVDISOTitle extends DLNAResource {

	private final File file;
	private final int title;
	private final String parentName;

	private long length;

	public DVDISOTitle(File file, String parentName, int title) {
		this.file = file;
		this.title = title;
		this.parentName = parentName;
		setLastModified(file.lastModified());
	}

	@Override
	protected void resolveOnce() {
		if (getMedia() == null) {
			MediaInfo media = new MediaInfo();
			MPlayerParser.parseDvdTitle(media, file, title);
			setMedia(media);
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
	public boolean isFolder() {
		return false;
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
		return MediaInfo.TRANS_SIZE;
	}

	// Ditlew
	@Override
	public long length(Renderer renderer) {
		// WDTV Live at least, needs a realistic size for stop/resume to works proberly. 2030879 = ((15000 + 256) * 1024 / 8 * 1.04) : 1.04 = overhead
		int cbrVideoBitrate = getDefaultRenderer().getCBRVideoBitrate();
		return (cbrVideoBitrate > 0) ? (long) (((cbrVideoBitrate + 256) * 1024 / (double) 8 * 1.04) * getMedia().getDurationInSeconds()) : length();
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

			if (StringUtils.isNotBlank(configuration.getAlternateThumbFolder())) {
				thumbFolder = new File(configuration.getAlternateThumbFolder());

				if (!thumbFolder.isDirectory()) {
					break;
				}
			}

			alternativeCheck = true;
		}

		if (cachedThumbnail != null) {
			return DLNAThumbnailInputStream.toThumbnailInputStream(new FileInputStream(cachedThumbnail));
		} else if (getMedia() != null && getMedia().getThumb() != null) {
			return getMedia().getThumbnailInputStream();
		} else {
			return getGenericThumbnailInputStream(null);
		}
	}

	@Override
	protected String getDisplayNameSuffix(Renderer renderer, UmsConfiguration configuration) {
		String nameSuffix = super.getDisplayNameSuffix(renderer, configuration);
		if (
			getMedia() != null &&
			renderer != null &&
			getMedia().getDurationInSeconds() > 0 &&
			renderer.isShowDVDTitleDuration()
		) {
			nameSuffix += " (" + StringUtil.convertTimeToString(getMedia().getDurationInSeconds(), "%01d:%02d:%02.0f") + ")";
		}

		return nameSuffix;
	}
}
