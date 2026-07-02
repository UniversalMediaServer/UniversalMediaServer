package net.pms.store.container.audioaddict;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.external.audioaddict.AudioAddictService;
import net.pms.external.audioaddict.AudioAddictTrackDto;
import net.pms.external.audioaddict.Platform;
import net.pms.renderers.Renderer;
import net.pms.store.StoreContainer;

/**
 * The recent, older on-demand episodes of a single show, shown as a container next to the show's
 * current episode item. When there are more episodes than the configured page size they are split
 * into several sub containers. The signed content URLs expire, so the episodes are refetched on every browse.
 */
public class AudioAddictShowEpisodes extends StoreContainer {

	private static final Logger LOGGER = LoggerFactory.getLogger(AudioAddictShowEpisodes.class.getName());

	private final Platform network;
	private final String showSlug;
	private final String thumb;

	public AudioAddictShowEpisodes(Renderer renderer, Platform network, String showSlug, String showName, String thumb) {
		super(renderer, showName != null ? showName : showSlug, thumb);
		this.network = network;
		this.showSlug = showSlug;
		this.thumb = thumb;
	}

	@Override
	public void discoverChildren() {
		addEpisodes();
	}

	@Override
	public boolean isRefreshNeeded() {
		// Content URLs are time limited, so always refetch fresh ones on browse.
		return true;
	}

	@Override
	public void doRefreshChildren() {
		clearChildren();
		addEpisodes();
	}

	private void addEpisodes() {
		List<AudioAddictTrackDto> episodes = AudioAddictService.get().getShowEpisodes(network, showSlug);
		int pageSize = renderer.getUmsConfiguration().getAudioAddictEpisodesPerContainer();
		LOGGER.debug("{} : show {} has {} episodes (page size {}).", network.displayName, showSlug, episodes.size(), pageSize);
		if (episodes.size() <= pageSize) {
			for (AudioAddictTrackDto episode : episodes) {
				addChild(AudioAddictFileStream.from(renderer, episode));
			}
		} else {
			for (int start = 0; start < episodes.size(); start += pageSize) {
				int end = Math.min(start + pageSize, episodes.size());
				List<AudioAddictTrackDto> chunk = episodes.subList(start, end);
				addChild(new AudioAddictEpisodeChunk(renderer, chunkLabel(chunk), thumb, chunk));
			}
		}
	}

	/**
	 * Builds a label describing the episode/date range covered by a chunk (episodes are ordered
	 * newest first), e.g. "081 - 062  (26.05.2026 - 13.12.2022)".
	 */
	private static String chunkLabel(List<AudioAddictTrackDto> chunk) {
		AudioAddictTrackDto newest = chunk.get(0);
		AudioAddictTrackDto oldest = chunk.get(chunk.size() - 1);
		StringBuilder sb = new StringBuilder();
		if (newest.episodeNumber != null && oldest.episodeNumber != null) {
			if (newest.episodeNumber.equals(oldest.episodeNumber)) {
				sb.append(newest.episodeNumber);
			} else {
				sb.append(newest.episodeNumber).append(" - ").append(oldest.episodeNumber);
			}
		}
		String newestDate = newest.startLabel;
		String oldestDate = oldest.startLabel;
		if (newestDate != null && oldestDate != null) {
			String range = newestDate.equals(oldestDate) ? newestDate : (newestDate + " - " + oldestDate);
			if (sb.length() > 0) {
				sb.append("  (").append(range).append(")");
			} else {
				sb.append(range);
			}
		}
		return sb.length() > 0 ? sb.toString() : "Episodes";
	}
}
