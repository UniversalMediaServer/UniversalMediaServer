package net.pms.network.mediaserver.jupnp.support.umsservice.impl;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.sfuhrm.radiobrowser4j.AdvancedSearch;
import de.sfuhrm.radiobrowser4j.FieldName;
import de.sfuhrm.radiobrowser4j.Paging;
import de.sfuhrm.radiobrowser4j.RadioBrowser;
import de.sfuhrm.radiobrowser4j.Station;
import net.pms.PMS;
import net.pms.external.JavaHttpClient;
import net.pms.external.radiobrowser.RadioBrowser4j;
import net.pms.network.mediaserver.jupnp.support.umsservice.UmsExtendedServicesException;
import org.apache.commons.lang3.StringUtils;
import org.jupnp.model.types.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Searching radio-browser.info for a station, so a control point can offer a station picker without
 * having to browse a folder holding every station there is.
 */
public class RadioBrowserSearch {

	private static final Logger LOGGER = LoggerFactory.getLogger(RadioBrowserSearch.class.getName());

	private static final ObjectMapper MAPPER = new ObjectMapper();

	/** A dialog shows a page at a time; anything larger only costs bandwidth. */
	private static final int MAX_LIMIT = 100;
	private static final int DEFAULT_LIMIT = 50;

	private RadioBrowserSearch() {
	}

	/**
	 * @return the matching stations as a JSON array, ordered by popularity.
	 */
	public static String searchStations(String name, String countryCode, String language, String tag, int offset, int limit)
			throws UmsExtendedServicesException {
		RadioBrowser client = requireClient();
		AdvancedSearch.AdvancedSearchBuilder search = AdvancedSearch.builder()
				.hideBroken(true)
				.order(FieldName.CLICKCOUNT)
				.reverse(true);
		if (StringUtils.isNotBlank(name)) {
			search.name(name.trim());
		}
		if (StringUtils.isNotBlank(countryCode)) {
			search.countryCode(countryCode.trim());
		}
		if (StringUtils.isNotBlank(language)) {
			search.language(language.trim());
		}
		if (StringUtils.isNotBlank(tag)) {
			search.tag(tag.trim());
		}
		int page = limit < 1 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
		try {
			List<Station> stations = client.listStationsWithAdvancedSearch(
					Paging.at(Math.max(offset, 0), page), search.build());
			return toJson(stations);
		} catch (Exception e) {
			// the endpoint answered badly; the next call looks for another one
			RadioBrowser4j.invalidateEndpoint();
			LOGGER.debug("radio browser search failed", e);
			throw new UmsExtendedServicesException(ErrorCode.ACTION_FAILED, "radio station search failed : " + e.getMessage());
		}
	}

	/**
	 * Values for the filter fields. Read straight from the REST API rather than through the library.
	 *
	 * @param search restricts tags to those matching it; a genre field has to be a type-ahead
	 * because the full list is far too long to offer at once
	 */
	public static String getFilterValues(String kind, String search) throws UmsExtendedServicesException {
		String path = switch (StringUtils.lowerCase(StringUtils.trimToEmpty(kind))) {
			case "countries" -> "json/countries";
			case "languages" -> "json/languages";
			case "tags" -> StringUtils.isBlank(search) ? "json/tags" :
					"json/tags/" + URLEncoder.encode(search.trim(), StandardCharsets.UTF_8);
			default -> throw new UmsExtendedServicesException(ErrorCode.ARGUMENT_VALUE_INVALID,
					"unknown filter kind : " + kind);
		};
		JsonNode values = readApi(path);
		List<Map<String, Object>> result = new ArrayList<>();
		for (JsonNode node : values) {
			String value = node.path("name").asText("");
			if (StringUtils.isBlank(value)) {
				continue;
			}
			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("value", value);
			entry.put("stationCount", node.path("stationcount").asInt(0));
			// the search only takes the code, while a user picks the name
			String code = node.path("iso_3166_1").asText(null);
			if (StringUtils.isNotBlank(code)) {
				entry.put("code", code);
			}
			result.add(entry);
		}
		result.sort((a, b) -> Integer.compare((Integer) b.get("stationCount"), (Integer) a.get("stationCount")));
		return writeValueAsString(result);
	}

