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
package net.pms.renderers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renderer Filter class.
 */
public class RendererFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(RendererFilter.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final List<String> FILTER = new ArrayList<>();
	private static final Set<String> LOGGED = new HashSet<>();

	private RendererFilter() {
		throw new IllegalStateException("Static class");
	}

	public static synchronized void reset() {
		FILTER.clear();
		LOGGED.clear();
		String rendererFilter = CONFIGURATION.getRenderersFilter();
		if (rendererFilter != null) {
			String[] uuids = rendererFilter.split(",");
			for (String uuid : uuids) {
				if (StringUtils.isNotBlank(uuid)) {
					FILTER.add(uuid.toLowerCase());
				}
			}
		}
		for (String uuid : FILTER) {
			Renderer renderer = ConnectedRenderers.getRendererByUUID(uuid);
			if (renderer != null) {
				renderer.setAllowed(isAllowed(uuid, false));
			}
		}
	}

	public static boolean isAllowed(String uuid) {
		return isAllowed(uuid, true);
	}

	/**
	 * Updates the renderers filter.
	 *
	 * @param uuid the renderer uuid
	 * @param isAllowed
	 */
	public static synchronized void setAllowed(String uuid, boolean isAllowed) {
		if (StringUtils.isBlank(uuid)) {
			return;
		}
		String uuidLowerCase = uuid.toLowerCase();
		if (isAllowed == isAllowed(uuidLowerCase, false)) {
			LOGGER.trace("Renderer Filter: No change made, {} is already {}", uuidLowerCase, isAllowed ? "allowed" : "blocked");
			return;
		}
		if (CONFIGURATION.isRenderersBlockedByDefault()) {
			if (isAllowed) {
				FILTER.add(uuidLowerCase);
			} else {
				FILTER.remove(uuidLowerCase);
			}
		} else {
			if (isAllowed) {
				FILTER.remove(uuidLowerCase);
			} else {
				FILTER.add(uuidLowerCase);
			}
		}
		Renderer renderer = ConnectedRenderers.getRendererByUUID(uuidLowerCase);
		if (renderer != null) {
			renderer.setAllowed(isAllowed);
		}
		LOGGED.remove(uuidLowerCase);
		// persist the change
		CONFIGURATION.setRenderersFilter(getNormalizedFilter());
	}

	private static synchronized boolean isAllowed(String uuid, boolean logging) {
		if (StringUtils.isBlank(uuid)) {
			return !CONFIGURATION.isRenderersBlockedByDefault();
		}
		String uuidLowerCase = uuid.toLowerCase();
		boolean log = logging && isFirstDecision(uuidLowerCase);
		boolean inFilter = FILTER.contains(uuidLowerCase);
		if (inFilter == CONFIGURATION.isRenderersBlockedByDefault()) {
			if (log) {
				LOGGER.trace("Renderer Filter: Access granted to {}", uuidLowerCase);
			}
			return true;
		} else {
			if (log) {
				LOGGER.trace("Renderer Filter: Access denied to {}", uuidLowerCase);
			}
			return false;
		}
	}

	public static synchronized boolean getBlockedByDefault() {
		return CONFIGURATION.isRenderersBlockedByDefault();
	}

	public static synchronized void setBlockedByDefault(boolean value) {
		if (value != CONFIGURATION.isRenderersBlockedByDefault()) {
			CONFIGURATION.setRenderersFilter("");
			CONFIGURATION.setRenderersBlockedByDefault(value);
			reset();
		}
	}

	private static synchronized String getNormalizedFilter() {
		StringBuilder b = new StringBuilder();
		if (!FILTER.isEmpty()) {
			for (String r : FILTER) {
				if (!StringUtils.isBlank(r)) {
					b.append(r).append(",");
				}
			}
			b.deleteCharAt(b.length() - 1);
		}
		return b.toString();
	}

	private static synchronized boolean isFirstDecision(String uuid) {
		if (!LOGGED.contains(uuid)) {
			LOGGED.add(uuid);
			return true;
		}
		return false;
	}

}
