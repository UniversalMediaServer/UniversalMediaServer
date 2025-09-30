package net.pms.external.audioaddict.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Artist {

	public int id;
	public String name;
	@JsonProperty("asset_url")
	public String assetUrl;
	public Images images;
}
