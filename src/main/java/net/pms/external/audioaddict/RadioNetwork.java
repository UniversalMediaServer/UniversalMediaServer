package net.pms.external.audioaddict;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.jetty.client.AuthenticationStore;
import org.eclipse.jetty.client.BasicAuthentication;
import org.eclipse.jetty.client.CompletableResponseListener;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.util.Fields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import net.pms.external.audioaddict.mapper.ChannelFilter;
import net.pms.external.audioaddict.mapper.ChannelJson;
import net.pms.external.audioaddict.mapper.Favorite;
import net.pms.external.audioaddict.mapper.Root;

public class RadioNetwork {

	private static final Logger LOGGER = LoggerFactory.getLogger(RadioNetwork.class.getName());

	private final static String EUROPE_SERVER = "http://prem2";
	private final static String FAV = "Favorites";

	private final static ExecutorService EXEC_SERVICE = Executors.newSingleThreadExecutor();
	private static final Pattern API_KEY_PATTERN = Pattern.compile(".*api_key\":\\s*\"([\\w\\d]*)\",");
	private static final Pattern LISTEN_KEY_PATTERN = Pattern.compile(".*listen_key\":\\s*\"([\\w\\d]*)\",");

	private static String apiKey = null;
	private static String listenKey = null;
	private static boolean authenticated = false;

	private HttpClient httpNonBlocking = new HttpClient();
	private HttpClient httpBlocking = new HttpClient();
	private HttpClient httpBatch = new HttpClient();

	private volatile Root networkBatchRoot = null;
	private volatile LinkedList<String> filters = new LinkedList<>();;
	private volatile List<AudioAddictChannelDto> channels = null;
	private volatile List<Integer> favoriteChannelId = null;
	private volatile Channel[] channelUrls = null;

	private ObjectMapper om = null;
	private StreamListQuality quality = StreamListQuality.MP3_320;
	private LinkedHashMap<String, List<Integer>> channelsFilterMap = new LinkedHashMap<>();
	private AudioAddictServiceConfig config = null;

	private Platform network = null;

	public RadioNetwork(Platform network, AudioAddictServiceConfig config) {
		this.config = config;
		this.network = network;
	}

	public void start() {
		AuthenticationStore auth = httpBatch.getAuthenticationStore();
		URI uri = URI.create("http://api.audioaddict.com/v1");
		auth.addAuthenticationResult(new BasicAuthentication.BasicResult(uri, "ephemeron", "dayeiph0ne@pp"));

		httpBatch.setConnectTimeout(30000);
		httpBlocking.setConnectTimeout(10000);
		httpNonBlocking.setConnectTimeout(10000);

		try {
			httpNonBlocking.start();
			httpBatch.start();
			httpBlocking.start();
		} catch (Exception e) {
			LOGGER.error("cannot start http clients", e);
		}

		om = JsonMapper.builder().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build();
		if (apiKey == null) {
			authenticate();
		}

		Runnable r = new Runnable() {
			@Override
			public void run() {
				LOGGER.info("{} : initializing ...", network.name());
				readNetworkBatch();
			}
		};
		EXEC_SERVICE.execute(r);
	}

	public boolean isAuthenticated() {
		return authenticated;
	}

