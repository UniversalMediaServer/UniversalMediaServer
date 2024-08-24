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
package net.pms.renderers.devices;

import com.google.gson.JsonObject;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.image.ImageFormat;
import net.pms.network.webguiserver.IEventSourceClient;
import net.pms.renderers.Renderer;
import net.pms.renderers.devices.players.BasicPlayer;
import net.pms.renderers.devices.players.WebGuiPlayer;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;

public class WebGuiRenderer extends Renderer {

	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();

	private static final int CHROME = 1;
	private static final int MSIE = 2;
	private static final int FIREFOX = 3;
	private static final int SAFARI = 4;
	private static final int PS4 = 5;
	private static final int XBOX1 = 6;
	private static final int OPERA = 7;
	private static final int EDGE = 8;
	private static final int CHROMIUM = 9;
	private static final int VIVALDI = 10;

	private final int browser;
	private final String subLang;
	private IEventSourceClient sse;

	public WebGuiRenderer(String uuid, int userId, String userAgent, String subLang) throws ConfigurationException, InterruptedException {
		super(uuid);
		setUserId(userId);
		this.browser = getBrowser(userAgent);
		this.subLang = subLang;
		setFileless(true);
		configuration.setProperty(KEY_MEDIAPARSERV2, true);
		configuration.setProperty(KEY_MEDIAPARSERV2_THUMB, false);
		configuration.setProperty(KEY_TRANSCODE_AUDIO, "MP3");
		configuration.setProperty(KEY_TRANSCODE_VIDEO, "HLS-MPEGTS-H264-AAC,MP4-H264-AAC");
		configuration.setProperty(KEY_HLS_MULTI_VIDEO_QUALITY, true);
		configuration.setProperty(KEY_HLS_VERSION, 6);
		configuration.setProperty(KEY_AUTO_PLAY_TMO, 0);
		configuration.setProperty(KEY_LIMIT_FOLDERS, false);
	}

	@Override
	public boolean isAuthenticated() {
		return true;
	}

	public boolean havePermission(int permission) {
		return account.havePermission(permission);
	}

	public boolean isImageFormatSupported(ImageFormat format) {
		if (format == null) {
			return false;
		}
		if (format == ImageFormat.GIF || format == ImageFormat.JPEG || format == ImageFormat.PNG) {
			return true;
		}
		return switch (format) {
			case BMP -> browser == FIREFOX || browser == CHROME ||
				browser == CHROMIUM || browser == OPERA ||
				browser == MSIE || browser == EDGE || browser == SAFARI;
			case TIFF -> browser == EDGE || browser == CHROMIUM || browser == SAFARI || browser == MSIE;
			case WEBP -> browser == EDGE || browser == FIREFOX || browser == CHROME || browser == CHROMIUM || browser == OPERA;
			default -> false;
		};
	}

	@Override
	public String getRendererName() {
		String username = getUserName();
		if (username != null) {
			return username + "@" + getBrowserName(browser);
		}
		return getBrowserName(browser);
	}

	@Override
	public String getConfName() {
		return getBrowserName(browser);
	}

	public String getUserName() {
		if (account.getUser() != null && account.getUser().getId() != Integer.MAX_VALUE) {
			if (StringUtils.isNotEmpty(account.getUser().getDisplayName())) {
				return account.getUser().getDisplayName();
			} else if (StringUtils.isNotEmpty(account.getUser().getUsername())) {
				return account.getUser().getUsername();
			}
		}
		return null;
	}

	@Override
	public String getRendererIcon() {
		return switch (browser) {
			case CHROME -> "chrome.svg";
			case MSIE -> "internetexplorer.svg";
			case FIREFOX -> "firefox.svg";
			case SAFARI -> "safari.svg";
			case PS4 -> "ps4.png";
			case XBOX1 -> "xbox-one.png";
			case OPERA -> "opera.svg";
			case EDGE -> "edge.svg";
			case CHROMIUM -> "chromium.svg";
			case VIVALDI -> "vivaldi.svg";
			default -> super.getRendererIcon();
		};
	}

	@Override
	public boolean isNoDynPlsFolder() {
		return true;
	}

	@Override
	public boolean nox264() {
		return true;
	}

	@Override
	public int getControls() {
		return CONFIGURATION.isWebPlayerControllable() ? PLAYCONTROL | VOLUMECONTROL : 0;
	}

	@Override
	public BasicPlayer getPlayer() {
		if (player == null) {
			player = new WebGuiPlayer(this);
		}
		return player;
	}

	@Override
	public String getSubLanguage() {
		if (!CONFIGURATION.useWebPlayerSubLang() || StringUtils.isEmpty(subLang)) {
			return super.getSubLanguage();
		}
		return subLang;
	}

	@Override
	public boolean isAllowed() {
		return true;
	}

	@Override
	public void setAllowed(boolean b) {
		//nothing to change
	}

	public void sendMessage(String... args) {
		JsonObject jObject = new JsonObject();
		jObject.addProperty("action", "player");
		if (args.length > 0) {
			jObject.addProperty("request", args[0]);
			if (args.length > 1) {
				jObject.addProperty("arg0", args[1]);
				if (args.length > 2) {
					jObject.addProperty("arg1", args[2]);
				}
			}
		}
		if (sse != null && sse.isOpened()) {
			sse.sendMessage(jObject.toString());
		}
		updateServerSentEventsActive();
	}

	public void addServerSentEvents(IEventSourceClient sse) {
		if (this.sse != null && this.sse.isOpened()) {
			this.sse.close();
		}
		this.sse = sse;
		updateServerSentEventsActive();
	}

	public void updateServerSentEventsActive() {
		boolean sseOpened = this.sse != null && this.sse.isOpened();
		if (sseOpened != isActive()) {
			setActive(sseOpened);
		}
	}

	private static String getBrowserName(int browser) {
		return switch (browser) {
			case CHROME -> "Chrome";
			case MSIE -> "Internet Explorer";
			case FIREFOX -> "Firefox";
			case SAFARI -> "Safari";
			case PS4 -> "Playstation 4";
			case XBOX1 -> "Xbox One";
			case OPERA -> "Opera";
			case EDGE -> "Edge";
			case CHROMIUM -> "Chromium";
			case VIVALDI -> "Vivaldi";
			default -> Messages.getString("WebGuiClient");
		};
	}

	private static int getBrowser(String userAgent) {
		String ua = userAgent.toLowerCase();
		return
			ua.contains("edg")           ? EDGE :
			ua.contains("chrome")        ? CHROME :
			(ua.contains("msie") ||
			ua.contains("trident"))      ? MSIE :
			ua.contains("firefox")       ? FIREFOX :
			ua.contains("safari")        ? SAFARI :
			ua.contains("playstation 4") ? PS4 :
			ua.contains("xbox one")      ? XBOX1 :
			ua.contains("opera")         ? OPERA :
			ua.contains("chromium")      ? CHROMIUM :
			ua.contains("vivaldi")       ? VIVALDI :
			0;
	}

}
