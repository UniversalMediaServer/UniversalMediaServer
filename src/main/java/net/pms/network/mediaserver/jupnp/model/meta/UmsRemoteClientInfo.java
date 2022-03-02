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
package net.pms.network.mediaserver.jupnp.model.meta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.jupnp.model.profile.RemoteClientInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UmsRemoteClientInfo extends RemoteClientInfo {
	private static final Logger LOGGER = LoggerFactory.getLogger(UmsRemoteClientInfo.class);
	// Used to filter out known headers when the renderer is not recognized
	private static final String[] KNOWN_HEADERS = {
		"accept",
		"accept-language",
		"accept-encoding",
		"callback",
		"connection",
		"content-length",
		"content-type",
		"date",
		"host",
		"nt",
		"sid",
		"timeout",
		"user-agent"
	};

	public final RendererConfiguration renderer;

	public UmsRemoteClientInfo(RemoteClientInfo info) {
		super(info.getConnection(), info.getRequestHeaders());
		renderer = toRendererConfiguration(info);
	}

	public static RendererConfiguration toRendererConfiguration(RemoteClientInfo info) {
		RendererConfiguration renderer = null;
		/*
		* don't use that now...
		if (uri.contains(RendererConfiguration.NOTRANSCODE)) {
			renderer = RendererConfiguration.getStreamingConf();
			LOGGER.debug("Forcing streaming.");
		}
		*/

		if (renderer == null) {
			// Attempt 2: try to recognize the renderer by its socket address from previous requests
			renderer = RendererConfiguration.getRendererConfigurationBySocketAddress(info.getRemoteAddress());
		}

		// If the renderer exists but isn't marked as loaded it means it's unrecognized
		// by upnp and we still need to attempt http recognition here.
		if (renderer == null || !renderer.loaded) {
			// Attempt 3: try to recognize the renderer by matching headers
			//let's take only the first header as getRendererConfigurationByHeaders doesn't support multiple values
			Map<String, String> headers = new HashMap<>();
			for (Entry<String, List<String>> header : info.getRequestHeaders().entrySet()) {
				headers.put(header.getKey(), header.getValue().get(0));
			}
			renderer = RendererConfiguration.getRendererConfigurationByHeaders(headers.entrySet(), info.getRemoteAddress());
		}
		// Still no media renderer recognized?
		if (renderer == null) {
			// Attempt 4: Not really an attempt; all other attempts to recognize
			// the renderer have failed. The only option left is to assume the
			// default renderer.
			renderer = RendererConfiguration.resolve(info.getRemoteAddress(), null);
			// If RendererConfiguration.resolve() didn't return the default renderer
			// it means we know via upnp that it's not really a renderer.
			if (renderer != null) {
				LOGGER.debug("Using default media renderer \"{}\"", renderer.getConfName());
				if (info.getRequestUserAgent() != null && !info.getRequestUserAgent().equals("FDSSDP")) {
					// We have found an unknown renderer
					List<String> identifiers = getIdentifiers(info);
					renderer.setIdentifiers(identifiers);
					LOGGER.info(
							"Media renderer was not recognized. Possible identifying HTTP headers:\n{}",
							StringUtils.join(identifiers, "\n")
					);
					PMS.get().setRendererFound(renderer);
				}
			}
		} else if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Recognized media renderer \"{}\"", renderer.getRendererName());
		}
		return renderer;
	}

	private static List<String> getIdentifiers(RemoteClientInfo info) {
		List<String> identifiers = new ArrayList<>();
		identifiers.add("User-Agent: " + info.getRequestUserAgent());
		for (Entry<String, List<String>> header : info.getRequestHeaders().entrySet()) {
			boolean isKnown = false;
			// Try to match known headers.
			String headerName = header.getKey().toLowerCase();
			for (String knownHeaderString : KNOWN_HEADERS) {
				if (headerName.startsWith(knownHeaderString)) {
					isKnown = true;
					break;
				}
			}
			if (!isKnown) {
				// Truly unknown header, therefore interesting.
				for (String value : header.getValue()) {
					identifiers.add(header.getKey() + ": " + value);
				}
			}
		}
		return identifiers;
	}

}
