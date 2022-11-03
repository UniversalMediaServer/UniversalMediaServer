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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DynamicPlaylist extends Playlist {
	private final String savePath;
	private long start;

	public DynamicPlaylist(String name, String dir, int mode) {
		super(name, null, 0, mode);
		savePath = dir;
		start = 0;
	}

	@Override
	public void clear() {
		super.clear();
		start = 0;
	}

	@Override
	public void save() {
		if (start == 0) {
			start = System.currentTimeMillis();
		}
		Date d = new Date(start);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH_mm", Locale.US);
		list.save(new File(savePath, "dynamic_" + sdf.format(d) + ".ups"));
	}
}