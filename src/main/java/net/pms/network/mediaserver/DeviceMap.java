/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.network.mediaserver;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceMap<T extends Renderer> extends HashMap<String, HashMap<String, T>> {
	private static final Logger LOGGER = LoggerFactory.getLogger(DeviceMap.class);
	private static final long serialVersionUID = 1510675619549915489L;

	private final Class<T> tClass;

	public DeviceMap(Class<T> t) {
		tClass = t;
	}

	public T get(String uuid, String id) {
		if (!containsKey(uuid)) {
			put(uuid, new HashMap<>());
		}
		HashMap<String, T> m = get(uuid);
		if (!m.containsKey(id)) {
			try {
				T newitem = tClass.getDeclaredConstructor().newInstance();
				newitem.uuid = uuid;
				m.put(id, newitem);
			} catch (Exception e) {
				LOGGER.error("Error instantiating item {}[{}]: {}", uuid, id, e.getMessage());
				LOGGER.trace("", e);
			}
		}
		return m.get(id);
	}

	public String get(String uuid, String id, String key) {
		return get(uuid, id).data.get(key);
	}

	public boolean containsKey(String uuid, String id) {
		return containsKey(uuid) && get(uuid).containsKey(id);
	}

	public HashMap<String, String> getData(String uuid, String id) {
		if (containsKey(uuid, id)) {
			return get(uuid, id).data;
		}
		return null;
	}

	public T put(String uuid, String id, T item) {
		item.uuid = uuid;
		if (!containsKey(uuid)) {
			get(uuid, "0");
		}
		return get(uuid).put(id, item);
	}

	public String put(String uuid, String id, String key, String value) {
		return get(uuid, id).data.put(key, value);
	}

	public void mark(String uuid, int property, Object value) {
		HashMap<String, T> m = get(uuid);
		if (m != null) {
			for (T i : m.values()) {
				switch (property) {
					case UPNPControl.ACTIVE:
						i.setActive((boolean) value);
						break;
					case UPNPControl.RENEW:
						i.renew = (boolean) value;
						break;
					case UPNPControl.CONTROLS:
						i.controls = (int) value;
						break;
					default:
						break;
				}
			}
		}
	}
}
