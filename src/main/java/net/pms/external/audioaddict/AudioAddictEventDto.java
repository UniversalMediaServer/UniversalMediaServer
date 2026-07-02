package net.pms.external.audioaddict;

/**
 * An upcoming show event of a network. It carries the identity of the show. Older episodes are not
 * held here; they are fetched lazily per show.
 */
public class AudioAddictEventDto {

	public int showId;
	public String showSlug;
	public String showName;

	/**
	 * Lifetime count of on-demand episodes reported by the API. Not the number of episodes that
	 * are actually available for playback (that can be far smaller), but a useful "has episodes"
	 * indicator.
	 */
	public int ondemandEpisodeCount;

	public String albumArt;

	/**
	 * Broadcast window as epoch milliseconds (0 when unknown). Used to compute the "live now" badge
	 * at item-build time (not at fetch time, which is cached), so it stays accurate across browses.
	 */
	public long startAtMs;
	public long endAtMs;

	/**
	 * The current (most recent, already aired) episode as a directly playable item, or null when
	 * the upcoming broadcast has not aired yet.
	 */
	public AudioAddictTrackDto currentEpisode;

}
