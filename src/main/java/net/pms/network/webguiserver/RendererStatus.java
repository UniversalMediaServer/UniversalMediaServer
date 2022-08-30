/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
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
package net.pms.network.webguiserver;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.gui.IRendererGuiListener;
import static net.pms.network.webguiserver.servlets.SseApiServlet.broadcastMessage;
import net.pms.util.BasicPlayer;
import net.pms.util.StringUtil;
import net.pms.util.UMSUtils;
import org.apache.commons.lang3.StringUtils;

public class RendererStatus implements IRendererGuiListener {
	private static final HashMap<RendererConfiguration, RendererStatus> RENDERERS = new HashMap<>();
	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final int MAX_BUFFER_SIZE = CONFIGURATION.getMaxMemoryBufferSize();
	private static final AtomicInteger RENDERER_ID = new AtomicInteger(1);
	private final int id;
	private final RendererConfiguration renderer;
	private String name;
	private String icon;
	private String iconOverlays;
	private String playing;
	private String time;
	private int progressPercent;
	private boolean isActive;

	public RendererStatus(RendererConfiguration value) {
		id = RENDERER_ID.getAndIncrement();
		renderer = value;
		name = renderer.getRendererName();
		icon = renderer.getRendererIcon();
		iconOverlays = renderer.getRendererIconOverlays();
		playing = "";
		progressPercent = 0;
		renderer.addGuiListener(this);
		sendRendererAction("renderer_add");
	}

	public JsonObject toJsonObject() {
		JsonObject result = new JsonObject();
		result.addProperty("id", id);
		result.addProperty("name", name);
		result.addProperty("icon", icon);
		result.addProperty("iconOverlays", iconOverlays);
		result.addProperty("playing", playing);
		result.addProperty("time", time);
		result.addProperty("progressPercent", progressPercent);
		result.addProperty("isActive", isActive);
		return result;
	}

	private void sendRendererAction(String action) {
		JsonObject result = toJsonObject();
		result.addProperty("action", action);
		broadcastMessage(result.toString());
	}

	@Override
	public void updateRenderer(RendererConfiguration value) {
		name = renderer.getRendererName();
		icon = renderer.getRendererIcon();
		iconOverlays = renderer.getRendererIconOverlays();
		isActive = renderer.isActive();
		sendRendererAction("renderer_update");
	}

	@Override
	public void setActive(boolean active) {
		isActive = renderer.isActive();
		JsonObject result = toJsonObject();
		broadcastMessage(result.toString());
		sendRendererAction("renderer_update");
	}

	@Override
	public void delete() {
		renderer.removeGuiListener(this);
		synchronized (RENDERERS) {
			if (RENDERERS.containsKey(renderer)) {
				RENDERERS.remove(renderer);
			}
		}
		sendRendererAction("renderer_delete");
	}

	@Override
	public void refreshPlayerState(BasicPlayer.State state) {
		time = ((state.playback == BasicPlayer.STOPPED || StringUtil.isZeroTime(state.position)) ? " " :
			UMSUtils.playedDurationStr(state.position, state.duration));
		progressPercent = (int) (100 * state.buffer / MAX_BUFFER_SIZE);
		playing = (state.playback == BasicPlayer.STOPPED || StringUtils.isBlank(state.name)) ? " " : state.name;
		sendRendererAction("renderer_update");
	}

	public String getIcon() {
		return icon;
	}

	public static void addRenderer(RendererConfiguration renderer) {
		synchronized (RENDERERS) {
			if (!RENDERERS.containsKey(renderer)) {
				RENDERERS.put(renderer, new RendererStatus(renderer));
			}
		}
	}

	public static JsonArray getRenderersAsJsonArray() {
		JsonArray result = new JsonArray();
		synchronized (RENDERERS) {
			for (RendererStatus renderer : RENDERERS.values()) {
				result.add(renderer.toJsonObject());
			}
		}
		return result;
	}

	public static RendererStatus getRenderer(long id) {
		synchronized (RENDERERS) {
			for (RendererStatus renderer : RENDERERS.values()) {
				if (renderer.id == id) {
					return renderer;
				}
			}
		}
		return null;
	}

}
