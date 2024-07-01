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
package net.pms.util;

import java.io.File;
import java.util.Random;
import net.pms.PMS;
import net.pms.TestHelper;
import net.pms.configuration.RendererConfigurations;
import net.pms.configuration.UmsConfiguration;
import net.pms.formats.Format;
import net.pms.store.item.RealFile;
import net.pms.store.item.WebStream;
import static net.pms.util.PlayerUtil.*;
import org.apache.commons.configuration.ConfigurationException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PlayerUtilTest {

	private static RealFile video;
	private static RealFile audio;
	private static RealFile image;
	private static WebStream webImage;
	private static WebStream webVideo;
	private static WebStream webAudio;

	@BeforeAll
	public static void setUpClass() throws ConfigurationException, InterruptedException {
		PMS.setConfiguration(new UmsConfiguration(false));
		// initialise the fixtures
		// XXX we need to call isValid to call checktype(), which is needed to initialise the format
		image = new RealFile(RendererConfigurations.getDefaultRenderer(), getNonExistingFile("test.jpg"));
		image.resolveFormat();
		audio = new RealFile(RendererConfigurations.getDefaultRenderer(), getNonExistingFile("test.mp3"));
		audio.resolveFormat();
		video = new RealFile(RendererConfigurations.getDefaultRenderer(), getNonExistingFile("test.mpg"));
		video.resolveFormat();
		webImage = new WebStream(RendererConfigurations.getDefaultRenderer(), "", "http://example.com/test.jpg", "", Format.IMAGE, null);
		webImage.isValid();
		webAudio = new WebStream(RendererConfigurations.getDefaultRenderer(), "", "http://example.com/test.mp3", "", Format.AUDIO, null);
		webAudio.isValid();
		webVideo = new WebStream(RendererConfigurations.getDefaultRenderer(), "", "http://example.com/test.mpg", "", Format.VIDEO, null);
		webVideo.isValid();
	}

	@BeforeEach
	public void setUp() {
		TestHelper.SetLoggingOff();
	}

	@Test
	// sanity check
	public void testFixtures() {
		assertNotNull(image);
		assertNotNull(image.getFormat());
		assertNotNull(image.getFormat().getIdentifier());

		assertNotNull(audio);
		assertNotNull(audio.getFormat());
		assertNotNull(audio.getFormat().getIdentifier());

		assertNotNull(video);
		assertNotNull(video.getFormat());
		assertNotNull(video.getFormat().getIdentifier());

		assertNotNull(webImage);
		assertNotNull(webImage.getFormat());
		assertNotNull(webImage.getFormat().getIdentifier());

		assertNotNull(webAudio);
		assertNotNull(webAudio.getFormat());
		assertNotNull(webAudio.getFormat().getIdentifier());

		assertNotNull(webVideo);
		assertNotNull(webVideo.getFormat());
		assertNotNull(webVideo.getFormat().getIdentifier());
	}

	@Test
	public void testIsImage1() {
		assertTrue(isImage(image));
		assertFalse(isImage(audio));
		assertFalse(isImage(video));
		assertTrue(isImage(webImage));
		assertFalse(isImage(webAudio));
		assertFalse(isImage(webVideo));
	}

	@Test
	public void testIsAudio1() {
		assertFalse(isAudio(image));
		assertTrue(isAudio(audio));
		assertFalse(isAudio(video));
		assertFalse(isAudio(webImage));
		assertTrue(isAudio(webAudio));
		assertFalse(isAudio(webVideo));
	}

	@Test
	public void testIsVideo1() {
		assertFalse(isVideo(image));
		assertFalse(isVideo(audio));
		assertTrue(isVideo(video));
		assertFalse(isVideo(webImage));
		assertFalse(isVideo(webAudio));
		assertTrue(isVideo(webVideo));
	}

	@Test
	public void testIsImage2() {
		assertTrue(isImage(image, Format.Identifier.JPG));
		assertFalse(isImage(image, Format.Identifier.WEB));
		assertFalse(isImage(audio, Format.Identifier.MP3));
		assertFalse(isImage(video, Format.Identifier.MPG));
		assertTrue(isImage(webImage, Format.Identifier.WEB));
		assertFalse(isImage(webImage, Format.Identifier.JPG));
		assertFalse(isImage(webAudio, Format.Identifier.WEB));
		assertFalse(isImage(webVideo, Format.Identifier.WEB));
	}

	@Test
	public void testIsAudio2() {
		assertFalse(isAudio(image, Format.Identifier.JPG));
		assertTrue(isAudio(audio, Format.Identifier.MP3));
		assertFalse(isAudio(audio, Format.Identifier.WEB));
		assertFalse(isAudio(video, Format.Identifier.MPG));
		assertFalse(isAudio(webImage, Format.Identifier.WEB));
		assertTrue(isAudio(webAudio, Format.Identifier.WEB));
		assertFalse(isAudio(webAudio, Format.Identifier.MP3));
		assertFalse(isAudio(webVideo, Format.Identifier.WEB));
	}

	@Test
	public void testIsVideo2() {
		assertFalse(isVideo(image, Format.Identifier.JPG));
		assertFalse(isVideo(audio, Format.Identifier.MP3));
		assertTrue(isVideo(video, Format.Identifier.MPG));
		assertFalse(isVideo(video, Format.Identifier.WEB));
		assertFalse(isVideo(webImage, Format.Identifier.WEB));
		assertFalse(isVideo(webAudio, Format.Identifier.WEB));
		assertTrue(isVideo(webVideo, Format.Identifier.WEB));
		assertFalse(isVideo(webVideo, Format.Identifier.MPG));
	}

	@Test
	public void testIsWebAudio() {
		assertFalse(isWebAudio(image));
		assertFalse(isWebAudio(audio));
		assertFalse(isWebAudio(video));
		assertFalse(isWebAudio(webImage));
		assertTrue(isWebAudio(webAudio));
		assertFalse(isWebAudio(webVideo));
	}

	@Test
	public void testIsWebVideo() {
		assertFalse(isWebVideo(image));
		assertFalse(isWebVideo(audio));
		assertFalse(isWebVideo(video));
		assertFalse(isWebVideo(webImage));
		assertFalse(isWebVideo(webAudio));
		assertTrue(isWebVideo(webVideo));
	}

	private static File getNonExistingFile(String initialname) {
		String basename = FileUtil.getFileNameWithoutExtension(initialname);
		String extension = FileUtil.getExtension(initialname);
		Random random = new Random();
		File result = new File(initialname);
		while (result.exists()) {
			result = new File(basename + random.nextInt() + "." + extension);
		}
		return result;
	}

}
