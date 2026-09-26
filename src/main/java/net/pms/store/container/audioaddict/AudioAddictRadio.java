package net.pms.store.container.audioaddict;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.external.audioaddict.AudioAddictService;
import net.pms.external.audioaddict.Platform;
import net.pms.renderers.Renderer;
import net.pms.store.StoreContainer;

/**
 * Collection of the live radio stations of a network, grouped by their channel filters
 * (e.g. "Favorites" and the genre filters).
 */
public class AudioAddictRadio extends StoreContainer {

	private static final Logger LOGGER = LoggerFactory.getLogger(AudioAddictRadio.class.getName());

	private final Platform network;

	public AudioAddictRadio(Renderer renderer, Platform network) {
		super(renderer, "Radio", network.albumArt);
		this.network = network;
	}

	@Override
	public void discoverChildren() {
		List<String> filterList = AudioAddictService.get().getFiltersForNetwork(network);
		LOGGER.debug("{} : adding {} radio filters.", network.displayName, filterList.size());
		for (String filter : filterList) {
			addChild(new AudioAddictNetworkFilter(renderer, network, filter));
		}
	}
}
