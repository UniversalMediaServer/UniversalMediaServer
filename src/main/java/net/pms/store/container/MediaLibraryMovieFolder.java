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
package net.pms.store.container;

import java.io.IOException;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.media.MediaInfo;
import net.pms.renderers.Renderer;
import net.pms.store.MediaInfoStore;
import net.pms.store.ThumbnailStore;

/**
 * @author SurfaceS
 */
public class MediaLibraryMovieFolder extends MediaLibraryFolder {

	private final String filename;

	public MediaLibraryMovieFolder(Renderer renderer, String name, String filename, String[] sql, int[] expectedOutput) {
		super(renderer, null, sql, expectedOutput);
		setName(name);
		this.filename = filename;
	}

	@Override
	public String getSystemName() {
		return "movie_" + getName();
	}

	/**
	 * Returns the localized display name.
	 *
	 * @return The localized display name.
	 */
	@Override
	public String getLocalizedDisplayName(String lang) {
		if (getMediaInfo() != null && mediaInfo.hasVideoMetadata()) {
			mediaInfo.getVideoMetadata().ensureHavingTranslation(lang);
			return mediaInfo.getVideoMetadata().getTitle(lang);
		}
		return super.getDisplayNameBase();
	}

	@Override
	public String getDisplayNameBase() {
		return getLocalizedDisplayName(null);
	}

	@Override
	public MediaInfo getMediaInfo() {
		if (mediaInfo == null && filename != null) {
			mediaInfo = MediaInfoStore.getMediaInfo(filename);
		}
		return mediaInfo;
	}

	/**
	 * @return a {@link InputStream} that represents the thumbnail used.
	 * @throws IOException
	 *
	 * @see StoreResource#getThumbnailInputStream()
	 */
	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		if (getMediaInfo() != null && mediaInfo.getThumbnailId() != null) {
			return ThumbnailStore.getThumbnailInputStream(mediaInfo.getThumbnailId());
		}
		return super.getThumbnailInputStream();
	}

}
