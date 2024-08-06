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

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.List;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableVideoMetadata;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.media.video.metadata.TvSeriesMetadata;
import net.pms.renderers.Renderer;
import net.pms.store.MediaInfoStore;
import net.pms.store.MediaStatusStore;
import net.pms.store.ThumbnailStore;

/**
 * @author SurfaceS
 */
public class MediaLibraryTvSeries extends MediaLibraryFolder {

	private final Long tvSeriesId;
	private TvSeriesMetadata tvSeriesMetadata;

	public MediaLibraryTvSeries(Renderer renderer, Long tvSeriesId, String[] sql, int[] expectedOutput) {
		super(renderer, null, sql, expectedOutput);
		setName(tvSeriesId.toString());
		this.tvSeriesId = tvSeriesId;
		setSortable(true);
	}

	@Override
	public String getSystemName() {
		return "tv_series_" + getName();
	}

	/**
	 * Returns the localized display name.
	 *
	 * @return The localized display name.
	 */
	@Override
	public String getLocalizedDisplayName(String lang) {
		if (getTvSeriesMetadata() != null) {
			tvSeriesMetadata.ensureHavingTranslation(lang);
			return tvSeriesMetadata.getTitle(lang);
		}
		return super.getDisplayNameBase();
	}

	@Override
	public String getDisplayNameBase() {
		return getLocalizedDisplayName(null);
	}

	public synchronized TvSeriesMetadata getTvSeriesMetadata() {
		if (tvSeriesMetadata == null && tvSeriesId != null) {
			tvSeriesMetadata = MediaInfoStore.getTvSeriesMetadata(tvSeriesId);
		}
		return tvSeriesMetadata;
	}

	/**
	 * @return a {@link InputStream} that represents the thumbnail used.
	 * @throws IOException
	 *
	 * @see StoreResource#getThumbnailInputStream()
	 */
	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		if (getTvSeriesMetadata() != null && getTvSeriesMetadata().getThumbnailId() != null) {
			return ThumbnailStore.getThumbnailInputStream(getTvSeriesMetadata().getThumbnailId());
		}

		try {
			return super.getThumbnailInputStream();
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public boolean isFullyPlayedAware() {
		return true;
	}

	@Override
	public boolean isFullyPlayed() {
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				List<String> filenames = MediaTableVideoMetadata.getTvEpisodesFilesByTvSeriesId(connection, tvSeriesId);
				for (String filename : filenames) {
					if (renderer.hasShareAccess(new File(filename)) && !MediaStatusStore.isFullyPlayed(filename, renderer.getAccountUserId())) {
						return false;
					}
				}
				return true;
			}
		} finally {
			MediaDatabase.close(connection);
		}
		return false;
	}

	@Override
	public void setFullyPlayed(boolean fullyPlayed) {
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				List<String> filenames = MediaTableVideoMetadata.getTvEpisodesFilesByTvSeriesId(connection, tvSeriesId);
				for (String filename : filenames) {
					if (renderer.hasShareAccess(new File(filename))) {
						MediaStatusStore.setFullyPlayed(filename, renderer.getAccountUserId(), fullyPlayed, null);
					}
				}
			}
		} finally {
			MediaDatabase.close(connection);
		}
	}
}
