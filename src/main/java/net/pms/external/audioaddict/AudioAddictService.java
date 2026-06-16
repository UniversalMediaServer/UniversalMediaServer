package net.pms.external.audioaddict;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.configuration2.event.ConfigurationEvent;
import org.apache.commons.configuration2.event.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;

public class AudioAddictService implements EventListener<ConfigurationEvent> {

	private static final Logger LOGGER = LoggerFactory.getLogger(AudioAddictService.class.getName());

	private static AudioAddictService singleton = new AudioAddictService();

	private AudioAddictServiceConfig conf = new AudioAddictServiceConfig();

	private volatile ConcurrentHashMap<Platform, RadioNetwork> networkSettings = new ConcurrentHashMap<>();

	public static AudioAddictService get() {
		return singleton;
	}

	private AudioAddictService() {
		initService();
	}

	private void initService() {
		UmsConfiguration ums = PMS.getConfiguration();

		conf.user = ums.getAudioAddictUser();
		conf.pass = ums.getAudioAddictPassword();
		conf.preferEuropeanServer = ums.isAudioAddictEuropeanServer();

		networkSettings.clear();
		for (Platform network : Platform.values()) {
			getNetwork(network);
		}
	}

	public RadioNetwork getRadioNetwork(Platform platform) {
		return networkSettings.get(platform);
	}

	public List<AudioAddictChannelDto> getChannelFor(Platform platform) {
		RadioNetwork network = getNetwork(platform);
		return network.getChannel();
	}

	private synchronized RadioNetwork getNetwork(Platform platform) {
		RadioNetwork network = networkSettings.get(platform);

		if (network == null) {
			LOGGER.debug("Initializing network settings for {}", platform.displayName);
			network = new RadioNetwork(platform, conf);
			networkSettings.put(platform, network);
			network.start();
		}
		return network;
	}

	public List<AudioAddictChannelDto> getFilteredChannels(Platform platform, String filter) {
		RadioNetwork network = getNetwork(platform);
		return network.getFilteredChannels(filter);
	}

	public List<String> getFiltersForNetwork(Platform platform) {
		RadioNetwork network = getNetwork(platform);
		return network.getFilters();
	}

	public List<AudioAddictPlaylistDto> getPlaylists(Platform platform) {
		RadioNetwork network = getNetwork(platform);
		return network.getPlaylists();
	}

	public List<AudioAddictTrackDto> getPlaylistTracks(Platform platform, int playlistId) {
		RadioNetwork network = getNetwork(platform);
		return network.getPlaylistTracks(playlistId);
	}

	@Override
	public void onEvent(ConfigurationEvent event) {
		UmsConfiguration ums = PMS.getConfiguration();

		if (conf.user != ums.getAudioAddictUser() || conf.pass != ums.getAudioAddictPassword() ||
			conf.preferEuropeanServer != ums.isAudioAddictEuropeanServer()) {
			LOGGER.info("AudioAddicNetwork : applying new config values ...");
			initService();
		}

	}
}
