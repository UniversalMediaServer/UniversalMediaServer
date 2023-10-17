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
import java.sql.Connection;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableTVSeries;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.media.video.metadata.TvSeriesMetadata;
import net.pms.renderers.Renderer;
import net.pms.store.ThumbnailStore;

/**
 * @author SurfaceS
 */
public class MediaLibraryTvSeries extends MediaLibraryFolder {

	private final Long tvSeriesId;
	private TvSeriesMetadata tvSeriesMetadata;

	public MediaLibraryTvSeries(Renderer renderer, Long tvSeriesId, String[] sql, int[] expectedOutput) {
		super(renderer, null, sql, expectedOutput);
		this.name = "tv_series_" + tvSeriesId;
		this.tvSeriesId = tvSeriesId;	
		this.isSortableByDisplayName = true;
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
		if (tvSeriesMetadata == null && tvSeriesId != null && MediaDatabase.isAvailable()) {
			Connection connection = null;
			try {
				connection = MediaDatabase.getConnectionIfAvailable();
				tvSeriesMetadata = MediaTableTVSeries.getTvSeriesMetadata(connection, tvSeriesId);
			} finally {
				MediaDatabase.close(connection);
			}
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
	public boolean isFullyPlayedMark() {
		return isFullyPlayed(renderer.getAccountUserId());
	}

	private boolean isFullyPlayed(int userId) {
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				return MediaTableTVSeries.isFullyPlayed(connection, name, userId);
			}
		} finally {
			MediaDatabase.close(connection);
		}
		return false;
	}

}
