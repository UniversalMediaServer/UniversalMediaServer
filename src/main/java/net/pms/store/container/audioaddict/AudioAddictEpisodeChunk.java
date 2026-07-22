package net.pms.store.container.audioaddict;

import java.util.ArrayList;
import java.util.List;
import net.pms.external.audioaddict.AudioAddictTrackDto;
import net.pms.renderers.Renderer;
import net.pms.store.StoreContainer;

/**
 * A page of a show's older on-demand episodes. It simply lists the episodes of its slice as playable items.
 * The slice is resolved fresh whenever the parent show container is browsed, so it carries currently valid signed content URLs.
 */
public class AudioAddictEpisodeChunk extends StoreContainer {

	private final List<AudioAddictTrackDto> episodes;
	private volatile boolean populated = false;

	public AudioAddictEpisodeChunk(Renderer renderer, String label, String thumb, List<AudioAddictTrackDto> episodes) {
		super(renderer, label, thumb);
		this.episodes = new ArrayList<>(episodes);
	}

	@Override
	public synchronized void discoverChildren() {
		if (populated) {
			return;
		}
		for (AudioAddictTrackDto episode : episodes) {
			addChild(AudioAddictFileStream.from(renderer, episode));
		}
		populated = true;
	}
}
