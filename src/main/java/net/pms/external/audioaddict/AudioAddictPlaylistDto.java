package net.pms.external.audioaddict;

public class AudioAddictPlaylistDto {

	public int id;
	public String name;
	public String description;
	public String duration;
	public int trackCount;
	public String albumArt;

	public AudioAddictPlaylistDto() {
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("AudioAddictPlaylistDto [");
		sb.append("id=").append(this.id).append(", ");
		sb.append("name=").append(this.name).append(", ");
		sb.append("trackCount=").append(this.trackCount).append(", ");
		sb.append("duration=").append(this.duration).append(", ");
		sb.append("albumArt=").append(this.albumArt);
		sb.append("]");
		return sb.toString();
	}

}
