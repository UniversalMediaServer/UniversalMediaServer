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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.File;
import java.util.List;
import net.pms.encoders.EncodingFormat;
import net.pms.formats.Format;
import net.pms.formats.MKV;
import net.pms.media.MediaInfo;
import net.pms.media.video.MediaVideo;
import net.pms.parsers.ParserTest;
import net.pms.renderers.Renderer;
import net.pms.store.item.RealFile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class StoreItemStereoTest {

	@BeforeAll
	static void configure() {
		ParserTest.setUpClass();
	}

	private StoreItem item(String codec, String layout, String output, boolean streamed) throws Exception {
		Renderer renderer = new Renderer((String) null) {
			@Override
			public String getRendererName() {
				return "Stereo refusal test";
			}

			@Override
			public boolean isUseMediaInfo() {
				return true;
			}

			@Override
			public boolean isCompatible(StoreItem resource, Format format) {
				return true;
			}

			@Override
			public List<EncodingFormat> getTranscodingFormats() {
				return List.of(EncodingFormat.VIDEO.get("MPEGTS-H264-AC3"));
			}

			@Override
			public String getOutput3DFormat() {
				return output;
			}

			@Override
			public String getStreamedExtensions() {
				return streamed ? "mkv" : null;
			}
		};
		MediaVideo video = new MediaVideo();
		video.setCodec(codec);
		video.setFormatLevel("4.0");
		video.setWidth(1280);
		video.setHeight(720);
		video.setMultiViewLayout(layout);
		MediaInfo media = new MediaInfo();
		media.addVideoTrack(video);
		StoreItem item = new RealFile(renderer, new File("stereo.mkv"));
		MKV format = new MKV();
		assertTrue(format.match("stereo.mkv"));
		item.setFormat(format);
		item.setMediaInfo(media);
		return item;
	}

	@Test
	void h264StereoRequiresTheRequestedConversion() throws Exception {
		assertNotNull(item("h264", "side by side (left eye first)", "abl", false).resolveTranscodingSettings());
		assertNotNull(item("h264", "side by side (left eye first)", "ml", false).resolveTranscodingSettings());
	}

	@Test
	void compatibleAndUnspecifiedStereoAndMonoCanStream() throws Exception {
		assertNull(item("h264", "side by side (left eye first)", "sbsl", true).resolveTranscodingSettings());
		assertNull(item("h264", "side by side (left eye first)", "", true).resolveTranscodingSettings());
		assertNull(item("h264", null, "abl", true).resolveTranscodingSettings());
	}

	@Test
	void unsupportedPackingFailsVisiblyInsteadOfStreaming() {
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			item("h265", "checkboard (left eye first)", "sbsl", false).resolveTranscodingSettings();
		});
		assertTrue(exception.getMessage().contains("from \"checkboard (left eye first)\" to \"sbsl\""));
	}

	@Test
	void streamedUnsupportedPackingFailsVisiblyInsteadOfStreaming() {
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			item("h265", "checkboard (left eye first)", "sbsl", true).resolveTranscodingSettings();
		});
		assertTrue(exception.getMessage().contains("from \"checkboard (left eye first)\" to \"sbsl\""));
	}

	@Test
	void unsupportedRecognizedAnaglyphPackingFailsVisiblyInsteadOfStreaming() {
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			item("h265", "arcc", "sbsl", false).resolveTranscodingSettings();
		});
		assertTrue(exception.getMessage().contains("from \"arcc\" to \"sbsl\""));
	}
	@Test
	void refusalNamesTheFileLayoutsAndRendererAtWarningLevel() throws Exception {
		StoreItem resource = item("h265", "arcc", "sbsl", false);
		Logger logger = (Logger) LoggerFactory.getLogger(StoreItem.class);
		Level previous = logger.getLevel();
		ListAppender<ILoggingEvent> events = new ListAppender<>();
		events.start();
		logger.addAppender(events);
		logger.setLevel(Level.WARN);
		try {
			assertThrows(IllegalStateException.class, resource::resolveTranscodingSettings);
			assertTrue(events.list.stream().anyMatch(event -> event.getLevel() == Level.WARN &&
				event.getFormattedMessage().contains("No supported conversion can convert \"stereo.mkv\" from \"arcc\" to \"sbsl\", so it is not offered to ") &&
				event.getFormattedMessage().endsWith("Stereo refusal test")));
		} finally {
			logger.detachAppender(events);
			logger.setLevel(previous);
			events.stop();
		}
	}

	@Test
	void unsupportedPackingCanStreamWhenNoConversionIsRequested() throws Exception {
		assertNull(item("h265", "checkboard (left eye first)", "", true).resolveTranscodingSettings());
		assertNull(item("h265", "arcc", "arcc", true).resolveTranscodingSettings());
	}

}
