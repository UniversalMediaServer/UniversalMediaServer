/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008-2013 A. Brochard.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.util;

import ch.qos.logback.classic.LoggerContext;
import java.io.File;
import java.util.Random;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.RealFile;
import net.pms.dlna.WebStream;
import net.pms.formats.Format;
import static net.pms.util.PlayerUtil.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class PlayerUtilTest {
	private static DLNAResource video;
	private static DLNAResource audio;
	private static DLNAResource image;
	private static DLNAResource webImage;
	private static DLNAResource webVideo;
	private static DLNAResource webAudio;

	@BeforeClass
	public static void setUpClass() {
		// initialise the fixtures
		// XXX we need to call isValid to call checktype(), which is needed to initialise the format
		image = new RealFile(getNonExistingFile("test.jpg"));
		image.isValid();
		audio = new RealFile(getNonExistingFile("test.mp3"));
		audio.isValid();
		video = new RealFile(getNonExistingFile("test.mpg"));
		video.isValid();
		webImage = new WebStream("", "http://example.com/test.jpg", "", Format.IMAGE);
		webImage.isValid();
		webAudio = new WebStream("", "http://example.com/test.mp3", "", Format.AUDIO);
		webAudio.isValid();
		webVideo = new WebStream("", "http://example.com/test.mpg", "", Format.VIDEO);
		webVideo.isValid();
	}

	@Before
	public void setUp() {
		// Silence all log messages from the PMS code that is being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.reset();
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
