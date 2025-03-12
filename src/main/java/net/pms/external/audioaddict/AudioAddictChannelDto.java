package net.pms.external.audioaddict;

public class AudioAddictChannelDto {

	public Integer tracklistServerId;
	public Integer id;
	public String key;
	public String name;
	public String streamUrl;
	public String descLong;
	public String descShort;
	public String albumArt;

	public AudioAddictChannelDto() {
	}

	public AudioAddictChannelDto(Integer tracklistServerId, Integer id, String key, String name, String streamUrl, String descLong,
		String descShort, String albumArt) {
		this.tracklistServerId = tracklistServerId;
		this.id = id;
		this.key = key;
		this.name = name;
		this.streamUrl = streamUrl;
		this.descLong = descLong;
		this.descShort = descShort;
		this.albumArt = albumArt;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("AudioAddictChannelDto [");
		sb.append("tracklistServerId=").append(this.tracklistServerId).append(", ");
		sb.append("id=").append(this.id).append(", ");
		sb.append("key=").append(this.key).append(", ");
		sb.append("name=").append(this.name).append(", ");
		sb.append("streamUrl=").append(this.streamUrl).append(", ");
		sb.append("descLong=").append(this.descLong).append(", ");
		sb.append("descShort=").append(this.descShort).append(", ");
		sb.append("albumArt=").append(this.albumArt).append(", ");
		sb.append("]");
		return sb.toString();
	}

}