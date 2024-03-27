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
package net.pms.media.video.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ApiImages extends  ArrayList<HashMap<String, List<ApiImage>>> {
	private static final String BACKDROPS = "backdrops";
	private static final String LOGOS = "logos";
	private static final String POSTERS = "posters";

	public List<ApiImage> getBackDrops() {
		return this.isEmpty() ? this.get(0).get(BACKDROPS) : null;
	}

	public boolean hasBackDrop() {
		return !this.isEmpty() && this.get(0).containsKey(BACKDROPS) && !this.get(0).get(BACKDROPS).isEmpty();
	}

	public List<ApiImage> getLogos() {
		return this.isEmpty() ? this.get(0).get(LOGOS) : null;
	}

	public boolean hasLogo() {
		return !this.isEmpty() && this.get(0).containsKey(LOGOS) && !this.get(0).get(LOGOS).isEmpty();
	}

	public List<ApiImage> getPosters() {
		return this.isEmpty() ? this.get(0).get(POSTERS) : null;
	}

	public boolean hasPoster() {
		return !this.isEmpty() && this.get(0).containsKey(POSTERS) && !this.get(0).get(POSTERS).isEmpty();
	}

}