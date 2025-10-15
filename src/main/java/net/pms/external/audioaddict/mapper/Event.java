package net.pms.external.audioaddict.mapper;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Event {

	public int id;
	@JsonProperty("start_at")
	public Date startAt;
	public int duration;
	public String name;
	@JsonProperty("end_at")
	public Date endAt;
	@JsonProperty("artists_tagline")
	public String artistsTagline;
	@JsonProperty("description_html")
	public String descriptionHtml;
	public Show show;
	@JsonProperty("event_id")
	public int eventId;
	@JsonProperty("channel_id")
	public int channelId;
	@JsonProperty("start_date")
	public String startDate;
	@JsonProperty("end_date")
	public String endDate;
	public String title;
	public String description;
	public String url;
}
