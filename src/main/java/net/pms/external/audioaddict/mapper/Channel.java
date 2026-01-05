package net.pms.external.audioaddict.mapper;

import java.util.ArrayList;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Channel {

	public int id;
	@JsonProperty("ad_channels")
	public String adChannels;
	@JsonProperty("channel_director")
	public String channelDirector;
	@JsonProperty("description_long")
	public String descriptionLong;
	@JsonProperty("description_short")
	public String descriptionShort;
	public String key;
	public String name;
	public boolean mypublic;
	@JsonProperty("ad_dfp_unit_id")
	public String adDfpUnitId;
	@JsonProperty("network_id")
	public int networkId;
	@JsonProperty("premium_id")
	public int premiumId;
	@JsonProperty("tracklist_server_id")
	public int tracklistServerId;
	public ArrayList<Artist> artists;
	@JsonProperty("asset_id")
	public int assetId;
	@JsonProperty("asset_url")
	public String assetUrl;
	@JsonProperty("banner_url")
	public String bannerUrl;
	public String description;
	@JsonProperty("created_at")
	public Date createdAt;
	@JsonProperty("updated_at")
	public Date updatedAt;
	@JsonProperty("similar_channels")
	public ArrayList<SimilarChannel> similarChannels;
	public Images images;
	@JsonProperty("channel_filter_ids")
	public int[] channelFilterIds;
}
