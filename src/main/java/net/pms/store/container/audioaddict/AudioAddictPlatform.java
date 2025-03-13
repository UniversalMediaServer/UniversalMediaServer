package net.pms.store.container.audioaddict;

import net.pms.external.audioaddict.Platform;
import net.pms.renderers.Renderer;
import net.pms.store.StoreContainer;

/**
 * Collection of all radio networks that belong to the audio addict platform.
 */
public class AudioAddictPlatform extends StoreContainer {

	public AudioAddictPlatform(Renderer renderer, String name) {
		super(renderer, name, "/images/audioaddict/audioAddict.png");

		for (Platform network : Platform.values()) {
			addChild(new AudioAddictNetwork(renderer, network.displayName, network));
		}
	}
}
