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
package net.pms.network.webguiserver;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.gui.IRendererGuiListener;
import net.pms.renderers.Renderer;
import net.pms.renderers.devices.players.PlayerState;
import net.pms.store.StoreItem;
import net.pms.store.StoreResource;
import net.pms.util.StringUtil;
import net.pms.util.UMSUtils;
import org.apache.commons.lang3.StringUtils;

public class RendererItem implements IRendererGuiListener {
	private static final HashMap<Renderer, RendererItem> RENDERERS = new HashMap<>();
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final int MAX_BUFFER_SIZE = CONFIGURATION.getMaxMemoryBufferSize();
	private static final AtomicInteger RENDERER_ID = new AtomicInteger(1);
	private static final Gson GSON = new Gson();
	private static final String ACTION_ADD = "renderer_add";
	private static final String ACTION_DELETE = "renderer_delete";
	private static final String ACTION_UPDATE = "renderer_update";

	private final int id;
	private final Renderer renderer;

	private String name;
	private String address;
	private String uuid;
	private String icon;
	private String iconOverlays;
	private String playing;
	private String time;
	private int progressPercent;
	private boolean isActive;
	private boolean isAllowed;
	private boolean isAuthenticated;
	private int userId;
	private int controls;
	private PlayerState state;

	public RendererItem(Renderer value) {
		id = RENDERER_ID.getAndIncrement();
		renderer = value;
		init();
	}

	@Override
	public void updateRenderer(Renderer value) {
		//we can use renderer itself as it's a pointer to real renderer object
		updateRendererValues();
		sendRendererAction(ACTION_UPDATE);
	}

	@Override
	public void setActive(boolean active) {
		//we can use renderer itself as it's a pointer to real renderer object
		if (isActive != renderer.isActive()) {
			isActive = renderer.isActive();
			sendRendererAction(ACTION_UPDATE);
		}
	}

	@Override
	public void setAllowed(boolean allowed) {
		//we can use renderer itself as it's a pointer to real renderer object
		if (isAllowed != renderer.isAllowed()) {
			isAllowed = renderer.isAllowed();
			sendRendererAction(ACTION_UPDATE);
		}
	}

	@Override
	public void setUserId(int value) {
		//we can use renderer itself as it's a pointer to real renderer object
		if (userId != renderer.getUserId()) {
			userId = renderer.getUserId();
			sendRendererAction(ACTION_UPDATE);
		}
	}

	@Override
	public void delete() {
		renderer.removeGuiListener(this);
		synchronized (RENDERERS) {
			if (RENDERERS.containsKey(renderer)) {
				RENDERERS.remove(renderer);
			}
		}
		sendRendererAction(ACTION_DELETE);
	}

	@Override
	public void refreshPlayerState(PlayerState state) {
		this.state = state;
		time = ((state.isStopped()) || StringUtil.isZeroTime(state.getPosition())) ? " " :
			UMSUtils.playedDurationStr(state.getPosition(), state.getDuration());
		progressPercent = (int) (100 * state.getBuffer() / MAX_BUFFER_SIZE);
		playing = (state.isStopped() || StringUtils.isBlank(state.getName())) ? " " : state.getName();
		sendRendererAction(ACTION_UPDATE);
	}

	public String getIcon() {
		return icon;
	}

