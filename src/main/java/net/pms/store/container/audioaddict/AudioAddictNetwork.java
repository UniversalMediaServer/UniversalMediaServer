package net.pms.store.container.audioaddict;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.external.audioaddict.AudioAddictService;
import net.pms.external.audioaddict.Platform;
import net.pms.renderers.Renderer;
import net.pms.store.StoreContainer;

public class AudioAddictNetwork extends StoreContainer implements INetworkInitilized {

	private Platform network = null;
	private static final Logger LOGGER = LoggerFactory.getLogger(AudioAddictNetwork.class.getName());

	public AudioAddictNetwork(Renderer renderer, String name, Platform network) {
		super(renderer, name, network.albumArt);
		this.network = network;
		AudioAddictService.get().getRadioNetwork(network).addInitCallbackHandler(this);
		if (AudioAddictService.get().getRadioNetwork(network).isInitilized()) {
			LOGGER.debug("AudioAddictNetwork store container {} : network is already initilized. ", network.displayName);
			addChilds();
		}
	}

	@Override
	public void networkInitilized() {
		LOGGER.debug("AudioAddictNetwork store container {} : received init signal. Adding childs ... ", network.displayName);
		addChilds();
	}

	private void addChilds() {
		clearChildren();
		LOGGER.debug("AudioAddictNetwork store container {} : adding childs ... ", network.displayName);
		List<String> filterList = AudioAddictService.get().getFiltersForNetwork(this.network);
		for (String filter : filterList) {
			LOGGER.debug("AudioAddictNetwork store container {} : adding child {}", network.displayName, filter);
			addChild(new AudioAddictNetworkFilter(renderer, network, filter));
		}
	}
}
