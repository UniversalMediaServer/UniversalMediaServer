package net.pms.network.webguiserver.servlets;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.Collections;
import net.pms.util.SubtitleColor;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.encoders.EncodingFormat;
import net.pms.encoders.FFmpegHlsVideo;
import net.pms.encoders.TranscodingSettings;
import net.pms.formats.v2.SubtitleType;
import net.pms.media.MediaInfo;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.renderers.Renderer;
import net.pms.store.container.FileTranscodeVirtualFolder;
import net.pms.store.item.RealFile;
import org.junit.jupiter.api.Test;

class PlayerExternalSubtitlesTest {
	@Test
	void externalTracksAreOfferedOnlyForOrdinaryHlsWithoutChangingSelection() throws Exception {
		System.setProperty(PMS.PROPERTY_RUNNING_TESTS, "true");
		var config = new UmsConfiguration(false);
		PMS.setConfiguration(config);
		var renderer = new Renderer((String) null) {
			@Override
			public UmsConfiguration getUmsConfiguration() {
				return config;
			}
		};
		var constructor = FFmpegHlsVideo.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		var engine = constructor.newInstance();
		var item = new RealFile(renderer, new File("avatar.avi"));
		var media = new MediaInfo();
		var subtitle = new MediaSubtitle();
		subtitle.setId(0);
		subtitle.setLang("cze");
		subtitle.setType(SubtitleType.WEBVTT);
		subtitle.setExternalFileOnly(new File("avatar.cze.vtt"));
		var chinese = new MediaSubtitle();
		chinese.setId(0);
		chinese.setLang("chn");
		chinese.setType(SubtitleType.SUBRIP);
		chinese.setExternalFileOnly(new File("avatar.chn.srt"));
		media.getSubtitlesTracks().add(chinese);
		media.getSubtitlesTracks().add(subtitle);
		var embedded = new MediaSubtitle();
		embedded.setId(0);
		embedded.setType(SubtitleType.SUBRIP);
		media.getSubtitlesTracks().add(embedded);
		item.setMediaInfo(media);
		item.setMediaSubtitle(subtitle);
		item.setTranscodingSettings(new TranscodingSettings(
			engine, EncodingFormat.VIDEO.get("HLS-MPEGTS-H264-AAC")));
		var tracks = PlayerApiServlet.getExternalSubtitles(item);
		assertEquals(2, tracks.size());
		String chineseId = tracks.get(0).getAsJsonObject().get("id").getAsString();
		String czechId = tracks.get(1).getAsJsonObject().get("id").getAsString();
		assertNotEquals(chineseId, czechId);
		assertSame(chinese, PlayerApiServlet.findExternalSubtitle(item, chineseId));
		assertSame(subtitle, PlayerApiServlet.findExternalSubtitle(item, czechId));
		assertNull(PlayerApiServlet.findExternalSubtitle(item, "0"));
		assertNull(PlayerApiServlet.findExternalSubtitle(item, "unknown"));
		Collections.reverse(media.getSubtitlesTracks());
		assertSame(subtitle, PlayerApiServlet.findExternalSubtitle(item, czechId));
		assertSame(chinese, PlayerApiServlet.findExternalSubtitle(item, chineseId));
		Collections.reverse(media.getSubtitlesTracks());
		assertFalse(tracks.get(0).getAsJsonObject().get("default").getAsBoolean());
		assertTrue(tracks.get(1).getAsJsonObject().get("default").getAsBoolean());
		assertEquals("cs", tracks.get(1).getAsJsonObject().get("language").getAsString());
		assertSame(subtitle, item.getMediaSubtitle());
		var selectedCopy = (MediaSubtitle) subtitle.clone();
		item.setMediaSubtitle(selectedCopy);
		assertTrue(PlayerApiServlet.getExternalSubtitles(item).get(1).getAsJsonObject().get("default").getAsBoolean());
		config.setFont("Arial");
		config.setSubsColor(new SubtitleColor(255, 205, 89, 128));
		config.setSubtitleFontHeightPercent("7.3");
		var style = PlayerApiServlet.getSubtitleStyle(item);
		assertEquals("#FFCD5980", style.get("color").getAsString());
		assertEquals(7.3, style.get("fontHeightPercent").getAsDouble());
		assertTrue(style.has("fontFamily"));
		item.setMediaSubtitle(null);
		assertFalse(PlayerApiServlet.getExternalSubtitles(item).get(1).getAsJsonObject().get("default").getAsBoolean());
		item.setMediaSubtitle(subtitle);
		item.setParent(new FileTranscodeVirtualFolder(renderer, item));
		assertTrue(PlayerApiServlet.getExternalSubtitles(item).isEmpty());
		assertNull(PlayerApiServlet.findExternalSubtitle(item, czechId));
		assertSame(subtitle, item.getMediaSubtitle());
	}
}
