package net.pms.external.audioaddict.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Images {

	public String mydefault;
	@JsonProperty("horizontal_banner")
	public String horizontalBanner;
	@JsonProperty("tall_banner")
	public String tallBanner;
	public String compact;
	public String square;
	public String vertical;
}
