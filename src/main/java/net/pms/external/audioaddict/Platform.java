package net.pms.external.audioaddict;

import java.util.ArrayList;
import java.util.List;

/**
 * Audio Addict Networks
 */
public enum Platform {

	CLASSIC_RADIO("Classical Radio", 	"http://listen.classicalradio.com",		"classicalradio", "radio_cr_",
		new StreamListQuality[]{StreamListQuality.AAC_64, StreamListQuality.AAC_128, StreamListQuality.MP3_320},
		"classicalradio.com"),
	RADIO_TUNES("Radio Tunes", 		"http://listen.radiotunes.com",			"radiotunes", "radio_rt_",
		new StreamListQuality[]{StreamListQuality.AAC_64, StreamListQuality.AAC_128, StreamListQuality.MP3_320},
		"radiotunes.com"),
	ROCK_RADIO("Rock Radio", 		"http://listen.rockradio.com",			"rockradio", "radio_rr_",
		new StreamListQuality[]{StreamListQuality.AAC_64, StreamListQuality.AAC_128, StreamListQuality.MP3_320},
		"rockradio.com"),
	JAZZ_RADIO("Jazz Radio", 		"http://listen.jazzradio.com",			"jazzradio", "radio_jr_",
		new StreamListQuality[]{StreamListQuality.AAC_64, StreamListQuality.AAC_128, StreamListQuality.MP3_320},
		"jazzradio.com"),
	ZEN_RADIO("Zen Radio", 		"http://listen.zenradio.com",			"zenradio", "radio_zr_",
		new StreamListQuality[]{StreamListQuality.AAC_64, StreamListQuality.AAC_128, StreamListQuality.MP3_320},
		"zenradio.com"),
	DI_FM("DI.fm", 			"http://listen.di.fm",					"di", "radio_di_",
		new StreamListQuality[]{StreamListQuality.AAC_64, StreamListQuality.AAC_128, StreamListQuality.MP3_320},
		"di.fm");


	public String displayName = "";
	public String listenUrl = "";
	public String shortName = "";
	public final AudioAddictLogo logo;
	public String favPrefix = "";
	public StreamListQuality[] streamList;

	private static List<RadioNetworkQuality> streamListAsDto = null;

	Platform(String displayName, String listenUrl, String shortName, String favPrefix, StreamListQuality[] streamList, String domain) {
		this.displayName = displayName;
		this.listenUrl = listenUrl;
		this.shortName = shortName;
		this.streamList = streamList;
		this.logo = new AudioAddictLogo(domain);
		this.favPrefix = favPrefix;
	}

	public synchronized List<RadioNetworkQuality> getStreamListQualityDto() {
		if (streamListAsDto == null) {
			streamListAsDto = new ArrayList<>();
			for (StreamListQuality streamListQuality : streamList) {
				streamListAsDto.add(new RadioNetworkQuality(streamListQuality.name(), streamListQuality.displayName));
			}
		}
		return streamListAsDto;
	}
}
