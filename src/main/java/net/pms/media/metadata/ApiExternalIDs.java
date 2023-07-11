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
package net.pms.media.metadata;

import java.util.ArrayList;
import java.util.HashMap;

public class ApiExternalIDs extends ArrayList<HashMap<String, String>> {

	public boolean hasImdbId() {
		return getImdbId() != null;
	}

	public String getImdbId() {
		return !isEmpty() && get(0).containsKey("imdb_id") ? get(0).get("imdb_id") : null;
	}

	public boolean hasFacebookId() {
		return getFacebookId() != null;
	}

	public String getFacebookId() {
		return !isEmpty() && get(0).containsKey("facebook_id") ? get(0).get("facebook_id") : null;
	}

	public boolean hasInstagramId() {
		return getInstagramId() != null;
	}

	public String getInstagramId() {
		return !isEmpty() && get(0).containsKey("instagram_id") ? get(0).get("instagram_id") : null;
	}

	public boolean hasTwitterId() {
		return getTwitterId() != null;
	}

	public String getTwitterId() {
		return !isEmpty() && get(0).containsKey("twitter_id") ? get(0).get("twitter_id") : null;
	}

}