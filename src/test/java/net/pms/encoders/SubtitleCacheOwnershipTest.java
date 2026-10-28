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
package net.pms.encoders;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import net.pms.configuration.UmsConfiguration;
import net.pms.formats.v2.SubtitleType;
import net.pms.io.OutputParams;
import net.pms.media.MediaInfo;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.media.video.MediaVideo;
import net.pms.parsers.ParserTest;
import net.pms.renderers.Renderer;
import net.pms.store.item.RealFile;
import net.pms.util.SubtitleUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SubtitleCacheOwnershipTest {
	@TempDir
	File tempDir;

	@BeforeAll
	static void configure() {
		ParserTest.setUpClass();
	}

	@Test
	void failedCacheConversionMustNotEditOrDeleteTheOriginalSubtitle() throws Exception {
		File original = new File(tempDir, "user.ass");
		byte[] bytes = ("\ufeff[Script Info]\nPlayResX: 384\nPlayResY: 288\n" +
			"[Events]\nFormat: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n" +
			"Dialogue: 0,0:00:00.00,0:00:01.00,Default,,0,0,0,,Caption\n").getBytes(StandardCharsets.UTF_16LE);
		Files.write(original.toPath(), bytes);
		var modified = Files.getLastModifiedTime(original.toPath());
		File cache = new File(tempDir, "unwritable-cache");
		assertTrue(cache.mkdir());
		assertTrue(cache.setWritable(false, false));
		UmsConfiguration config = new UmsConfiguration(false) {
			@Override
			public String getDataFile(String name) { return cache.getAbsolutePath(); }
		};
		config.setFFmpegFontConfig(true);
		config.setUseEmbeddedSubtitlesStyle(false);
		MediaSubtitle subtitle = new MediaSubtitle();
		subtitle.setId(1000);
		subtitle.setType(SubtitleType.ASS);
		subtitle.setExternalFile(original);
		MediaInfo media = new MediaInfo();
		MediaVideo video = new MediaVideo();
		video.setWidth(384);
		video.setHeight(288);
		video.setMultiViewLayout("half side by side (left eye first)");
		media.addVideoTrack(video);
		var renderer = new Renderer((String) null);
		var item = new RealFile(renderer, new File(tempDir, "source.mp4"));
		item.setMediaInfo(media);
		var params = new OutputParams(config);
		params.setSid(subtitle);
		try {
			File converted = SubtitleUtils.getSubtitles(item, media, params, config, SubtitleType.ASS);
			assertArrayEquals(bytes, Files.readAllBytes(original.toPath()), "Cache failure must not modify the user file");
			assertEquals(modified, Files.getLastModifiedTime(original.toPath()));
			assertFalse(new File(tempDir, "user_3D.ass").exists(), "No converted file beside the user file");
			assertNull(converted);
		} finally {
			assertTrue(cache.setWritable(true, true));
		}
	}
}
