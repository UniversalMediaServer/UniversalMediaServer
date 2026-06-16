package net.pms.store.container.audioaddict;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.external.audioaddict.AudioAddictService;
import net.pms.external.audioaddict.AudioAddictTrackDto;
import net.pms.external.audioaddict.Platform;
import net.pms.media.MediaInfo;
import net.pms.renderers.Renderer;
import net.pms.store.StoreResource;
import net.pms.store.StoreContainer;
import net.pms.store.item.WebAudioStream;

/**
 * Upcoming events (scheduled show episodes) of a network, listed as playable items. The
 * episode content URLs are signed and expire, so the children are (re)resolved on every
 * browse instead of being cached.
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
		List<AudioAddictTrackDto> events = AudioAddictService.get().getUpcomingEvents(network);
		LOGGER.debug("{} : adding {} events.", network.displayName, events.size());
		for (AudioAddictTrackDto event : events) {
			MediaInfo mi = new MediaInfo();
			mi.setMimeType("audio/mpeg");
			mi.setMediaParser("STATIC");
			String title = event.artist != null ? (event.artist + " - " + event.title) : event.title;
			if (event.startLabel != null) {
				title = event.startLabel + "  " + title;
			}
			StoreResource sr = new WebAudioStream(renderer, title, event.contentUrl, event.albumArt);
			sr.setMediaInfo(mi);
			addChild(sr);
		}
	}
}
