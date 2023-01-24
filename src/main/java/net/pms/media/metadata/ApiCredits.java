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
import java.util.List;

public class ApiCredits extends  ArrayList<HashMap<String, List<ApiCredit>>> {
	private static final String CAST = "cast";
	private static final String CREW = "crew";

	public List<ApiCredit> getCast() {
		return this.isEmpty() ? this.get(0).get(CAST) : null;
	}

	public boolean hasCast() {
		return !this.isEmpty() && this.get(0).containsKey(CAST) && !this.get(0).get(CAST).isEmpty();
	}

	public List<ApiCredit> getCrew() {
		return this.isEmpty() ? this.get(0).get(CREW) : null;
	}

	public boolean hasCrew() {
		return !this.isEmpty() && this.get(0).containsKey(CREW) && !this.get(0).get(CREW).isEmpty();
	}

}