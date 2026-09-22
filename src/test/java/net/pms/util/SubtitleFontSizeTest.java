package net.pms.util;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.media.MediaInfo;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class SubtitleFontSizeTest {
	@TempDir Path temp;

	@BeforeAll
	static void setup() throws Exception {
		System.setProperty(PMS.PROPERTY_RUNNING_TESTS, "true");
		PMS.setConfiguration(new UmsConfiguration(false));
	}

	@Test
	void migratesLegacyWithoutChangingEffectiveSize() throws Exception {
		var c = new UmsConfiguration(false);
		assertEquals(21, c.getSubtitleFontSize(288), 1e-10);
		c.setAssScale("2.0");
		assertEquals(30, c.getSubtitleFontSize(288), 1e-10);
		c.setSubtitleFontHeightPercent("7");
		assertEquals(20.16, c.getSubtitleFontSize(288), 1e-10);
		assertEquals(75.6, c.getSubtitleFontSize(1080), 1e-10);
	}

	@Test
	void handlesDecimalCommaAndInvalidSettings() throws Exception {
		var c = new UmsConfiguration(false);
		c.setSubtitleFontHeightPercent("7,5");
		assertEquals(21.6, c.getSubtitleFontSize(288), 1e-10);
		for (String value : new String[] {"NaN", "Infinity", "-3", "0", "bad"}) {
			c.setSubtitleFontHeightPercent(value);
			assertEquals(21, c.getSubtitleFontSize(288), 1e-10);
		}
	}

	@Test
	void usesAssCoordinateHeightAndKeepsFractionalSizes() throws Exception {
		var c = new UmsConfiguration(false);
		c.setSubtitleFontHeightPercent("7.5");
		for (int height : new int[] {288, 720, 1080, 2160}) {
			Path p = temp.resolve(height + ".ass");
			Files.writeString(p, "[Script Info]\nScriptType: v4.00+\nPlayResX: 384\nPlayResY: " +
							height +
							"\n\n" +
							"[V4+ Styles]\n" +
							"Format: Name, Fontname, Fontsize, PrimaryColour\n" +
							"Style: Default,Arial,16.5,&H00FFFFFF\n\n" +
							"[Events]\n");
			SubtitleUtils.applyFontconfigToASSTempSubsFile(p.toFile(), new MediaInfo(), c);
			String style =Files.readAllLines(p).stream()
							.filter(l -> l.startsWith("Style:"))
							.findFirst()
							.orElseThrow();
			assertEquals(height * 0.075, Double.parseDouble(style.split(",")[2]), 1e-10);
		}
	}

	@Test
	void suppliesReferenceCoordinatesWhenMissing() throws Exception {
		var c = new UmsConfiguration(false);
		c.setSubtitleFontHeightPercent("7");
		Path p = temp.resolve("missing.ass");
		Files.writeString(p, "[Script Info]\n" +
						"ScriptType: v4.00+\n\n" +
						"[V4+ Styles]\n" +
						"Format: Name, Fontname, Fontsize\n" +
						"Style: Default,Arial,16\n");
		SubtitleUtils.applyFontconfigToASSTempSubsFile(p.toFile(), new MediaInfo(), c);
		String result = Files.readString(p);
		assertTrue(result.contains("PlayResY: 288"));
		assertTrue(result.contains("PlayResX: 384"));
		assertTrue(result.contains("Arial,20.16"));
	}

	@Test
	void threeDimensionalLayoutAppliesPercentageOnlyOnce() throws Exception {
		var c = new UmsConfiguration(false);
		c.setSubtitleFontHeightPercent("7.5");
		var params = new net.pms.io.OutputParams(c);
		params.setMediaRenderer(
				new net.pms.renderers.Renderer((String) null) {
					@Override
					public UmsConfiguration getUmsConfiguration() {
						return c;
					}
				});
		try {
			for (var mode :
					new net.pms.media.video.MediaVideo.Mode3D[] {
						net.pms.media.video.MediaVideo.Mode3D.ABL, net.pms.media.video.MediaVideo.Mode3D.SBSL
					}) {
				var media = new MediaInfo();
				media.addVideoTrack(
						new net.pms.media.video.MediaVideo() {
							@Override
							public Mode3D get3DLayout() {
								return mode;
							}
						});
				Path p = temp.resolve(mode + ".ass");
				Files.writeString(p, "[Script Info]\n\n" +
								"[Events]\n" +
								"Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect," +
								" Text\n" +
								"Dialogue: 0,0:00:00.00,0:00:05.00,Default,,0,0,0,,Test\n");
				var output = SubtitleUtils.convertASSToASS3D(p.toFile(), media, params);
				String[] style = Files.readAllLines(output.toPath()).stream()
								.filter(l -> l.startsWith("Style:"))
								.findFirst()
								.orElseThrow()
								.split(",");
				assertEquals(21.6, Double.parseDouble(style[2]), 1e-10);
				assertEquals(mode == net.pms.media.video.MediaVideo.Mode3D.ABL ? "100" : "50", style[11]);
				assertEquals(mode == net.pms.media.video.MediaVideo.Mode3D.ABL ? "50" : "100", style[12]);
			}
		} finally {
			c.setSubtitleFontHeightPercent("");
		}
	}
}
