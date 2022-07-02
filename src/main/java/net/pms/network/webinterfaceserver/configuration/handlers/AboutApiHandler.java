/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is free software; you can redistribute it and/or
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
package net.pms.network.webinterfaceserver.configuration.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import net.pms.PMS;
import net.pms.iam.Account;
import net.pms.iam.AuthService;
import net.pms.network.webinterfaceserver.WebInterfaceServerUtil;
import net.pms.network.webinterfaceserver.configuration.ApiHelper;
import net.pms.util.PropertiesUtil;
import net.pms.util.StringUtil;
import net.pms.util.SystemInformation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

public class AboutApiHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(AboutApiHandler.class);
	public static final String BASE_PATH = "/v1/api/about";

	private SystemInfo systemInfo;
	private HardwareAbstractionLayer hardware;

	/**
	 * Handle API calls.
	 *
	 * @param exchange
	 * @throws java.io.IOException
	 */
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try {
			if (WebInterfaceServerUtil.deny(exchange)) {
				exchange.close();
				return;
			}
			if (LOGGER.isTraceEnabled()) {
				WebInterfaceServerUtil.logMessageReceived(exchange, "");
			}
			var api = new ApiHelper(exchange, BASE_PATH);
			try {
				if (api.get("/")) {
					JsonObject jsonResponse = new JsonObject();
					jsonResponse.addProperty("app", PropertiesUtil.getProjectProperties().get("project.name"));
					jsonResponse.addProperty("version", PMS.getVersion());
					String commitId = PropertiesUtil.getProjectProperties().get("git.commit.id");
					jsonResponse.addProperty("commit", commitId.substring(0, 9) + " (" + PropertiesUtil.getProjectProperties().get("git.commit.time") + ")");
					jsonResponse.addProperty("commitUrl", "https://github.com/UniversalMediaServer/UniversalMediaServer/commit/" + commitId);
					jsonResponse.addProperty("website", "https://www.universalmediaserver.com");
					jsonResponse.addProperty("licence", "GNU General Public License version 2");
					jsonResponse.addProperty("licenceUrl", "https://www.gnu.org/licenses/old-licenses/gpl-2.0.txt");
					JsonArray jsonlinks = new JsonArray();
					jsonlinks.add(toJsonObject("Crowdin", PMS.CROWDIN_LINK));
					jsonlinks.add(toJsonObject("FFmpeg", "https://ffmpeg.org/"));
					jsonlinks.add(toJsonObject("MPlayer", "http://www.mplayerhq.hu"));
					jsonlinks.add(toJsonObject("MPlayer, MEncoder and InterFrame builds", "https://www.spirton.com"));
					jsonlinks.add(toJsonObject("MediaInfo", "https://mediaarea.net/en/MediaInfo"));
					jsonlinks.add(toJsonObject("AviSynth MT", "https://forum.doom9.org/showthread.php?t=148782"));
					jsonlinks.add(toJsonObject("DryIcons", "https://dryicons.com/"));
					jsonlinks.add(toJsonObject("Jordan Michael Groll's Icons", "https://www.deviantart.com/jrdng"));
					jsonlinks.add(toJsonObject("SVP", "https://www.svp-team.com/"));
					jsonlinks.add(toJsonObject("OpenSubtitles.org", "https://www.opensubtitles.org/"));
					jsonResponse.add("links", jsonlinks);
					Account account = AuthService.getAccountLoggedIn(api.getAuthorization(), api.getRemoteHostString());
					if (account != null && (account.havePermission("settings_view") || account.havePermission("settings_modify"))) {
						jsonResponse.addProperty("operatingSystem", getOperatingSystem());
						jsonResponse.addProperty("systemMemorySize", getSystemMemorySize());
						jsonResponse.addProperty("jvmMemoryMax", getJavaMemoryMax());
					}
					WebInterfaceServerUtil.respond(exchange, jsonResponse.toString(), 200, "application/json");
				} else {
					LOGGER.trace("AboutApiHandler request not available : {}", api.getEndpoint());
					WebInterfaceServerUtil.respond(exchange, null, 404, "application/json");
				}
			} catch (RuntimeException e) {
				LOGGER.error("RuntimeException in AboutApiHandler: {}", e.getMessage());
				WebInterfaceServerUtil.respond(exchange, "Internal server error", 500, "application/json");
			}
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in AboutApiHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

	private JsonObject toJsonObject(String key, String value) {
		JsonObject result = new JsonObject();
		result.addProperty("key", key);
		result.addProperty("value", value);
		return result;
	}

	private void initSystemInfo() {
		if (systemInfo == null) {
			systemInfo =  new SystemInfo();
		}
	}
	private void initHardwareInfo() {
		initSystemInfo();
		if (hardware == null) {
			hardware =  systemInfo.getHardware();
		}
	}

	private String getOperatingSystem() {
		initSystemInfo();
		OperatingSystem os = systemInfo.getOperatingSystem();
		StringBuilder sb = new StringBuilder();
		if (os != null && StringUtils.isNotBlank(os.toString())) {
			sb.append(os.toString()).append(" ").append(SystemInformation.getOSBitness()).append("-bit");
		} else {
			sb.append(System.getProperty("os.name")).append(" ").append(SystemInformation.getOSBitness()).append("-bit ");
			sb.append(System.getProperty("os.version"));
		}
		return sb.toString();
	}

	private String getSystemMemorySize() {
		initHardwareInfo();
		GlobalMemory memory = hardware.getMemory();
		if (memory != null) {
			return StringUtil.formatBytes(memory.getTotal(), true);
		}
		return "-";
	}

	private String getJavaMemoryMax() {
		long jvmMemory = Runtime.getRuntime().maxMemory();
		if (jvmMemory == Long.MAX_VALUE) {
			return ("-");
		} else {
			return StringUtil.formatBytes(jvmMemory, true);
		}
	}
}