	public JsonObject getInfos() {
		JsonObject result = new JsonObject();
		result.addProperty("title", name);
		result.addProperty("isUpnp", renderer.isUpnp());
		JsonArray detailsArray = new JsonArray();
		JsonObject entryObject = new JsonObject();
		entryObject.addProperty("key", "confname");
		entryObject.addProperty("value", renderer.getConfName());
		detailsArray.add(entryObject);
		if (!renderer.isFileless() && renderer.getFile() != null) {
			entryObject = new JsonObject();
			entryObject.addProperty("key", "conffile");
			entryObject.addProperty("value", renderer.getFile().getName());
			detailsArray.add(entryObject);
		}
		if (renderer.isUpnp()) {
			Map<String, String> details = renderer.getDetails();
			for (Entry<String, String> entry : details.entrySet()) {
				entryObject = new JsonObject();
				entryObject.addProperty("key", entry.getKey());
				entryObject.addProperty("value", entry.getValue());
				detailsArray.add(entryObject);
			}
			entryObject = new JsonObject();
			entryObject.addProperty("key", "Services");
			entryObject.addProperty("value", StringUtils.join(renderer.getUpnpServices(), ", "));
			detailsArray.add(entryObject);
		} else {
			entryObject = new JsonObject();
			entryObject.addProperty("key", "i18n@Name");
			entryObject.addProperty("value", name);
			detailsArray.add(entryObject);
			result.addProperty("i18n@Name", name);
			if (!"".equals(address)) {
				entryObject = new JsonObject();
				entryObject.addProperty("key", "i18n@Address");
				entryObject.addProperty("value", address);
				detailsArray.add(entryObject);
			}
			if (renderer.getIdentifiers() != null) {
				for (String identifier : renderer.getIdentifiers()) {
					entryObject = new JsonObject();
					entryObject.addProperty("key", "");
					entryObject.addProperty("value", identifier);
					detailsArray.add(entryObject);
				}
			}
		}
		result.add("details", detailsArray);
		return result;
	}

	private void init() {
		updateRendererValues();
		playing = "";
		progressPercent = 0;
		renderer.addGuiListener(this);
		sendRendererAction(ACTION_ADD);
	}

	private void updateRendererValues() {
		name = renderer.getRendererName();
		address = (renderer.getAddress() != null) ? renderer.getAddress().getHostAddress() : "";
		uuid = (renderer.getUUID() != null) ? renderer.getUUID() : "";
		icon = renderer.getRendererIcon();
		iconOverlays = renderer.getRendererIconOverlays();
		isActive = renderer.isActive();
		isAllowed = renderer.isAllowed();
		isAuthenticated = renderer.isAuthenticated();
		userId = renderer.getUserId();
		controls = renderer.getControls();
		state = renderer.getPlayer().getState();
	}

	private void playerBack() {
		renderer.getPlayer().rewind();
	}

	private void playerPrev() {
		renderer.getPlayer().prev();
	}

	private void playerPlay() {
		renderer.getPlayer().play();
	}

	private void playerPause() {
		renderer.getPlayer().pause();
	}

	private void playerStop() {
		renderer.getPlayer().stop();
	}

	private void playerNext() {
		renderer.getPlayer().next();
	}

	private void playerForward() {
		renderer.getPlayer().forward();
	}

	private void playerMute() {
		renderer.getPlayer().mute();
	}

	private void playerSetVolume(int volume) {
		renderer.getPlayer().setVolume(volume);
	}

	private void playerSetMediaId(String id) {
		List<StoreResource> resources = renderer.getMediaStore().getResources(id, false);
		if (!resources.isEmpty() && resources.get(0) instanceof StoreItem item) {
			renderer.getPlayer().setURI(item.getMediaURL("", true), null);
		}
	}

	private JsonObject toJsonObject() {
		JsonObject result = new JsonObject();
		result.addProperty("id", id);
		result.addProperty("name", name);
		result.addProperty("address", address);
		result.addProperty("uuid", uuid);
		result.addProperty("icon", icon);
		result.addProperty("iconOverlays", iconOverlays);
		result.addProperty("playing", playing);
		result.addProperty("time", time);
		result.addProperty("progressPercent", progressPercent);
		result.addProperty("isActive", isActive);
		result.addProperty("isAllowed", isAllowed);
		result.addProperty("isAuthenticated", isAuthenticated);
		result.addProperty("userId", userId);
		result.addProperty("controls", controls);
		result.add("state", GSON.toJsonTree(state));
		return result;
	}

