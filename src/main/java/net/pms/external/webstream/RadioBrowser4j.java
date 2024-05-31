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
package net.pms.external.webstream;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.sfuhrm.radiobrowser4j.ConnectionParams;
import de.sfuhrm.radiobrowser4j.EndpointDiscovery;
import de.sfuhrm.radiobrowser4j.RadioBrowser;
import de.sfuhrm.radiobrowser4j.Station;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.media.WebStreamMetadata;

public class RadioBrowser4j {

	private static final Logger LOGGER = LoggerFactory.getLogger(RadioBrowser.class.getName());
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();

	private static RadioBrowser radioBrowser;

	/**
	 * This class is not meant to be instantiated.
	 */
	private RadioBrowser4j() {
	}

	private static boolean isRadioBrowserExists() {
		if (radioBrowser == null) {
			Optional<String> endpoint = Optional.empty();
			try {
				endpoint = new EndpointDiscovery("UMS/" + PMS.getVersion()).discover();
			} catch (IOException e) {
				LOGGER.debug("IO problem while endpoint discovery.");
			}
			if (endpoint.isPresent()) {
				ConnectionParams cx = ConnectionParams
						.builder()
						.apiUrl(endpoint.get())
						.userAgent("UMS/" + PMS.getVersion())
						.timeout(5000)
						.build();
				radioBrowser = new RadioBrowser(cx);
			}
		}
		return radioBrowser != null;
	}

	public static WebStreamMetadata getWebStreamMetadata(String url, String radioBrowserUUID) {
		if (!CONFIGURATION.getExternalNetwork() || StringUtils.isAllBlank(radioBrowserUUID)) {
			return null;
		}
		if (isRadioBrowserExists()) {
			try {
				UUID uuid = UUID.fromString(radioBrowserUUID);
				Station station = radioBrowser.getStationByUUID(uuid).orElseThrow();
				Format f = FormatFactory.getAssociatedFormat("." + station.getCodec());
				return new WebStreamMetadata(
						url,
						station.getFavicon(),
						getGenres(station.getTagList()),
						f != null ? f.mimeType() : null,
						null, // sample rate not available over API
						station.getBitrate(),
						null,
						null,
						Format.AUDIO);
			} catch (Exception e) {
				LOGGER.debug("cannot read radio browser metadata for uuid {}", radioBrowserUUID, e);
			}
		}
		return null;
	}

	/**
	 * Splits tag list " / " separator.
	 *
	 * @param tags
	 * @return
	 */
	protected static String getGenres(List<String> tags) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < tags.size(); i++) {
			sb.append(tags.get(i));
			if (i < tags.size() - 1) {
				sb.append(" / ");
			}
		}
		return sb.toString();
	}

}
