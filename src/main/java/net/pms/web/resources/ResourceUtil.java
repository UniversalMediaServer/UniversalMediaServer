package net.pms.web.resources;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.Principal;
import java.util.LinkedHashSet;
import java.util.Locale;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpRequest;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.IpFilter;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.WebRender;
import net.pms.dlna.Range;
import net.pms.newgui.LooksFrame;
import net.pms.util.Languages;

public class ResourceUtil {

	public static LinkedHashSet<String> getLangs(HttpHeaders headers) {
		return getLangs(headers.getHeaderString("Accept-language"));
	}

	public static String getFirstSupportedLanguage(HttpHeaders headers) {
		return getFirstSupportedLanguage(headers.getHeaderString("Accept-language"));
	}

	public static String getFirstSupportedLanguage(String header) {
		LinkedHashSet<String> languages = getLangs(header);
		for (String language : languages) {
			String code = Languages.toLanguageTag(language);
			if (code != null) {
				return code;
			}
		}
		return "";
	}

	public static LinkedHashSet<String> getLangs(String hdr) {
		LinkedHashSet<String> result = new LinkedHashSet<>();
		if (StringUtils.isEmpty(hdr)) {
			return result;
		}

		String[] tmp = hdr.split(",");
		for (String language : tmp) {
			String[] l1 = language.split(";");
			result.add(l1[0]);
		}
		return result;
	}

	public static String read(String file) throws IOException {
		return IOUtils.toString(open(file), "UTF-8");
	}

	public static InputStream open(String file) {
		InputStream in = ResourceUtil.class.getResourceAsStream("/resources/web/" + file);
		if (in != null) {
			return in;
		}
		throw new NotFoundException("Unable to find " + file);
	}

	public static String getMsgString(String key, String lang) {
		if (PMS.getConfiguration().useWebLang()) {
			if (!lang.isEmpty()) {
				return Messages.getString(key, Locale.forLanguageTag(lang));
			}
		}
		return Messages.getString(key);
	}

	public static String getMsgString(String key, HttpRequest req) {
		return getMsgString(key, getFirstSupportedLanguage(HttpHeaders.ACCEPT_LANGUAGE));
	}

	public static String getUserName(SecurityContext t) {
		Principal p = t.getUserPrincipal();
		if (p == null) {
			return "";
		}
		return p.getName();
	}

	public static String getCookie(HttpHeaders headers, String cookieName) {
		String cookieHeader = headers.getHeaderString(HttpHeaders.USER_AGENT);
		if (cookieHeader != null) {
			for (String cookie : cookieHeader.split("")) {
				if (cookie.startsWith(cookieName + "=")) {
					return cookie.substring(cookieName.length() + 1);
				}
			}
		}
		return null;
	}

	public static InetAddress getAddress(ChannelHandlerContext chc) {
		return ((InetSocketAddress) chc.getChannel().getRemoteAddress()).getAddress();
	}

	private static IpFilter bumpFilter = null;

	public static boolean bumpAllowed(ChannelHandlerContext chc) {
		if (bumpFilter == null) {
			bumpFilter = new IpFilter(PMS.getConfiguration().getBumpAllowedIps());
		}
		return bumpFilter.allowed(getAddress(chc));
	}

	public static WebRender matchRenderer(String user, HttpHeaders header, ChannelHandlerContext chc) {
		int browser = WebRender.getBrowser(header.getHeaderString(HttpHeaders.USER_AGENT));
		String confName = WebRender.getBrowserName(browser);
		RendererConfiguration r = RendererConfiguration.find(confName, getAddress(chc));
		return ((r instanceof WebRender) && (StringUtils.isBlank(user) || user.equals(((WebRender) r).getUser()))) ? (WebRender) r : null;
	}

	public static Range.Byte parseRange(HttpHeaders headers, long length) {
		if (headers == null) {
			return new Range.Byte(0L, length);
		}
		return parseRange(headers.getHeaderString("Range"), length);
	}

	public static Range.Byte parseRange(String range, long length) {
		if (range == null) {
			return new Range.Byte(0L, length);
		}
		String[] tmp = range.split("=")[1].split("-");
		long start = Long.parseLong(tmp[0]);
		long end = tmp.length == 1 ? length : Long.parseLong(tmp[1]);
		return new Range.Byte(start, end);
	}

	public static Response logoResponse() throws IOException {
		return Response.ok(new StreamingRendererOutput(LooksFrame.class.getResourceAsStream("/resources/images/logo.png"), null)).build();
	}
}
