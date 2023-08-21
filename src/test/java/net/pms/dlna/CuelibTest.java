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
package net.pms.dlna;

import ch.qos.logback.classic.Level;
import java.io.IOException;
import java.util.List;
import net.pms.logging.LoggingConfig;
import org.apache.commons.io.FileUtils;
import org.digitalmediaserver.cuelib.CueParser;
import org.digitalmediaserver.cuelib.CueSheet;
import org.digitalmediaserver.cuelib.FileData;
import org.digitalmediaserver.cuelib.TrackData;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CuelibTest {
	private final Class<?> CLASS = CuelibTest.class;

	@Test
	public void testReadCueFile() {
		LoggingConfig.setRootLevel(Level.TRACE);
		// force unbuffered if in trace mode
		LoggingConfig.setBuffered(false);

		Logger LOGGER = LoggerFactory.getLogger(CLASS);

		CueSheet sheet;
		try {
			sheet = CueParser.parse(FileUtils.toFile(CLASS.getResource("test.cue")), null);
		} catch (IOException e) {
			LOGGER.info("Error in parsing cue: " + e.getMessage());
			return;
		}
		List<FileData> files = sheet.getFileData();
		List<TrackData> tracks = files.get(0).getTrackData();
		TrackData track = tracks.get(0);
		assertTrue(track.getTitle().matches("Reverence"), "\"Reverence\" is valid title for track 01");
		assertTrue(track.getPerformer().matches("Faithless"), "\"Faithless\" is valid performer for track 01");
		track = tracks.get(1);
		assertTrue(track.getTitle().matches("She's My Baby"), "\"She's My Baby\" is valid title for track 02");
		assertTrue(track.getPerformer().matches("Noname"), "\"Noname\" is valid performer for track 02");
	}
}
