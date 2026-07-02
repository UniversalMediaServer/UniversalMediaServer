package net.pms.external.audioaddict;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.jetty.client.AuthenticationStore;
import org.eclipse.jetty.client.BasicAuthentication;
import org.eclipse.jetty.client.CompletableResponseListener;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.client.StringRequestContent;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.Fields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import net.pms.PMS;
import net.pms.external.audioaddict.mapper.ChannelFilter;
import net.pms.external.audioaddict.mapper.ChannelJson;
import net.pms.external.audioaddict.mapper.CurrentlyPlayingJson;
import net.pms.external.audioaddict.mapper.EventJson;
import net.pms.external.audioaddict.mapper.Favorite;
import net.pms.external.audioaddict.mapper.PlaylistJson;
import net.pms.external.audioaddict.mapper.PlaylistPlayResponse;
import net.pms.external.audioaddict.mapper.PlaylistTrackJson;
import net.pms.external.audioaddict.mapper.PlaylistsResponse;
import net.pms.external.audioaddict.mapper.Root;
import net.pms.store.container.audioaddict.INetworkInitialized;

public class RadioNetwork {

	private static final Logger LOGGER = LoggerFactory.getLogger(RadioNetwork.class.getName());

	private final static String EUROPE_SERVER = "http://prem2";
	private final static String FAV = "Favorites";

	private final static ExecutorService EXEC_SERVICE = Executors.newSingleThreadExecutor();
	private static final Pattern API_KEY_PATTERN = Pattern.compile(".*api_key\":\\s*\"([\\w\\d]*)\",");
	private static final Pattern LISTEN_KEY_PATTERN = Pattern.compile(".*listen_key\":\\s*\"([\\w\\d]*)\",");
	private static final Pattern SESSION_KEY_PATTERN = Pattern.compile(".*session_key\":\\s*\"([\\w\\d]*)\",");
	private static final DateTimeFormatter EVENT_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM. HH:mm");
	private static final DateTimeFormatter EPISODE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	// maybe we make it configurable in the future, but for now we limit the number of events to 500
	private static final int EVENTS_LIMIT = 500;

	// The episodes endpoint caps per_page at 100; we page through it until exhausted (bounded).
	private static final int EPISODES_PER_PAGE = 100;
	private static final int EPISODES_MAX_PAGES = 20;

	// Basic auth for "ephemeron:dayeiph0ne@pp" - required by the play
	private static final String BASIC_AUTH_HEADER = "Basic ZXBoZW1lcm9uOmRheWVpcGgwbmVAcHA=";

	private static String apiKey = null;

	private static String listenKey = null;

	// Server-issued per login (in the member object next to api_key/listen_key). Not used in the
	// current api_key streaming mode; kept for a possible future per-stream x-session-key mode.
	private static String sessionKey = null;

	private static boolean authenticated = false;

	private HttpClient httpNonBlocking = new HttpClient();
	private HttpClient httpBlocking = new HttpClient();
	private HttpClient httpBatch = new HttpClient();
	private static int maxRetryBatchCount = 5;
	private static int maxResponseSize = 10 * 1024 * 1024; // 10 MB

	private volatile Root networkBatchRoot = null;
	private volatile LinkedList<String> filters = new LinkedList<>();;
	private volatile List<AudioAddictChannelDto> channels = null;
	private volatile List<Integer> favoriteChannelId = null;
	private volatile Channel[] channelUrls = null;
	private volatile List<INetworkInitialized> networkInitCallbacks = new ArrayList<>();

	private ObjectMapper om = null;
	private StreamListQuality quality = StreamListQuality.MP3_320;
	private LinkedHashMap<String, List<Integer>> channelsFilterMap = new LinkedHashMap<>();
	private AudioAddictServiceConfig config = null;

	private Platform network = null;
	private volatile boolean successInit = false;

