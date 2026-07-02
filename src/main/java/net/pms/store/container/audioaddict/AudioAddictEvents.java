package net.pms.store.container.audioaddict;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.external.audioaddict.AudioAddictEventDto;
import net.pms.external.audioaddict.AudioAddictService;
import net.pms.external.audioaddict.Platform;
import net.pms.renderers.Renderer;
import net.pms.store.StoreContainer;

/**
 * Upcoming events (scheduled show episodes) of a network. Each event contributes the current,
 * already aired episode as a directly playable item (when available) plus a container holding the
 * show's older, on-demand episodes ({@link AudioAddictShowEpisodes}). The episode content URLs are
 * signed and expire, so the children are (re)resolved on every browse instead of being cached.
 */
public class AudioAddictEvents extends StoreContainer {

	private static final Logger LOGGER = LoggerFactory.getLogger(AudioAddictEvents.class.getName());

	private final Platform network;

	public AudioAddictEvents(Renderer renderer, Platform network) {
		super(renderer, "Events", network.albumArt);
		this.network = network;
	}

	@Override
	public void discoverChildren() {
		addEvents();
	}

	@Override
	public boolean isRefreshNeeded() {
		// Content URLs are time limited, so always refetch fresh ones on browse.
		return true;
	}

	@Override
	public void doRefreshChildren() {
		clearChildren();
		addEvents();
	}

	private void addEvents() {
		List<AudioAddictEventDto> events = AudioAddictService.get().getUpcomingEvents(network);
		LOGGER.debug("{} : adding {} events.", network.displayName, events.size());
		for (AudioAddictEventDto event : events) {
			// The current (already aired) episode is directly playable at the top level.
			if (event.currentEpisode != null) {
				addChild(AudioAddictFileStream.from(renderer, event.currentEpisode));
			}
			// Older episodes of the show go into their own (lazily filled) container.
			if (event.showSlug != null && event.ondemandEpisodeCount > 0) {
				String thumb = event.albumArt != null ? event.albumArt : network.albumArt;
				addChild(new AudioAddictShowEpisodes(renderer, network, event.showSlug, event.showName, thumb));
			}
		}
	}
}
