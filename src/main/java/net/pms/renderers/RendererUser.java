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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.util.HashMap;
import java.util.Map;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.iam.AccountService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RendererUser {

	private static final Logger LOGGER = LoggerFactory.getLogger(RendererUser.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final Map<String, Integer> USERS = new HashMap<>();
	private static final Gson GSON = new Gson();

	/**
	 * This class is not meant to be instantiated.
	 */
	private RendererUser() {
	}

	public static synchronized void reset() {
		USERS.clear();
		String rendererUser = CONFIGURATION.getRenderersUser().replace('|', '"');
		try {
			Map<String, Number> users = GSON.fromJson(rendererUser, USERS.getClass());
			for (Map.Entry<String, Number> entry : users.entrySet()) {
				USERS.put(entry.getKey(), entry.getValue().intValue());
			}
		} catch (JsonSyntaxException e) {
			LOGGER.error("Renderer User: error reading configuration value {}", rendererUser, e);
		}
	}

	public static synchronized void setRendererUser(String uuid, int userId) {
		if (StringUtils.isBlank(uuid)) {
			return;
		}
		String uuidLowerCase = uuid.toLowerCase();
		if (userId == getUserId(uuidLowerCase)) {
			LOGGER.trace("Renderer User: No change made, {} is already linked to userId {}", uuidLowerCase, userId);
			return;
		}
		if (userId > -1) {
			if (AccountService.getUserById(userId) == null) {
				LOGGER.error("Renderer User: trying to link renderer {} to inexistant userId {}", uuidLowerCase, userId);
			} else {
				LOGGER.trace("Renderer User: linking renderer {} to userId {}", uuidLowerCase, userId);
				USERS.put(uuidLowerCase, userId);
			}
		} else {
			LOGGER.trace("Renderer User: removing renderer {} linked userId", uuidLowerCase);
			USERS.remove(uuidLowerCase);
		}
		Renderer renderer = ConnectedRenderers.getRendererByUUID(uuidLowerCase);
		if (renderer != null) {
			renderer.setUserId(userId);
		}
		// persist the change
		CONFIGURATION.setRenderersUser(GSON.toJson(USERS).replace('"', '|'));
	}

	public static synchronized int getUserId(String uuid) {
		if (StringUtils.isBlank(uuid)) {
			return -1;
		}
		String uuidLowerCase = uuid.toLowerCase();
		if (USERS.containsKey(uuidLowerCase)) {
			return USERS.get(uuidLowerCase);
		} else {
			return -1;
		}
	}

}