	// Cache of the global "currently playing" track per channel id, refreshed at most every TTL.
	private static final long CURRENTLY_PLAYING_TTL_MS = 15000;
	private volatile Map<Integer, String> currentlyPlaying = new HashMap<>();
	private volatile long currentlyPlayingFetchedAt = 0;
	private final AtomicBoolean currentlyPlayingRefreshInFlight = new AtomicBoolean(false);

	// Cache for the events/episodes trees. The signed content URLs stay valid for hours,
	// so a cached tree is safe. TTL is configurable.
	private volatile List<AudioAddictEventDto> cachedEvents = null;
	private volatile long cachedEventsAt = 0;
	private final Map<String, List<AudioAddictTrackDto>> cachedEpisodes = new ConcurrentHashMap<>();
	private final Map<String, Long> cachedEpisodesAt = new ConcurrentHashMap<>();

	private static long treeCacheTtlMs() {
		return PMS.getConfiguration().getAudioAddictTreeCacheTtlMinutes() * 60_000L;
	}

	public RadioNetwork(Platform network, AudioAddictServiceConfig config) {
		this.config = config;
		this.network = network;
	}

	public void addInitCallbackHandler(INetworkInitialized callback) {
		networkInitCallbacks.add(callback);
	}

	public boolean isInitilized() {
		return successInit;
	}

	public void start() {
		LOGGER.debug("{} : start() called ...", network.displayName);
		AuthenticationStore auth = httpBatch.getAuthenticationStore();
		URI uri = URI.create("http://api.audioaddict.com/v1");
		auth.addAuthenticationResult(new BasicAuthentication.BasicResult(uri, "ephemeron", "dayeiph0ne@pp"));

		// The playlist resources are served over HTTPS and require the same basic authentication.
		URI secureUri = URI.create("https://api.audioaddict.com/v1");
		httpBlocking.getAuthenticationStore().addAuthenticationResult(
			new BasicAuthentication.BasicResult(secureUri, "ephemeron", "dayeiph0ne@pp"));

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

		readNetworkConfiguration();
	}

