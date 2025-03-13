package net.pms.external.audioaddict.mapper;

import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Show {

	public int id;
	public String name;
	public String description;
	@JsonProperty("artists_tagline")
	public String artistsTagline;
	@JsonProperty("description_html")
	public String descriptionHtml;
	@JsonProperty("human_readable_schedule")
	public ArrayList<String> humanReadableSchedule;
	@JsonProperty("ondemand_episode_count")
	public int ondemandEpisodeCount;
	public ArrayList<Channel> channels;
	public Images images;
}
