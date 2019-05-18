package net.pms.web.resources;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Locale;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

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

	public static String getMsgString(String key, HttpServletRequest req) {
		return getMsgString(key, getFirstSupportedLanguage(HttpHeaders.ACCEPT_LANGUAGE));
	}

	public static String getUserName(SecurityContext t) {
		Principal p = t.getUserPrincipal();
		if (p == null) {
			return "";
		}
		return p.getName();
	}

	public static String getCookie(HttpServletRequest request, String cookieName) {
		if (request.getCookies() != null) {
			for (Cookie cookie : request.getCookies()) {
				if (cookie.getName().equals(cookieName)) {
					return cookie.getValue();
				}
			}
		}
		return null;
	}

	public static InetAddress getAddress(HttpServletRequest request) {
		try {
			return InetAddress.getByAddress(request.getRemoteHost(),
					InetAddress.getByName(request.getRemoteAddr()).getAddress());
		} catch (UnknownHostException e) {
			return null;
		}
	}

	private static IpFilter bumpFilter = null;

	public static boolean bumpAllowed(HttpServletRequest t) {
		if (bumpFilter == null) {
			bumpFilter = new IpFilter(PMS.getConfiguration().getBumpAllowedIps());
		}
		return bumpFilter.allowed(getAddress(t));
	}

	public static WebRender matchRenderer(String user, HttpServletRequest request) {
		int browser = WebRender.getBrowser(request.getHeader("User-agent"));
		String confName = WebRender.getBrowserName(browser);
		RendererConfiguration r = RendererConfiguration.find(confName, getAddress(request));
		return ((r instanceof WebRender) && (StringUtils.isBlank(user) || user.equals(((WebRender) r).getUser())))
				? (WebRender) r
				: null;
	}

	public static Range.Byte parseRange(HttpServletRequest req, long length) {
		if (req == null) {
			return new Range.Byte(0L, length);
		}
		Enumeration<String> r = req.getHeaders("Range");
		if (r == null || !r.hasMoreElements()) { // no range
			return new Range.Byte(0L, length);
		}
		// assume only one
		String range = r.nextElement();
		String[] tmp = range.split("=")[1].split("-");
		long start = Long.parseLong(tmp[0]);
		long end = tmp.length == 1 ? length : Long.parseLong(tmp[1]);
		return new Range.Byte(start, end);
	}

	public static Response logoResponse() throws IOException {
		return Response.ok(
				new StreamingRendererOutput(LooksFrame.class.getResourceAsStream("/resources/images/logo.png"), null))
				.build();
	}
}
