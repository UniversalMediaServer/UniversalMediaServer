package net.pms.encoders;

import java.io.File;
import java.util.List;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.io.OutputParams;
import net.pms.media.MediaInfo;
import net.pms.media.MediaLang;
import net.pms.media.audio.MediaAudio;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.media.video.MediaVideo;
import net.pms.renderers.Renderer;
import net.pms.store.container.FileTranscodeVirtualFolder;
import net.pms.store.item.RealFile;
import net.pms.formats.v2.SubtitleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FFmpegHlsSubtitlesTest {
	@org.junit.jupiter.api.BeforeAll static void configure() throws Exception {
		System.setProperty(PMS.PROPERTY_RUNNING_TESTS, "true");
		PMS.setConfiguration(new UmsConfiguration(false));
	}
	private RealFile item;
	private OutputParams params;
	private Renderer renderer;
	private final FFmpegHlsVideo engine = new FFmpegHlsVideo() {
		@Override public String getExecutable() { return "ffmpeg"; }
	};

	@BeforeEach
	void setup() throws Exception {
		System.setProperty(PMS.PROPERTY_RUNNING_TESTS, "true");
		var config = new UmsConfiguration(false);
		config.setFFmpegFontConfig(true);
		config.setSubtitleFontHeightPercent("7.5");
		PMS.setConfiguration(config);
		renderer = new Renderer((String) null) {
			@Override public UmsConfiguration getUmsConfiguration() { return config; }
			@Override public boolean streamSubsForTranscodedVideo() { return true; }
			@Override public boolean isExternalSubtitlesFormatSupported(MediaSubtitle sub, net.pms.store.StoreItem resource) { return true; }
		};
		item = new RealFile(renderer, new File("multiple_sub_sample.mkv"));
		var media = new MediaInfo();
		var video = new MediaVideo(); video.setWidth(1280); video.setHeight(720); media.addVideoTrack(video);
		for (int i = 0; i < 2; i++) {
			var audio = new MediaAudio(); audio.setId(i); media.addAudioTrack(audio);
			media.getSubtitlesTracks().add(subtitle(i));
		}
		item.setMediaInfo(media);
		item.setParent(new FileTranscodeVirtualFolder(renderer, item));
		item.setTranscodingSettings(new TranscodingSettings(engine, EncodingFormat.VIDEO.get("HLS-MPEGTS-H264-AAC")));
		params = new OutputParams(config); params.setMediaRenderer(renderer);
		params.setHlsConfiguration(HlsHelper.getByKey("HD_AAC-LC_0"));
	}

	private static MediaSubtitle subtitle(int id) {
		var sub = new MediaSubtitle(); sub.setId(id); sub.setType(SubtitleType.SUBRIP); sub.setLang("eng"); return sub;
	}

	private List<String> command() throws Exception { return engine.buildCommand(item, item.getMediaInfo(), params); }

	@Test void burnsSelectedExternalTrackWithConfiguredStyleAndPreservesSeekTimestamps() throws Exception {
		var sub = subtitle(100); sub.setExternalFileOnly(new File("selected.srt")); item.setMediaSubtitle(sub);
		params.setTimeSeek(60);
		var command = command(); String filter = command.get(command.indexOf("-filter_complex") + 1);
		assertTrue(filter.contains("selected.srt"), filter);
		assertTrue(filter.contains("Fontsize=21.6"), filter);
		assertTrue(filter.contains("PrimaryColour=&H"), filter);
		assertFalse(filter.contains("setpts="), filter);
		assertFalse(command.contains("0:V")); // no second, unfiltered video output
		assertTrue(command.contains("0:a:0"));
		assertFalse(HlsHelper.getHLSm3u8(item, renderer, "/").contains("TYPE=SUBTITLES"));
	}

	@Test void burnsSecondEmbeddedTrackInsteadOfFirst() throws Exception {
		item.setMediaSubtitle(item.getMediaInfo().getSubtitlesTracks().get(1));
		var command = command(); String filter = command.get(command.indexOf("-filter_complex") + 1);
		assertTrue(filter.contains(":si=1"), filter);
		assertFalse(HlsHelper.getHLSm3u8(item, renderer, "/").contains("SUBTITLES="));
	}

	@Test void offSelectionDoesNotExposeEmbeddedSubtitles() throws Exception {
		item.setMediaSubtitle(subtitle(MediaLang.DUMMY_ID));
		assertFalse(command().contains("-filter_complex"));
		assertFalse(HlsHelper.getHLSm3u8(item, renderer, "/").contains("TYPE=SUBTITLES"));
	}

	@Test void ordinaryHlsKeepsSelectableSubtitleRenditions() throws Exception {
		item.setParent(null); item.setMediaSubtitle(item.getMediaInfo().getSubtitlesTracks().get(1));
		assertFalse(command().contains("-filter_complex"));
		String playlist = HlsHelper.getHLSm3u8(item, renderer, "/");
		assertTrue(playlist.contains("NONE_NONE_0.m3u8"));
		assertTrue(playlist.contains("NONE_NONE_1.m3u8"));
	}

	@Test void audioRenditionsDoNotBurnVideoSubtitles() throws Exception {
		item.setMediaSubtitle(item.getMediaInfo().getSubtitlesTracks().get(1));
		params.setHlsConfiguration(HlsHelper.getByKey("NONE_AAC-LC_0"));
		assertFalse(command().contains("-filter_complex"));
	}
}