	private void sendRendererAction(String action) {
		if (EventSourceServer.hasHomeServerSentEvents()) {
			JsonObject result = toJsonObject();
			result.addProperty("action", action);
			EventSourceServer.broadcastHomeMessage(result.toString());
		}
	}

	public static boolean remoteControlRenderer(JsonObject post) {
		if (post != null && post.has("id") && post.has("action")) {
			int rId = post.get("id").getAsInt();
			RendererItem renderer = RendererItem.getRenderer(rId);
			if (renderer == null) {
				return false;
			}
			String action = post.get("action").getAsString();
			switch (action) {
				case "back" -> {
					renderer.playerBack();
					return true;
				}
				case "prev" -> {
					renderer.playerPrev();
					return true;
				}
				case "play" -> {
					renderer.playerPlay();
					return true;
				}
				case "pause" -> {
					renderer.playerPause();
					return true;
				}
				case "stop" -> {
					renderer.playerStop();
					return true;
				}
				case "next" -> {
					renderer.playerNext();
					return true;
				}
				case "forward" -> {
					renderer.playerForward();
					return true;
				}
				case "volume" -> {
					int volume = post.get("value").getAsInt();
					renderer.playerSetVolume(volume);
					return true;
				}
				case "mute" -> {
					renderer.playerMute();
					return true;
				}
				case "mediaid" -> {
					String id = post.get("value").getAsString();
					renderer.playerSetMediaId(id);
					return true;
				}
				default -> {
					return false;
				}
			}
		}
		return false;
	}

	public static JsonObject getRemoteControlBrowse(JsonObject post) {
		if (post != null && post.has("id") && post.has("media")) {
			int rId = post.get("id").getAsInt();
			RendererItem renderer = RendererItem.getRenderer(rId);
			if (renderer == null) {
				return null;
			}
			String media = post.get("media").getAsString();
			JsonObject result = new JsonObject();
			JsonArray parents = new JsonArray();
			JsonArray childrens = new JsonArray();
			List<StoreResource> resources = renderer.renderer.getMediaStore().getResources(media, true);
			if (!resources.isEmpty()) {
				StoreResource parentFromResources = resources.get(0).getParent();
				if (parentFromResources != null && parentFromResources.isFolder() && !"0".equals(parentFromResources.getResourceId())) {
					JsonObject parent = new JsonObject();
					parent.addProperty("value", parentFromResources.getResourceId());
					parent.addProperty("label", parentFromResources.getName());
					parents.add(parent);
					parentFromResources = parentFromResources.getParent();
					if (parentFromResources != null && parentFromResources.isFolder() && !"0".equals(parentFromResources.getResourceId())) {
						parent = new JsonObject();
						parent.addProperty("value", parentFromResources.getResourceId());
						parent.addProperty("label", parentFromResources.getName());
						parents.add(parent);
					}
				}
				for (StoreResource resource : resources) {
					if (resource == null) {
						continue;
					}
					JsonObject children = new JsonObject();
					children.addProperty("value", resource.getResourceId());
					children.addProperty("label", resource.getName());
					children.addProperty("browsable", resource.isFolder());
					childrens.add(children);
				}
			}
			result.add("parents", parents);
			result.add("childrens", childrens);
			return result;
		}
		return null;
	}

	public static void addRenderer(Renderer renderer) {
		synchronized (RENDERERS) {
			if (!RENDERERS.containsKey(renderer)) {
				RENDERERS.put(renderer, new RendererItem(renderer));
			}
		}
	}

	public static JsonArray getRenderersAsJsonArray() {
		JsonArray result = new JsonArray();
		synchronized (RENDERERS) {
			for (RendererItem renderer : RENDERERS.values()) {
				result.add(renderer.toJsonObject());
			}
		}
		return result;
	}

	public static RendererItem getRenderer(long id) {
		synchronized (RENDERERS) {
			for (RendererItem renderer : RENDERERS.values()) {
				if (renderer.id == id) {
					return renderer;
				}
			}
		}
		return null;
	}

}
