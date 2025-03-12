package net.pms.store.container.audioaddict;

import java.util.List;
import net.pms.external.audioaddict.AudioAddictService;
import net.pms.external.audioaddict.Platform;
import net.pms.renderers.Renderer;
import net.pms.store.StoreContainer;

public class AudioAddictNetwork extends StoreContainer {

	private Platform network = null;

	public AudioAddictNetwork(Renderer renderer, String name, String thumbnailIcon, Platform network) {
		super(renderer, name, thumbnailIcon);
		this.network = network;
		List<String> filterList = AudioAddictService.get().getFiltersForNetwork(this.network);
		for (String filter : filterList) {
			addChild(new AudioAddictNetworkFilter(renderer, network, filter));
		}
	}
}
