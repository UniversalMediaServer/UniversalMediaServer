package net.pms.external.audioaddict.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SimilarChannel {

	public int id;
	@JsonProperty("similar_channel_id")
	public int similarChannelId;
}
