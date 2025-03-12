package net.pms.external.audioaddict;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import net.pms.external.audioaddict.mapper.ChannelFilter;
import net.pms.external.audioaddict.mapper.ChannelJson;
import net.pms.external.audioaddict.mapper.Root;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RadioNetwork {

	private static final Logger LOGGER = LoggerFactory.getLogger(RadioNetwork.class.getName());

	private final static String EUROPE_SERVER = "http://prem2";
	private final static String FAV = "Favorites";

	private final static ExecutorService EXEC_SERVICE = Executors.newSingleThreadExecutor();
	private static final Pattern FAV_CHANNEL_SHORT = Pattern.compile(".*/(.*)\\?");

	private volatile Root networkBatchRoot = null;
	private volatile OkHttpClient okClientBatch = new OkHttpClient.Builder()
		.addInterceptor(new BasicAuthInterceptor("ephemeron", "dayeiph0ne@pp")).readTimeout(30, TimeUnit.SECONDS).build();

	private ObjectMapper om = null;
	private OkHttpClient okClient = null;
	private List<AudioAddictChannelDto> channels = null;
	private StreamListQuality quality = StreamListQuality.MP3_320;
	private LinkedList<String> filters = null;
	private LinkedHashMap<String, List<Integer>> channelsFilterMap = new LinkedHashMap<>();
	private AudioAddictServiceConfig config = null;
	private Platform network = null;
	private boolean userPassProvided = false;;

	public RadioNetwork(Platform network, AudioAddictServiceConfig config) {
		this.config = config;
		this.network = network;

		om = JsonMapper.builder().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build();
		initRoot();
	}

	private void initRoot() {
		Runnable r = new Runnable() {

			@Override
			public void run() {
				LOGGER.info("{} : init http client for network ...", network.name());
				initHttpClient(config);
				LOGGER.info("{} : reading batch update for network ...", network.name());
				networkBatchRoot = readNetworkBatch();
				LOGGER.info("{} : reading filters of network ... ", network.name());
				getFilters();
			}
		};
		EXEC_SERVICE.execute(r);
	}

	private void initHttpClient(AudioAddictServiceConfig config) {
		if (StringUtils.isAllBlank(config.pass) && StringUtils.isAllBlank(config.user)) {
			userPassProvided = false;
			okClient = new OkHttpClient.Builder().build();
		} else {
			userPassProvided = true;
			LOGGER.info("{} : channel 'favorites' enabled.", this.network.displayName);
			okClient = new OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS)
				.addInterceptor(new BasicAuthInterceptor(config.user, config.pass)).build();
		}
	}

	public void updateConfig(AudioAddictServiceConfig config) {
		this.config = config;
		initRoot();
	}

	public List<AudioAddictChannelDto> getChannel() {
		checkChannelAvailable();
		return channels;
	}

	public List<AudioAddictChannelDto> getFilteredChannels(String filterName) {
		checkChannelAvailable();
		if (channelsFilterMap.get(filterName) == null) {
			getFilters();
		}

		List<AudioAddictChannelDto> filtered = new ArrayList<>();
		for (AudioAddictChannelDto audioAddictChannelDto : channels) {
			if (channelsFilterMap.get(filterName).contains(audioAddictChannelDto.id)) {
				filtered.add(audioAddictChannelDto);
			}
		}
		return filtered;
	}

	private void checkChannelAvailable() {
		if (channels == null) {
			updateChannels();
		}
	}

	public List<String> getFilters() {
		if (networkBatchRoot == null) {
			// not initialized yet ...
			LOGGER.debug("{} : not initialized yet. please wait ... ");
			return new ArrayList<>();
		}
		checkChannelAvailable();
		if (filters == null) {
			filters = new LinkedList<>();
			List<Integer> favList = null;
			for (ChannelFilter filter : networkBatchRoot.channelFilters) {
				this.filters.add(filter.name);
				List<Integer> filterChannelId = new ArrayList<>();
				for (net.pms.external.audioaddict.mapper.Channel c : filter.channels) {
					filterChannelId.add(c.id);
					LOGGER.debug("{} : added channel id {} to filterlist {} ", this.network.displayName, c.id, filter.name);
				}
				channelsFilterMap.put(filter.name, filterChannelId);
				if ("all".equalsIgnoreCase(filter.name) && this.userPassProvided) {
					favList = getFavorites(filter);
				} else {
					LOGGER.trace("{} - getFilter : User/Pass not provided. Favorites filter is unavailable.", this.network.displayName);
				}
			}
			if (favList != null) {
				LOGGER.info("{} : added favorites filter with {} entries", this.network.displayName, favList.size());
				LinkedHashMap<String, List<Integer>> newMap = (LinkedHashMap<String, List<Integer>>) channelsFilterMap.clone();
				channelsFilterMap.clear();
				channelsFilterMap.put(FAV, favList);
				channelsFilterMap.putAll(newMap);
				filters.addFirst(FAV);
			} else {
				LOGGER.info("{} : no favorites filter available.", this.network.displayName);
			}
		}
		LOGGER.debug("returning {} filter for network {} ", filters.size(), network.displayName);
		return filters;
	}

	private List<Integer> getFavorites(ChannelFilter filter) {
		List<Integer> filterChannelId = new ArrayList<>();

		String url = String.format("%s/public3/favorites.pls?%s", network.listenUrl, config.token);
		String body = responseBody(url);
		LOGGER.debug("response to my favorites request : " + body);
		Matcher m = FAV_CHANNEL_SHORT.matcher(body);
		Set<String> favList = new HashSet<>();
		while (m.find()) {
			String fav = m.group(1);
			fav = fav.substring(network.shortName.length() + 1);
			LOGGER.info("favorite channel : {}", fav);
			favList.add(fav);
		}

		int prefixLength = network.favPrefix.length();
		for (net.pms.external.audioaddict.mapper.Channel c : filter.channels) {
			String mappedChannelName = c.adDfpUnitId.substring(prefixLength);
			if (favList.contains(mappedChannelName)) {
				filterChannelId.add(c.id);
				LOGGER.debug("{} : added channel favorite channel id {} ", this.network.displayName, c.id);
			}
		}
		if (filterChannelId.size() > 0) {
			return filterChannelId;
		}
		return null;
	}

	private void updateChannels() {
		Channel[] channelUrls = getChannels();
		channels = new ArrayList<>();
		for (net.pms.external.audioaddict.mapper.Channel c : getChannelByName("all").channels) {
			AudioAddictChannelDto dto = new AudioAddictChannelDto();
			dto.tracklistServerId = c.tracklistServerId;
			dto.id = c.id;
			dto.key = c.key;
			dto.name = c.name;
			Optional<Channel> s = Arrays.stream(channelUrls).filter(channel -> channel.id() == c.id).findAny();
			if (s.isPresent()) {
				dto.streamUrl = s.get().url();
			} else {
				LOGGER.warn("{} : URL not present for channel {} ", this.network.displayName, c.name);
			}
			dto.albumArt = c.assetUrl;
			dto.descLong = c.descriptionLong;
			dto.descShort = c.descriptionShort;
			channels.add(dto);
		}
		LOGGER.info("{} : updates {} channels.", this.network.displayName, channels.size());
	}

	private ChannelFilter getChannelByName(String filterName) {
		Optional<ChannelFilter> allFilter = networkBatchRoot.channelFilters.stream()
			.filter(filter -> filter.name.equalsIgnoreCase(filterName)).findAny();
		if (allFilter.isEmpty()) {
			throw new RuntimeException("ALL filter not present");
		} else {
			return allFilter.get();
		}
	}

	private Root readNetworkBatch() {
		String url = String.format("http://api.audioaddict.com/v1/%s/mobile/batch_update?stream_set_key=", network.shortName);
		LOGGER.debug("{} : using batch url : {}", this.network.displayName, url);
		String batchResponse = responseBodyBatch(url);
		LOGGER.debug(batchResponse);
		try {
			Root root = om.readValue(batchResponse, Root.class);
			LOGGER.info("{} : network initialized.", this.network.displayName);
			return root;
		} catch (JsonProcessingException e) {
			LOGGER.error("{} : OR exception read batch channel exception", network.name(), e);
		}
		LOGGER.warn("{} : initializing network failed.", network.name());
		return null;
	}

	/*
	 * Get all channels on this network
	 */
	private Channel[] getChannels() {
		LOGGER.debug("get channels ... ");
		String url = String.format("%s/%s", network.listenUrl, quality.path);
		String channelsJson = responseBody(url);
		try {
			ChannelJson[] allChannels = om.readValue(channelsJson, ChannelJson[].class);
			return convertPlsToUrl(allChannels);
		} catch (JsonProcessingException e) {
			LOGGER.error("read channels failed.", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Converts playlist to url Extracts one URL from the channel.
	 *
	 * @param channels
	 * @return
	 */
	private Channel[] convertPlsToUrl(ChannelJson[] allChannels) {
		LOGGER.debug("converting PLS to stream url");
		Channel[] preferredChannels = new Channel[allChannels.length];
		int i = 0;
		for (ChannelJson channelJson : allChannels) {
			String url = String.format("%s?%s", getBestUrlFromPlaylist(channelJson), config.token);
			preferredChannels[i] = new Channel(channelJson.getId(), channelJson.getKey(), channelJson.getName(), url);
			i++;
		}
		return preferredChannels;
	}

	/**
	 * Read playlist and choose one audio source (url)
	 *
	 * @param channelJson
	 * @return
	 */
	private String getBestUrlFromPlaylist(ChannelJson channelJson) {
		String body = responseBody(channelJson.getPlaylist());
		BufferedReader sr = new BufferedReader(new StringReader(body));
		String streamUrl = "";
		String line = "";
		while (line != null) {
			try {
				line = sr.readLine();
				if (line.startsWith("File")) {
					String value = line.split("=")[1];
					if (config.preferEuropeanServer && value.startsWith(EUROPE_SERVER)) {
						return value;
					} else {
						streamUrl = value;
					}
					if (!config.preferEuropeanServer) {
						return streamUrl;
					}
				}
			} catch (IOException e) {
				LOGGER.error("cannot read playlist", e);
			}
		}
		LOGGER.warn("couldn't find European stream server url.");
		return streamUrl;
	}

	private String responseBody(String url) {
		Request request = new Request.Builder().url(url).build();

		Call call = okClient.newCall(request);
		try (Response response = call.execute()) {
			String resp = response.body().string();
			if (response.code() != 200) {
				LOGGER.warn("{} : retuned code is {}. Body : ", this.network.displayName, response.code(), resp);
				return "";
			}

			return resp;
		} catch (Exception e) {
			LOGGER.error("http call failed.", e);
		}
		return "";
	}

	private String responseBodyBatch(String url) {
		Request request = new Request.Builder().url(url).build();

		Call call = okClientBatch.newCall(request);
		try (Response response = call.execute()) {
			String resp = response.body().string();
			if (response.code() != 200) {
				LOGGER.warn("{} : retuned code is {}. Body : ", this.network.displayName, response.code(), resp);
				return "";
			}

			return resp;
		} catch (Exception e) {
			LOGGER.error("http call failed.", e);
		}
		return "";
	}

	protected void setQuality(StreamListQuality quality) {
		this.quality = quality;
		this.channels = null;
	}

}
