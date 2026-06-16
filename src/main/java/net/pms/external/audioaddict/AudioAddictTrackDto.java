package net.pms.external.audioaddict;

public class AudioAddictTrackDto {

	public long id;
	public String title;
	public String artist;
	public int length;
	public String contentUrl;
	public String albumArt;
	/**
	 * Localized start time label for events (e.g. "16.06. 10:00"); null for playlist tracks.
	 */
	public String startLabel;

	public AudioAddictTrackDto() {
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("AudioAddictTrackDto [");
		sb.append("id=").append(this.id).append(", ");
		sb.append("title=").append(this.title).append(", ");
		sb.append("artist=").append(this.artist).append(", ");
		sb.append("length=").append(this.length).append(", ");
		sb.append("contentUrl=").append(this.contentUrl);
		sb.append("]");
		return sb.toString();
	}

}
