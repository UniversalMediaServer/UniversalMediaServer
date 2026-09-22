package net.pms.util;

import net.pms.configuration.UmsConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SubtitleColorTest {
	@Test
	void ffmpegUsesAssHexPrefixAndBgrOrder() {
		assertEquals("&H000000FF", new SubtitleColor(255, 0, 0).getASSv4PlusStylesHexValueForFFmpeg());
		assertEquals("&H0000FF00", new SubtitleColor(0, 255, 0).getASSv4PlusStylesHexValueForFFmpeg());
		assertEquals("&H00FF0000", new SubtitleColor(0, 0, 255).getASSv4PlusStylesHexValueForFFmpeg());
		assertEquals("&H00FFFFFF", new SubtitleColor(255, 255, 255).getASSv4PlusStylesHexValueForFFmpeg());
	}

	@Test
	void ffmpegInvertsOpacityForAssAlpha() {
		assertEquals("&H7F332211", new SubtitleColor(0x11, 0x22, 0x33, 0x80).getASSv4PlusStylesHexValueForFFmpeg());
		assertEquals("&HFF332211", new SubtitleColor(0x11, 0x22, 0x33, 0).getASSv4PlusStylesHexValueForFFmpeg());
	}

	@Test
	void guiConfigurationColorReachesFfmpegUnchanged() throws Exception {
		var configuration = new UmsConfiguration(false);
		configuration.setSubsColor(new SubtitleColor(0x11, 0x22, 0x33, 0x80));
		assertEquals("&H7F332211", configuration.getSubsColor().getASSv4PlusStylesHexValueForFFmpeg());
	}
}
