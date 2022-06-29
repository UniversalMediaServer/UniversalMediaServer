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
package net.pms.external;

import java.util.ArrayList;
import java.util.List;
import net.pms.external.URLResolver.URLResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class takes care of registering plugins. Plugin jars are loaded,
 * instantiated and stored for later retrieval.
 */
public class ExternalFactory {
	/**
	 * For logging messages.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ExternalFactory.class);

	/**
	 * List of urlresolvers.
	 */
	private static List<URLResolver> urlResolvers = new ArrayList<>();

	private static boolean quoted(String s) {
		return s.startsWith("\"") && s.endsWith("\"");
	}

	private static String quote(String s) {
		if (quoted(s)) {
			return s;
		}
		return "\"" + s + "\"";
	}

	public static URLResult resolveURL(String url) {
		String quotedUrl = quote(url);
		for (URLResolver resolver : urlResolvers) {
			URLResult res = resolver.urlResolve(url);
			if (res != null) {
				if (StringUtils.isEmpty(res.url) || quotedUrl.equals(quote(res.url))) {
					res.url = null;
				}
				if (res.precoder != null && res.precoder.isEmpty()) {
					res.precoder = null;
				}
				if (res.args != null && res.args.isEmpty()) {
					res.args = null;
				}
				if (res.url != null || res.precoder != null || res.args != null) {
					LOGGER.debug(((ExternalListener) resolver).name() + " resolver:" +
						(res.url == null ? "" : " url=" + res.url) +
						(res.precoder == null ? "" : " precoder=" + res.precoder) +
						(res.args == null ? "" : " args=" + res.args));
					return res;
				}
			}
		}
		return null;
	}
}
