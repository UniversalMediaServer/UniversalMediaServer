package net.pms.external.audioaddict.mapper;

import java.util.ArrayList;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ChannelFilter {

	public int id;
	@JsonProperty("description_text")
	public String descriptionText;
	@JsonProperty("description_title")
	public String descriptionTitle;
	public boolean display;
	@JsonProperty("display_description")
	public boolean displayDescription;
	public boolean genre;
	public String key;
	public boolean meta;
	public String name;
	public int position;
	@JsonProperty("network_id")
	public int networkId;
	@JsonProperty("created_at")
	public Object createdAt;
	@JsonProperty("updated_at")
	public Date updatedAt;
	public Images images;
	public ArrayList<Channel> channels;
}
