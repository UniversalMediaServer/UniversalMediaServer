package net.pms.external.audioaddict;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;

public class AudioAddictService implements ConfigurationListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(AudioAddictService.class.getName());

	private static AudioAddictService singleton = new AudioAddictService();

	private AudioAddictServiceConfig conf = new AudioAddictServiceConfig();

	private ConcurrentHashMap<Platform, RadioNetwork> networkSettings = new ConcurrentHashMap<>();


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
		conf.token = ums.getAudioAddictToken();
		conf.preferEuropeanServer = ums.isAudioAddictEuropeanServer();

		networkSettings.clear();

		for (Platform network : Platform.values()) {
			getNetwork(network);
		}
	}

	public List<AudioAddictChannelDto> getChannelFor(Platform platform) {
		RadioNetwork network = getNetwork(platform);
		return network.getChannel();
	}

	private RadioNetwork getNetwork(Platform platform) {
		RadioNetwork network = networkSettings.get(platform);

		if (network == null) {
			network = new RadioNetwork(platform, conf);
			networkSettings.put(platform, network);
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

	@Override
	public void configurationChanged(ConfigurationEvent event) {
		UmsConfiguration ums = PMS.getConfiguration();

		if (conf.user != ums.getAudioAddictUser() || conf.pass != ums.getAudioAddictPassword() || conf.token != ums.getAudioAddictToken() ||
			conf.preferEuropeanServer != ums.isPrettifyFilenames()) {
			LOGGER.info("AudioAddicNetwork : applying new config values ...");
			initService();
		}

	}
}
