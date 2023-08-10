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
package net.pms.media;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableFilesStatus;

public class MediaStatusStore {

	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final Map<Integer, Map<String, MediaStatus>> STORE = new HashMap<>();

	private MediaStatusStore() {
		//should not be instantiated
	}

	public static MediaStatus getMediaStatus(int userId, String filename) {
		synchronized (STORE) {
			if (STORE.containsKey(userId) && STORE.get(userId) != null && STORE.get(userId).containsKey(filename)) {
				return STORE.get(userId).get(filename);
			}
			MediaStatus mediaStatus = null;
			if (CONFIGURATION.getUseCache()) {
				Connection connection = MediaDatabase.getConnectionIfAvailable();
				if (connection != null) {
					mediaStatus = MediaTableFilesStatus.getData(connection, filename);
				}
				MediaDatabase.close(connection);
			}
			if (mediaStatus == null) {
				mediaStatus = new MediaStatus();
			}
			if (!STORE.containsKey(userId)) {
				STORE.put(userId, new HashMap<>());
			}
			STORE.get(userId).put(filename, mediaStatus);
			return mediaStatus;
		}
	}

	public static void clear(int userId) {
		synchronized (STORE) {
			STORE.get(userId).clear();
		}
	}

	public static void clear() {
		synchronized (STORE) {
			STORE.clear();
		}
	}

}
