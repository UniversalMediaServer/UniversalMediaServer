package net.pms.external.audioaddict.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Asset {

	public int id;
	public String name;
	@JsonProperty("content_hash")
	public String contentHash;
}