	private void authenticate() {
		String url = "https://api.audioaddict.com/v1/di/members/authenticate";
		Fields fields = new Fields();
		fields.put("username", config.user);
		fields.put("password", config.pass);
		try {
			ContentResponse response = httpNonBlocking.FORM(url, fields);
			String resp = response.getContentAsString();
			if (response.getStatus() != 200) {
				authenticated = false;
				LOGGER.warn("{} : retuned code is {}. Body : ", this.network.displayName, response.getStatus(), resp);
			} else {
				LOGGER.info("successfully authenticated user {}", config.user);
				authenticated = true;
				Matcher m = API_KEY_PATTERN.matcher(resp);
				if (m.find()) {
					apiKey = m.group(1);
				} else {
					LOGGER.warn("api-key not found!");
				}
				m = LISTEN_KEY_PATTERN.matcher(resp);
				if (m.find()) {
					listenKey = m.group(1);
				} else {
					LOGGER.warn("listen-key not found!");
				}
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			LOGGER.error("authentication failed", e);
		}
	}

	public void updateConfig(AudioAddictServiceConfig config) {
		this.config = config;
		start();
	}

	public List<AudioAddictChannelDto> getChannel() {
		return channels;
	}

	public List<AudioAddictChannelDto> getFilteredChannels(String filterName) {
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

	private void updateFilters() {
		LOGGER.info("{} : updating filter ...", this.network.displayName);
		filters = new LinkedList<>();
		for (ChannelFilter filter : networkBatchRoot.channelFilters) {
			this.filters.add(filter.name);
			List<Integer> filterChannelId = new ArrayList<>();
			for (net.pms.external.audioaddict.mapper.Channel c : filter.channels) {
				filterChannelId.add(c.id);
				LOGGER.debug("{} : added channel id {} to filterlist {} ", this.network.displayName, c.id, filter.name);
			}
			channelsFilterMap.put(filter.name, filterChannelId);
		}
		if (favoriteChannelId.size() > 0) {
			LOGGER.info("{} : added favorites filter with {} entries", this.network.displayName, favoriteChannelId.size());
			LinkedHashMap<String, List<Integer>> newMap = (LinkedHashMap<String, List<Integer>>) channelsFilterMap.clone();
			channelsFilterMap.clear();
			channelsFilterMap.put(FAV, favoriteChannelId);
			channelsFilterMap.putAll(newMap);
			filters.addFirst(FAV);
		} else {
			LOGGER.info("{} : no favorites filter available.", this.network.displayName);
		}
	}
	
	public List<String> getFilters() {
		return filters;
	}

	private void updateChannels() {
		LOGGER.debug("{} : updating channels ..." , this.network.displayName);
		channels = new ArrayList<>();
		ChannelFilter allFilter = getChannelByName("all");
		if (allFilter != null) {
			for (net.pms.external.audioaddict.mapper.Channel c : allFilter.channels) {
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
			LOGGER.info("{} : received {} channels.", this.network.displayName, channels.size());
		} else {
			LOGGER.warn("ALL filter unavailable. [TODO] Adjust retrieving logic for channels of this notwork.");
		}
	}

	private ChannelFilter getChannelByName(String filterName) {
		Optional<ChannelFilter> allFilter = networkBatchRoot.channelFilters.stream()
			.filter(filter -> filter.name.equalsIgnoreCase(filterName)).findAny();
		if (allFilter.isEmpty()) {
			return null;
		} else {
			return allFilter.get();
		}
	}

	/**
	 * Converts playlist to url. Extracts one URL from the channel.
	 *
	 * @param channels
	 * @return
	 */
	private Channel[] convertPlsToUrl(ChannelJson[] allChannels) {
		LOGGER.debug("converting PLS to stream url");
		Channel[] preferredChannels = new Channel[allChannels.length];
		int i = 0;
		for (ChannelJson channelJson : allChannels) {
			int errorConter = 0;
			try {
				ContentResponse response = httpBlocking.GET(channelJson.getPlaylist());				
				String url = getBestUrlFromPlaylist(response.getContentAsString());
				preferredChannels[i] = new Channel(channelJson.getId(), channelJson.getKey(), channelJson.getName(), url);
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				LOGGER.error("{} : convert playlist to url failed for item {} : {}", this.network.displayName, i, channelJson.getPlaylist(), e);
				if (errorConter < 2) {
					i--;
					LOGGER.warn("couldn't read playlist. Will try again ... ");
				}
				errorConter++;
			}
			i++;
		}
		LOGGER.debug("{} : received {} streaming url's.", this.network.displayName, preferredChannels.length);
		return preferredChannels;
	}

	/**
	 * Read playlist and choose one audio source (url)
	 *
	 * @param channelJson
	 * @return
	 */
	private String getBestUrlFromPlaylist(String body) {
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

	private void readFavorites() {
		LOGGER.debug("{} : reading favorites ... ", this.network.displayName);
		favoriteChannelId = new ArrayList<>();
		try {
			String url = String.format("https://api.audioaddict.com/v1/%s/members/1/favorites/channels?api_key=%s", network.shortName,
				apiKey);
			Request request = httpNonBlocking.newRequest(url);
			CompletableFuture<ContentResponse> completable = new CompletableResponseListener(request, 5 * 1024 * 1024).send();
			completable.whenComplete((response, failure) -> {
				LOGGER.info("{} : updating favorites", network.displayName);
				try {
					Favorite[] favChannels = om.readValue(response.getContentAsString(), Favorite[].class);
					for (Favorite favorite : favChannels) {
						LOGGER.debug("{} : favorite channel id {} added.", network.displayName, favorite.getChannelId());
						favoriteChannelId.add(favorite.getChannelId());
					}
				} catch (JsonProcessingException e) {
					LOGGER.warn("failed processing favorites", e);
				}
				updateFilters();
			});
			completable.get(10, TimeUnit.SECONDS);

		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			LOGGER.error("{} : readFavorites failed", this.network.displayName, e);
		}
	}

	/*
	 * Get all channels on this network
	 */
	private void readChannels() {
		LOGGER.debug("{} : reading channel list ... ");
		String url = String.format("%s/%s", network.listenUrl, quality.path);
		Request request = httpNonBlocking.newRequest(url);
		CompletableFuture<ContentResponse> completable = new CompletableResponseListener(request, 5 * 1024 * 1024).send();
		completable.whenComplete((response, failure) -> {
			LOGGER.info("{} : analyzing channels ...", network.displayName);
			try {
				ChannelJson[] allChannels = om.readValue(response.getContentAsString(), ChannelJson[].class);
				channelUrls = convertPlsToUrl(allChannels);
			} catch (JsonProcessingException e) {
				LOGGER.error("read channels failed.", e);
				throw new RuntimeException(e);
			}
			updateChannels();
			readFavorites();
		});
	}

	private void readNetworkBatch() {
		String url = String.format("http://api.audioaddict.com/v1/%s/mobile/batch_update?stream_set_key=", network.shortName);
		Request request = httpBatch.newRequest(url);
		CompletableFuture<ContentResponse> completable = new CompletableResponseListener(request, 5 * 1024 * 1024).send();
		completable.whenComplete((response, failure) -> {
			try {
				LOGGER.info("{} : analyzing batch update content ...", network.displayName);
				if (response.getStatus() != 200) {
					LOGGER.warn("{} : retuned code is {}. Body : ", this.network.displayName, response.getStatus(),
						response.getContentAsString());
				} else {
					networkBatchRoot = om.readValue(response.getContentAsString(), Root.class);
					LOGGER.info("{} : read network root data.", this.network.displayName);
					readChannels();
				}
			} catch (JsonProcessingException e) {
				LOGGER.error("reading network failed", e);
			}
		});
	}

	protected void setQuality(StreamListQuality quality) {
		this.quality = quality;
		this.channels = null;
	}
}
