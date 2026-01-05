package net.pms.external.audioaddict.mapper;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Root {

	@JsonProperty("channel_filters")
	public ArrayList<ChannelFilter> channelFilters;
	public ArrayList<Asset> assets;
	@JsonProperty("stream_sets")
	public ArrayList<Object> streamSets;
	public ArrayList<Event> events;
	@JsonProperty("track_history")
	public Map<String, TrackHistoryItem> trackHistory;
	public Object notification;
	@JsonProperty("ad_network")
	public String adNetwork;
	@JsonProperty("ad_networks")
	public ArrayList<Object> adNetworks;
	@JsonProperty("cached_at")
	public Date cachedAt;
}
