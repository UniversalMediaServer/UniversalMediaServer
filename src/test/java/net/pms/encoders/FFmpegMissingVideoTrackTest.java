package net.pms.encoders;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.List;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.io.OutputParams;
import net.pms.media.MediaInfo;
import net.pms.renderers.Renderer;
import net.pms.store.StoreItem;
import net.pms.store.item.RealFile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


class FFmpegMissingVideoTrackTest {
	@BeforeAll
	static void configure() throws Exception {
		System.setProperty(PMS.PROPERTY_RUNNING_TESTS, "true");
		PMS.setConfiguration(new UmsConfiguration(false));
	}

	private static class CommandCaptured extends RuntimeException {
		private final List<String> options;
		CommandCaptured(List<String> options) {
			this.options = options;
		}
	}

	@Test
	void missingVideoMetadataWithoutTsMuxer() throws Exception {
		checkMissingVideoMetadata(false);
	}

	@Test
	void missingVideoMetadataWithTsMuxer() throws Exception {
		checkMissingVideoMetadata(true);
	}

	private void checkMissingVideoMetadata(boolean tsMuxerEnabled) throws Exception {
		var config = new UmsConfiguration(false);
		config.setFFmpegMuxWithTsMuxerWhenCompatible(tsMuxerEnabled);
		var renderer = new Renderer((String) null) {
			@Override
			public UmsConfiguration getUmsConfiguration() {
				return config;
			}
		};
		var engine = new FFMpegVideo() {
			@Override
			public String getExecutable() {
				return "ffmpeg";
			}

			@Override
			protected synchronized List<String> getVideoTranscodeOptions(
				StoreItem item, MediaInfo media, OutputParams params, boolean canMux) {
				assertFalse(canMux, "Unknown video codec must not be copied");
				// Capture the real encoding options before launchTranscode creates a pipe/process.
				throw new CommandCaptured(super.getVideoTranscodeOptions(item, media, params, canMux));
			}
		};
		var item = new RealFile(renderer, new File("bbc_news.wtv"));
		var media = new MediaInfo();
		media.setContainer("wtv");
		item.setMediaInfo(media);
		item.setTranscodingSettings(new TranscodingSettings(engine, EncodingFormat.VIDEO.get("MPEGTS-H264-AC3")));
		var params = new OutputParams(config);
		params.setMediaRenderer(renderer);
		var captured = assertThrows(CommandCaptured.class, () -> engine.launchTranscode(item, media, params));
		assertEquals("libx264", captured.options.get(captured.options.indexOf("-c:v") + 1));
	}
}