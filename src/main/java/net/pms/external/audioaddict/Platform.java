package net.pms.external.audioaddict;

import java.util.ArrayList;
import java.util.List;

/**
 * Audio Addict Networks
 */
public enum Platform {

	CLASSIC_RADIO("Classical Radio", 	"http://listen.classicalradio.com",		"classicalradio", "radio_cr_",
		new StreamListQuality[]{StreamListQuality.AAC_64, StreamListQuality.AAC_128, StreamListQuality.MP3_320},
		"https://play-lh.googleusercontent.com/qW74pVFmQrtsjnHsPnKNr3bI9mMuTj1pIQm50NlWCuzm9dGQGYlZHG0YKgcokDkUidM"),
	RADIO_TUNES("Radio Tunes", 		"http://listen.radiotunes.com",			"radiotunes", "radio_rt_",
		new StreamListQuality[]{StreamListQuality.AAC_64, StreamListQuality.AAC_128, StreamListQuality.MP3_320},
		"https://cdn.audioaddict.com/radiotunes.com/assets/logo-opengraph-919eb9235fe962984e6ae26245aab6d693a5cf67cd2f6c6185bfcedaa7c5908b.png"),
	ROCK_RADIO("Rock Radio", 		"http://listen.rockradio.com",			"rockradio", "radio_rr_",
		new StreamListQuality[]{StreamListQuality.AAC_64, StreamListQuality.AAC_128, StreamListQuality.MP3_320},
		"https://play-lh.googleusercontent.com/GB5h6krjJRyABrCSnDHg4qz6-6CJg2W3-8HLqczsebzBX3WtY5INePZPl46ssQBIyoY"),
	JAZZ_RADIO("Jazz Radio", 		"http://listen.jazzradio.com",			"jazzradio", "radio_jr_",
		new StreamListQuality[]{StreamListQuality.AAC_64, StreamListQuality.AAC_128, StreamListQuality.MP3_320},
		"https://play-lh.googleusercontent.com/Waca0ZFN4T4CBt8aPyfUdtn-ZaEbBoCVtD0jtt3QFn_Pv1eAFio2M4Pk9pIbKrRZ5is"),
	ZEN_RADIO("Zen Radio", 		"http://listen.zenradio.com",			"zenradio", "radio_zr_",
		new StreamListQuality[]{StreamListQuality.AAC_64, StreamListQuality.AAC_128, StreamListQuality.MP3_320},
		"https://www.zenradio.com/assets/logo-opengraph-3b9fe3d5c4716aebbd04d43d459d266b7f978dcbf9dcdbb8f6896b6665d3d418.png"),
	DI_FM("DI.fm", 			"http://listen.di.fm",					"di", "radio_di_",
		new StreamListQuality[]{StreamListQuality.AAC_64, StreamListQuality.AAC_128, StreamListQuality.MP3_320},
		"https://cdn.audioaddict.com/di.fm/assets/logo-opengraph-62075c0e0f3327c590c93e269c2ed71a9a70caae992c8dd9bcd190b2505f3bc1.png");


	public String displayName = "";
	public String listenUrl = "";
	public String shortName = "";
	public String albumArt = "";
	public String favPrefix = "";
	public StreamListQuality[] streamList;

	private static List<RadioNetworkQuality> streamListAsDto = null;

	Platform(String displayName, String listenUrl, String shortName, String favPrefix, StreamListQuality[] streamList, String albumArt) {
		this.displayName = displayName;
		this.listenUrl = listenUrl;
		this.shortName = shortName;
		this.streamList = streamList;
		this.albumArt = albumArt;
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