	private static JsonNode readApi(String path) throws UmsExtendedServicesException {
		String base = RadioBrowser4j.getEndpoint();
		if (base == null) {
			throw new UmsExtendedServicesException(ErrorCode.ACTION_FAILED, "no radio-browser.info server is reachable");
		}
		String uri = base.endsWith("/") ? base + path : base + "/" + path;
		try {
			return MAPPER.readTree(JavaHttpClient.getBytes(uri));
		} catch (IOException e) {
			RadioBrowser4j.invalidateEndpoint();
			LOGGER.debug("cannot read {} : {}", uri, e.getMessage());
			throw new UmsExtendedServicesException(ErrorCode.ACTION_FAILED, "cannot read filter values : " + e.getMessage());
		}
	}

	/**
	 * Reads one station, so the caller only has to pass its uuid around.
	 */
	public static Station getStation(String stationUuid) throws UmsExtendedServicesException {
		if (StringUtils.isBlank(stationUuid)) {
			throw new UmsExtendedServicesException(ErrorCode.ARGUMENT_VALUE_INVALID, "no station uuid given");
		}
		RadioBrowser client = requireClient();
		try {
			return client.getStationByUUID(java.util.UUID.fromString(stationUuid.trim()))
					.orElseThrow(() -> new UmsExtendedServicesException(ErrorCode.ARGUMENT_VALUE_INVALID,
							"no radio station with uuid " + stationUuid));
		} catch (UmsExtendedServicesException e) {
			throw e;
		} catch (IllegalArgumentException e) {
			throw new UmsExtendedServicesException(ErrorCode.ARGUMENT_VALUE_INVALID, "not a uuid : " + stationUuid);
		} catch (Exception e) {
			RadioBrowser4j.invalidateEndpoint();
			LOGGER.debug("cannot read radio station {}", stationUuid, e);
			throw new UmsExtendedServicesException(ErrorCode.ACTION_FAILED, "cannot read radio station : " + e.getMessage());
		}
	}

	/**
	 * The url radio-browser already resolved, so UMS does not have to follow a playlist redirect
	 * before it can play the station. Falls back to the registered url.
	 */
	public static String getStreamUrl(Station station) {
		return StringUtils.isNotBlank(station.getUrlResolved()) ? station.getUrlResolved() : station.getUrl();
	}

	private static RadioBrowser requireClient() throws UmsExtendedServicesException {
		if (!PMS.getConfiguration().getExternalNetwork()) {
			throw new UmsExtendedServicesException(ErrorCode.ACTION_FAILED, "external network access is disabled");
		}
		RadioBrowser client = RadioBrowser4j.getClient();
		if (client == null) {
			throw new UmsExtendedServicesException(ErrorCode.ACTION_FAILED, "no radio-browser.info server is reachable");
		}
		return client;
	}

	private static String toJson(List<Station> stations) throws UmsExtendedServicesException {
		List<Map<String, Object>> result = new ArrayList<>();
		for (Station station : stations) {
			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("uuid", station.getStationUUID() != null ? station.getStationUUID().toString() : "");
			entry.put("name", StringUtils.trimToEmpty(station.getName()));
			entry.put("url", StringUtils.trimToEmpty(getStreamUrl(station)));
			entry.put("favicon", StringUtils.trimToEmpty(station.getFavicon()));
			entry.put("countryCode", StringUtils.trimToEmpty(station.getCountryCode()));
			entry.put("language", StringUtils.trimToEmpty(station.getLanguage()));
			entry.put("tags", StringUtils.trimToEmpty(station.getTags()));
			entry.put("codec", StringUtils.trimToEmpty(station.getCodec()));
			entry.put("bitrate", station.getBitrate() != null ? station.getBitrate() : 0);
			entry.put("votes", station.getVotes() != null ? station.getVotes() : 0);
			result.add(entry);
		}
		return writeValueAsString(result);
	}

	private static String writeValueAsString(Object value) throws UmsExtendedServicesException {
		try {
			return MAPPER.writeValueAsString(value);
		} catch (JsonProcessingException e) {
			throw new UmsExtendedServicesException(ErrorCode.ACTION_FAILED, "cannot serialize the result");
		}
	}
}