	private void readNetworkConfiguration() {
		Runnable r = new Runnable() {

			@Override
			public void run() {
				LOGGER.info("{} : initializing ...", network.name());
				int i = 0;
				boolean successBatchRead = false;
				while (i <= maxRetryBatchCount && !successBatchRead) {
					try {
						readNetworkBatch();
						successBatchRead = true;
					} catch (Exception e) {
						i++;
						LOGGER.warn("{} : failed to read batch update network data. Waiting 5 seconds. Retry count {} of {}", network.displayName, i, maxRetryBatchCount);
						try {
							Thread.sleep(5000L);
						} catch (InterruptedException e1) {
							LOGGER.error("{} : interupted ... ", network.displayName, e);
							Thread.interrupted();
						}
					}
				}
			};
		};
		LOGGER.debug("{} : creating read thread ...", network.displayName);
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
				extractAuthInfo(resp);
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			LOGGER.error("authentication failed", e);
		}
	}

	protected void extractAuthInfo(String resp) {
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
		m = SESSION_KEY_PATTERN.matcher(resp);
		if (m.find()) {
			sessionKey = m.group(1);
			LOGGER.info("{} : extracted session_key (length {}) - kept for possible future per-stream session use",
				network.displayName, sessionKey.length());
		} else {
			LOGGER.warn("{} : session-key not found in authenticate response!", network.displayName);
		}
	}

	public void updateConfig(AudioAddictServiceConfig config) {
		this.config = config;
		LOGGER.info("{} : configuration changed. Restarting ... ", this.network.displayName);
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
		LOGGER.debug("{} : updating channels ...", this.network.displayName);
		channels = new ArrayList<>();
		Map<Integer, String> genreFilters = buildGenreFilterMap();
		ChannelFilter allFilter = getChannelByName("all");
		if (allFilter != null) {
			for (net.pms.external.audioaddict.mapper.Channel c : allFilter.channels) {
				AudioAddictChannelDto dto = new AudioAddictChannelDto();
				dto.tracklistServerId = c.tracklistServerId;
				dto.id = c.id;
				dto.key = c.key;
				dto.name = c.name;
				dto.genres = joinGenres(c.channelFilterIds, genreFilters);
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

	/**
	 * Builds a map of filter id to filter name for the genre filters only (the "genre"
	 * flag distinguishes real genres from meta filters like "all" or "Favorites").
	 */
	private Map<Integer, String> buildGenreFilterMap() {
		Map<Integer, String> genreFilters = new HashMap<>();
		if (networkBatchRoot != null && networkBatchRoot.channelFilters != null) {
			for (ChannelFilter filter : networkBatchRoot.channelFilters) {
				if (filter.genre) {
					genreFilters.put(filter.id, filter.name);
				}
			}
		}
		return genreFilters;
	}

	/**
	 * Resolves the genre names of an event from the channels of its show.
	 */
	private static String eventGenres(EventJson event, Map<Integer, String> genreFilters) {
		if (event.show == null || event.show.channels == null) {
			return null;
		}
		LinkedHashSet<String> names = new LinkedHashSet<>();
		for (net.pms.external.audioaddict.mapper.Channel c : event.show.channels) {
			if (c.channelFilterIds == null) {
				continue;
			}
			for (int id : c.channelFilterIds) {
				String name = genreFilters.get(id);
				if (name != null) {
					names.add(name);
				}
			}
		}
		return names.isEmpty() ? null : String.join(", ", names);
	}

	private static String joinGenres(int[] filterIds, Map<Integer, String> genreFilters) {
		if (filterIds == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for (int id : filterIds) {
			String name = genreFilters.get(id);
			if (name != null) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append(name);
			}
		}
		return sb.length() > 0 ? sb.toString() : null;
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
				String plsRequest = String.format("%s?listen_key=%s", channelJson.getPlaylist(), listenKey);
				ContentResponse response = httpBlocking.GET(plsRequest);
				String pls = response.getContentAsString();
				String url = getBestUrlFromPlaylist(pls);
				preferredChannels[i] = new Channel(channelJson.getId(), channelJson.getKey(), channelJson.getName(), url);
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				LOGGER.error("{} : convert playlist to url failed for item {} : {}", this.network.displayName, i, channelJson.getPlaylist(),
					e);
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
	protected String getBestUrlFromPlaylist(String body) {
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

	/**
	 * reads favorites if available
	 */
	private void readFavorites() {
		LOGGER.debug("{} : reading favorites ... ", this.network.displayName);
		favoriteChannelId = new ArrayList<>();
		try {
			String url = String.format("https://api.audioaddict.com/v1/%s/members/1/favorites/channels?api_key=%s", network.shortName,
				apiKey);
			Request request = httpNonBlocking.newRequest(url);
			CompletableFuture<ContentResponse> completable = new CompletableResponseListener(request, maxResponseSize).send();
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

				// updateFilters is the last step in initializing the network.
				initFinished();
			});
			completable.get(10, TimeUnit.SECONDS);

		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			LOGGER.error("{} : readFavorites failed", this.network.displayName, e);
		}
	}

	private void initFinished() {
		successInit = true;
		LOGGER.debug("{} : Initialization finished. Calling callback handler ... ", network.displayName);
		for (INetworkInitialized callback : networkInitCallbacks) {
			callback.networkInitialized();
		}
	}

	/*
	 * Get all channels on this network
	 */
	private void readChannels() {
		LOGGER.debug("{} : reading channel list ... ");
		String url = String.format("%s/%s", network.listenUrl, quality.path);
		Request request = httpNonBlocking.newRequest(url);
		CompletableFuture<ContentResponse> completable = new CompletableResponseListener(request, maxResponseSize).send();
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
		CompletableFuture<ContentResponse> completable = new CompletableResponseListener(request, maxResponseSize).send();
		completable.whenComplete((response, failure) -> {
			LOGGER.info("{} : analyzing batch update content ...", network.displayName);
			if (response.getStatus() != 200) {
				LOGGER.warn("{} : retuned code is {}. Body : ", this.network.displayName, response.getStatus(),
					response.getContentAsString());
			} else {
				try {
					networkBatchRoot = om.readValue(response.getContentAsString(), Root.class);
				} catch (JsonProcessingException e) {
					LOGGER.error("{} : network data not parseable.", this.network.displayName);
					throw new RuntimeException(e);
				}
				LOGGER.info("{} : read network root data.", this.network.displayName);
				readChannels();
			}
		});
	}

	protected void setQuality(StreamListQuality quality) {
		this.quality = quality;
		this.channels = null;
	}

	protected static String getApiKey() {
		return apiKey;
	}

	protected static String getListenKey() {
		return listenKey;
	}

	protected static String getSessionKey() {
		return sessionKey;
	}

	/**
	 * Reads the list of curated playlists of this network.
	 *
	 * @return the available playlists, or an empty list when unavailable.
	 */
	public List<AudioAddictPlaylistDto> getPlaylists() {
		List<AudioAddictPlaylistDto> result = new ArrayList<>();
		if (apiKey == null) {
			LOGGER.warn("{} : cannot read playlists, not authenticated.", network.displayName);
			return result;
		}
		String url = String.format("https://api.audioaddict.com/v1/%s/playlists?api_key=%s", network.shortName, apiKey);
		try {
			ContentResponse response = httpBlocking.GET(url);
			PlaylistsResponse resp = om.readValue(response.getContentAsString(), PlaylistsResponse.class);
			if (resp.results != null) {
				for (PlaylistJson p : resp.results) {
					AudioAddictPlaylistDto dto = new AudioAddictPlaylistDto();
					dto.id = p.id;
					dto.name = p.name;
					dto.description = p.description;
					dto.duration = p.duration;
					dto.trackCount = p.trackCount;
					dto.albumArt = imageUrlFromMap(p.images);
					result.add(dto);
				}
			}
			LOGGER.info("{} : received {} playlists.", network.displayName, result.size());
		} catch (InterruptedException | ExecutionException | TimeoutException | JsonProcessingException e) {
			LOGGER.error("{} : failed reading playlists", network.displayName, e);
		}
		return result;
	}

	/**
	 * Reads the ordered tracks of a single playlist. The returned content URLs are signed,
	 * IP bound and expire shortly, so this must be called close to playback time and the
	 * result must not be cached.
	 *
	 * @param playlistId the playlist id.
	 * @return the playlist tracks, or an empty list when unavailable.
	 */
	public List<AudioAddictTrackDto> getPlaylistTracks(int playlistId) {
		List<AudioAddictTrackDto> result = new ArrayList<>();
		if (apiKey == null) {
			LOGGER.warn("{} : cannot read playlist tracks, not authenticated.", network.displayName);
			return result;
		}
		String url = String.format("https://api.audioaddict.com/v1/%s/playlists/%d/play?api_key=%s", network.shortName, playlistId,
			apiKey);
		try {
			ContentResponse response = httpBlocking.POST(url).send();
			PlaylistPlayResponse resp = om.readValue(response.getContentAsString(), PlaylistPlayResponse.class);
			if (resp.tracks != null) {
				for (PlaylistTrackJson t : resp.tracks) {
					String contentUrl = firstContentUrl(t);
					if (contentUrl == null) {
						LOGGER.warn("{} : no playable content for track {}", network.displayName, t.id);
						continue;
					}
					AudioAddictTrackDto dto = new AudioAddictTrackDto();
					dto.id = t.id;
					dto.title = t.displayTitle != null ? t.displayTitle : t.track;
					dto.artist = t.displayArtist;
					dto.length = t.length;
					dto.contentUrl = contentUrl;
					dto.albumArt = normalizeUrl(t.assetUrl);
					result.add(dto);
				}
			}
			LOGGER.info("{} : received {} tracks for playlist {}.", network.displayName, result.size(), playlistId);
		} catch (InterruptedException | ExecutionException | TimeoutException | JsonProcessingException e) {
			LOGGER.error("{} : failed reading tracks for playlist {}", network.displayName, playlistId, e);
		}
		return result;
	}

	/**
	 * Reads the upcoming events (scheduled show episodes), one per show. The recent/older episodes of
	 * each show are not read here; they are fetched lazily. The result is cached for a while.
	 *
	 * @return the upcoming events
	 */
	public synchronized List<AudioAddictEventDto> getUpcomingEvents() {
		long now = System.currentTimeMillis();
		if (cachedEvents != null && now - cachedEventsAt < treeCacheTtlMs()) {
			return cachedEvents;
		}
		List<AudioAddictEventDto> result = fetchUpcomingEvents();
		if (!result.isEmpty()) {
			cachedEvents = result;
			cachedEventsAt = now;
		}
		return result;
	}

	private List<AudioAddictEventDto> fetchUpcomingEvents() {
		List<AudioAddictEventDto> result = new ArrayList<>();
		if (apiKey == null) {
			LOGGER.warn("{} : cannot read events, not authenticated.", network.displayName);
			return result;
		}
		String url = String.format("https://api.audioaddict.com/v1/%s/events/upcoming?limit=%d&api_key=%s", network.shortName,
			EVENTS_LIMIT, apiKey);
		try {
			Request request = httpBlocking.newRequest(url);
			CompletableFuture<ContentResponse> completable = new CompletableResponseListener(request, maxResponseSize).send();
			ContentResponse response = completable.get(30, TimeUnit.SECONDS);
			EventJson[] events = om.readValue(response.getContentAsString(), EventJson[].class);
			Map<Integer, String> genreFilters = buildGenreFilterMap();
			for (EventJson event : events) {
				if (event.show == null || event.show.slug == null) {
					continue;
				}
				AudioAddictEventDto ev = new AudioAddictEventDto();
				ev.showId = event.show.id;
				ev.showSlug = event.show.slug;
				ev.showName = event.show.name;
				ev.ondemandEpisodeCount = event.show.ondemandEpisodeCount;
				ev.startAtMs = parseEpochMs(event.startAt);
				ev.endAtMs = parseEpochMs(event.endAt);
				if (event.tracks != null && !event.tracks.isEmpty()) {
					PlaylistTrackJson t = event.tracks.get(0);
					String contentUrl = firstContentUrl(t);
					if (contentUrl != null) {
						AudioAddictTrackDto dto = new AudioAddictTrackDto();
						dto.id = t.id;
						dto.title = t.displayTitle != null ? t.displayTitle : (event.name != null ? event.name : t.track);
						dto.artist = t.displayArtist;
						dto.length = t.length;
						dto.contentUrl = contentUrl;
						dto.albumArt = normalizeUrl(t.assetUrl);
						dto.startLabel = formatEventStart(event.startAt);
						dto.genres = eventGenres(event, genreFilters);
						dto.album = event.show.name;
						ev.currentEpisode = dto;
						ev.albumArt = dto.albumArt;
					}
				}
				result.add(ev);
			}
			LOGGER.info("{} : received {} upcoming events.", network.displayName, result.size());
		} catch (InterruptedException | ExecutionException | TimeoutException | JsonProcessingException e) {
			LOGGER.error("{} : failed reading events", network.displayName, e);
		}
		return result;
	}

	/**
	 * Reads the recent, playable on-demand episodes of a single show, newest first. The number of
	 * episodes actually available for playback is server side and varies per show.
	 *
	 * @param showSlug the show slug
	 * @return the playable episodes as tracks, or an empty list when unavailable.
	 */
	public synchronized List<AudioAddictTrackDto> getShowEpisodes(String showSlug) {
		if (showSlug == null) {
			return new ArrayList<>();
		}
		long now = System.currentTimeMillis();
		List<AudioAddictTrackDto> cached = cachedEpisodes.get(showSlug);
		Long cachedAt = cachedEpisodesAt.get(showSlug);
		if (cached != null && cachedAt != null && now - cachedAt < treeCacheTtlMs()) {
			return cached;
		}
		List<AudioAddictTrackDto> result = fetchShowEpisodes(showSlug);
		if (!result.isEmpty()) {
			cachedEpisodes.put(showSlug, result);
			cachedEpisodesAt.put(showSlug, now);
		}
		return result;
	}

	private List<AudioAddictTrackDto> fetchShowEpisodes(String showSlug) {
		List<AudioAddictTrackDto> result = new ArrayList<>();
		if (apiKey == null) {
			LOGGER.warn("{} : cannot read episodes, not authenticated.", network.displayName);
			return result;
		}
		try {
			for (int page = 1; page <= EPISODES_MAX_PAGES; page++) {
				String url = String.format("https://api.audioaddict.com/v1/%s/shows/%s/episodes?per_page=%d&page=%d&api_key=%s",
					network.shortName, showSlug, EPISODES_PER_PAGE, page, apiKey);
				ContentResponse response = httpBlocking.GET(url);
				EventJson[] episodes = om.readValue(response.getContentAsString(), EventJson[].class);
				if (episodes.length == 0) {
					break;
				}
				for (EventJson episode : episodes) {
					if (episode.tracks == null || episode.tracks.isEmpty()) {
						continue;
					}
					PlaylistTrackJson t = episode.tracks.get(0);
					String contentUrl = firstContentUrl(t);
					if (contentUrl == null) {
						// Upcoming episode that has not aired yet - no playable content.
						continue;
					}
					AudioAddictTrackDto dto = new AudioAddictTrackDto();
					dto.id = t.id;
					dto.title = t.displayTitle != null ? t.displayTitle : (episode.name != null ? episode.name : t.track);
					dto.artist = t.displayArtist;
					dto.length = t.length;
					dto.contentUrl = contentUrl;
					dto.albumArt = normalizeUrl(t.assetUrl);
					dto.startLabel = formatEpisodeDate(episode.startAt);
					dto.episodeNumber = episode.slug;
					result.add(dto);
				}
				if (episodes.length < EPISODES_PER_PAGE) {
					break;
				}
			}
			LOGGER.info("{} : received {} episodes for show {}.", network.displayName, result.size(), showSlug);
		} catch (InterruptedException | ExecutionException | TimeoutException | JsonProcessingException e) {
			LOGGER.error("{} : failed reading episodes for show {}", network.displayName, showSlug, e);
		}
		return result;
	}

	/**
	 * Parses an event start time (ISO-8601 with offset "2026-06-16T04:00:00-04:00"
	 * and formats it in the local time zone of the host running UMS.
	 */
	private static String formatEventStart(String startAt) {
		if (startAt == null) {
			return null;
		}
		try {
			return OffsetDateTime.parse(startAt).atZoneSameInstant(ZoneId.systemDefault()).format(EVENT_TIME_FORMAT);
		} catch (DateTimeException e) {
			return null;
		}
	}

	/**
	 * Formats an episode air date (ISO-8601 with offset) as a local date "dd.MM.yyyy". Episodes can
	 * span several years, so the year is included.
	 */
	private static String formatEpisodeDate(String startAt) {
		if (startAt == null) {
			return null;
		}
		try {
			return OffsetDateTime.parse(startAt).atZoneSameInstant(ZoneId.systemDefault()).format(EPISODE_DATE_FORMAT);
		} catch (DateTimeException e) {
			return null;
		}
	}

	/**
	 * Used for the "live now" broadcast-window check.
	 */
	private static long parseEpochMs(String isoTime) {
		if (isoTime == null) {
			return 0;
		}
		try {
			return OffsetDateTime.parse(isoTime).toInstant().toEpochMilli();
		} catch (DateTimeException e) {
			return 0;
		}
	}

	/**
	 * Requests the next window of a playlist play session. AudioAddict tracks the playback
	 * progress per member. "markPlayed" calls advance it, so consecutive requests walk the playlist
	 * until "lastTracks" is set "remainingTracks" reaches 0.
	 *
	 * @param playlistId the playlist id.
	 */
	public AudioAddictPlayWindow playPlaylist(int playlistId) {
		AudioAddictPlayWindow window = new AudioAddictPlayWindow();
		if (apiKey == null) {
			LOGGER.warn("{} : cannot play playlist, not authenticated.", network.displayName);
			return window;
		}
		String url = String.format("https://api.audioaddict.com/v1/%s/playlists/%d/play?api_key=%s", network.shortName, playlistId,
			apiKey);
		try {
			ContentResponse response = httpBlocking.newRequest(url)
				.method(HttpMethod.POST)
				.headers(headers -> {
					headers.put("Authorization", BASIC_AUTH_HEADER);
					headers.put("Content-Type", "application/json");
				})
				.send();
			String content = response.getContentAsString();
			if (response.getStatus() != 200 || content == null || !content.startsWith("{")) {
				LOGGER.warn("{} : play playlist {} returned HTTP {} : {}", network.displayName, playlistId, response.getStatus(),
					abbreviate(content));
				return window;
			}
			PlaylistPlayResponse resp = om.readValue(content, PlaylistPlayResponse.class);
			window.lastTracks = resp.lastTracks;
			if (resp.currentProgress != null) {
				window.remainingTracks = resp.currentProgress.remainingTracks;
			}
			if (resp.tracks != null) {
				for (PlaylistTrackJson t : resp.tracks) {
					AudioAddictTrackDto dto = toTrackDto(t);
					if (dto != null) {
						window.tracks.add(dto);
					}
				}
			}
		} catch (InterruptedException | ExecutionException | TimeoutException | JsonProcessingException e) {
			LOGGER.error("{} : play playlist {} failed", network.displayName, playlistId, e);
		}
		return window;
	}

	/**
	 * Marks a track of a playlist as played to advance the member's playback progress for this
	 * playlist.
	 */
	public void markPlayed(int playlistId, long trackId) {
		if (apiKey == null) {
			return;
		}
		String url = String.format("https://api.audioaddict.com/v1/%s/listen_history?api_key=%s", network.shortName, apiKey);
		String body = String.format("{\"track_id\":%d,\"playlist_id\":%d}", trackId, playlistId);
		try {
			httpBlocking.newRequest(url)
				.method(HttpMethod.POST)
				.headers(headers -> {
					headers.put("Authorization", BASIC_AUTH_HEADER);
				})
				.body(new StringRequestContent("application/json", body))
				.send();
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			LOGGER.warn("{} : listen_history for track {} failed", network.displayName, trackId, e);
		}
	}

	/**
	 * @param channelId the channel id.
	 * @return the currently playing track on that channel as "Artist - Title", or NULL when unknown.
	 */
	public String getCurrentTrackTitle(int channelId) {
		triggerCurrentlyPlayingRefreshIfStale();
		return currentlyPlaying.get(channelId);
	}

	private void triggerCurrentlyPlayingRefreshIfStale() {
		if (System.currentTimeMillis() - currentlyPlayingFetchedAt < CURRENTLY_PLAYING_TTL_MS) {
			return;
		}
		// Only one refresh at a time; mark the timestamp up front so failures don't hammer the API.
		if (!currentlyPlayingRefreshInFlight.compareAndSet(false, true)) {
			return;
		}
		currentlyPlayingFetchedAt = System.currentTimeMillis();
		EXEC_SERVICE.submit(() -> {
			try {
				fetchCurrentlyPlaying();
			} catch (Exception e) {
				LOGGER.warn("{} : failed to refresh currently_playing", network.displayName, e);
			} finally {
				currentlyPlayingRefreshInFlight.set(false);
			}
		});
	}

	private void fetchCurrentlyPlaying() throws InterruptedException, ExecutionException, TimeoutException, JsonProcessingException {
		String url = String.format("https://api.audioaddict.com/v1/%s/currently_playing", network.shortName);
		ContentResponse response = httpBlocking.newRequest(url).method(HttpMethod.GET).send();
		String content = response.getContentAsString();
		if (response.getStatus() != 200 || content == null || !content.startsWith("[")) {
			LOGGER.warn("{} : currently_playing returned HTTP {} : {}", network.displayName, response.getStatus(),
				abbreviate(content));
			return;
		}
		CurrentlyPlayingJson[] entries = om.readValue(content, CurrentlyPlayingJson[].class);
		Map<Integer, String> map = new HashMap<>();
		for (CurrentlyPlayingJson e : entries) {
			if (e.channelId == null || e.track == null) {
				continue;
			}
			String title = formatCurrentTitle(e.track);
			if (title != null) {
				map.put(e.channelId, title);
			}
		}
		currentlyPlaying = map;
		LOGGER.debug("{} : refreshed currently_playing for {} channels", network.displayName, map.size());
	}

	private static String formatCurrentTitle(CurrentlyPlayingJson.Track t) {
		String artist = firstNonBlank(t.displayArtist, t.artist);
		String title = firstNonBlank(t.displayTitle, t.title);
		if (artist != null && title != null) {
			return artist + " - " + title;
		}
		if (t.track != null && !t.track.isBlank()) {
			return t.track;
		}
		return title;
	}

	private static String firstNonBlank(String a, String b) {
		if (a != null && !a.isBlank()) {
			return a;
		}
		if (b != null && !b.isBlank()) {
			return b;
		}
		return null;
	}

	private static AudioAddictTrackDto toTrackDto(PlaylistTrackJson t) {
		String contentUrl = firstContentUrl(t);
		if (contentUrl == null) {
			return null;
		}
		AudioAddictTrackDto dto = new AudioAddictTrackDto();
		dto.id = t.id;
		dto.title = t.displayTitle != null ? t.displayTitle : t.track;
		dto.artist = t.displayArtist;
		dto.length = t.length;
		dto.contentUrl = contentUrl;
		dto.albumArt = normalizeUrl(t.assetUrl);
		return dto;
	}

	private static String firstContentUrl(PlaylistTrackJson track) {
		if (track.content == null || track.content.assets == null || track.content.assets.isEmpty()) {
			return null;
		}
		return normalizeUrl(track.content.assets.get(0).url);
	}

	private static String imageUrlFromMap(Map<String, String> images) {
		if (images == null) {
			return null;
		}
		String image = images.get("square");
		if (image == null) {
			image = images.get("default");
		}
		if (image == null) {
			return null;
		}
		int templateStart = image.indexOf('{');
		if (templateStart > -1) {
			image = image.substring(0, templateStart);
		}
		return normalizeUrl(image);
	}

	/**
	 * AudioAddict serves protocol relative URLs (starting with "//"). Prepend the
	 * scheme so the URL can be opened directly.
	 */
	private static String normalizeUrl(String url) {
		if (url == null) {
			return null;
		}
		if (url.startsWith("//")) {
			return "https:" + url;
		}
		return url;
	}

	private static String abbreviate(String s) {
		if (s == null) {
			return "null";
		}
		String trimmed = s.strip();
		return trimmed.length() <= 300 ? trimmed : trimmed.substring(0, 300) + "...";
	}
}
