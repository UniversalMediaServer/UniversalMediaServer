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
import net.pms.renderers.Renderer;

/**
 * @author Surf@ceS
 */
public class MediaLibraryTvEpisode extends RealFile {

	private boolean isEpisodeWithinSeasonFolder = false;

	public MediaLibraryTvEpisode(Renderer renderer, File file, boolean isWithinSeasonFolder) {
		super(renderer, file);
		isEpisodeWithinSeasonFolder = isWithinSeasonFolder;
		setSortable(false);
	}

	@Override
	protected String getBaseNamePrettified() {
		return getBaseNamePrettified(isEpisodeWithinSeasonFolder(), isEpisodeWithinTVSeriesFolder());
	}

	/**
	 * @return Whether this is a TV episode being accessed within a season
	 * folder in the Media Library.
	 */
	public boolean isEpisodeWithinSeasonFolder() {
		return isEpisodeWithinSeasonFolder;
	}

	/**
	 * @return Whether this is a TV episode being accessed directly inside a TV
	 * series folder in the Media Library
	 */
	public boolean isEpisodeWithinTVSeriesFolder() {
		return !isEpisodeWithinSeasonFolder;
	}

}
