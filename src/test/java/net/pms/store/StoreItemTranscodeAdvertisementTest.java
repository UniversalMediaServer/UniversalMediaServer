/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.store;

import static org.junit.jupiter.api.Assertions.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.pms.configuration.RendererConfiguration;
import net.pms.encoders.EncodingFormat;
import net.pms.formats.FormatFactory;
import net.pms.media.MediaInfo;
import net.pms.media.audio.MediaAudio;
import net.pms.media.video.MediaVideo;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.Result;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.StoreResourceHelper;
import net.pms.parsers.ParserTest;
import net.pms.renderers.Renderer;
import net.pms.store.container.FileTranscodeVirtualFolder;
import net.pms.store.item.RealFile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class StoreItemTranscodeAdvertisementTest {
	@BeforeAll
	static void configure() {
		ParserTest.setUpClass();
	}

	private StoreItem item(boolean stereo) throws Exception {
		Renderer renderer = new Renderer(new RendererConfiguration(new File("src/main/external-resources/renderers/Sony-BraviaEX725.conf"))) {
			@Override
			public List<EncodingFormat> getTranscodingFormats() {
				return List.of(EncodingFormat.VIDEO.get("MPEGTS-H264-AC3"));
			}
			@Override
			public String getOutput3DFormat() { return "sbs2l"; }
			@Override
			public String getTranscodedExtensions() { return stereo ? "" : "mp4"; }
		};
		MediaVideo video = new MediaVideo();
		video.setCodec("h264");
		video.setFormatLevel("4.0");
		video.setWidth(1920);
		video.setHeight(1080);
		if (stereo) {
			video.setMultiViewLayout("top-bottom (left eye first)");
		}
		MediaAudio audio = new MediaAudio();
		audio.setCodec("aac-lc");
		MediaInfo media = new MediaInfo();
		media.setContainer("mp4");
		media.setMimeType("video/mp4");
		media.setMediaParser("test");
		media.addVideoTrack(video);
		media.addAudioTrack(audio);
		StoreItem item = new RealFile(renderer, new File("stereo.mp4"));
		item.setId("137");
		item.setFormat(FormatFactory.getAssociatedFormat("stereo.mp4"));
		item.setMediaInfo(media);
		return item;
	}

	@Test
	void verdictUsesTheSelectedOutputMimeBeforeSettingsAreAssigned() throws Exception {
		StoreItem item = item(false); // Force an ordinary 2D MP4, independently of the stereo fix.
		Logger logger = (Logger) LoggerFactory.getLogger(StoreItem.class);
		Level previous = logger.getLevel();
		ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> events = new ListAppender<>();
		events.start();
		logger.addAppender(events);
		logger.setLevel(Level.TRACE);
		try {
			assertNotNull(item.resolveTranscodingSettings());
			assertNull(item.getTranscodingSettings());
			assertTrue(events.list.stream().map(e -> e.getFormattedMessage()).anyMatch(message ->
				message.contains("Final verdict:") && message.contains("MPEGTS-H264-AC3") &&
				message.contains("mime type \"video/vnd.dlna.mpeg-tts\"")));
		} finally {
			logger.detachAppender(events);
			logger.setLevel(previous);
			events.stop();
		}
	}

	@Test
	void mainItemAdvertisesOnlyTheSelectedTransportStream() throws Exception {
		StoreItem item = item(false);
		item.setTranscodingSettings(item.resolveTranscodingSettings()); // StoreContainer.addChild does this before DIDL generation.
		assertTrue(item.isTranscoded());
		var main = StoreResourceHelper.getItem(item, "*");
		var videoResources = main.getResources().stream().filter(res -> res.getProtocolInfo().getContentFormat().startsWith("video/")).toList();
		assertEquals(3, videoResources.size()); // Sony requires three DLNA localizations of the same stream.
		for (var res : videoResources) {
			assertEquals("video/vnd.dlna.mpeg-tts", res.getProtocolInfo().getContentFormat());
			assertTrue(res.getProtocolInfo().getAdditionalInfo().contains("DLNA.ORG_PN=AVC_TS_"));
			assertTrue(res.getValue().toString().contains("/137/"));
			assertTrue(res.getValue().toString().endsWith("_transcoded_to.ts"));
			assertNull(res.getSize());
		}
		String xml = new Result().addItem(main).toString();
		assertFalse(xml.contains("http-get:*:video/mp4:"));
		Files.writeString(Path.of("target/case2-main-item.didl.xml"), xml);

		// The explicit No transcoding entry is a distinct object and retains its override.
		StoreItem direct = item.clone();
		direct.setParent(new FileTranscodeVirtualFolder(item.getDefaultRenderer(), item));
		direct.setId("138");
		direct.setNoName(true);
		direct.setTranscodingSettings(null);
		assertNull(direct.resolveTranscodingSettings());
		assertFalse(direct.isTranscoded());
		var original = StoreResourceHelper.getItem(direct, "*");
		assertTrue(original.getTitle().contains("No transcoding"));
		for (var res : original.getResources()) {
			if (res.getProtocolInfo().getContentFormat().startsWith("video/")) {
				assertEquals("video/mp4", res.getProtocolInfo().getContentFormat());
				assertTrue(res.getValue().toString().contains("/138/"));
				assertFalse(res.getValue().toString().contains("_transcoded_to"));
			}
		}
		Files.writeString(Path.of("target/case2-no-transcoding.didl.xml"), new Result().addItem(original).toString());
	}
}
