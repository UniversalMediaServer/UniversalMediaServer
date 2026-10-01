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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.renderers.Renderer;
import net.pms.store.item.RealFile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class StoreContainerResumeHashTest {

	private static Renderer renderer;

	@TempDir
	static Path tempDir;

	@BeforeAll
	public static void setUpClass() throws Exception {
		System.setProperty(PMS.PROPERTY_RUNNING_TESTS, "true");
		PMS.setConfiguration(new UmsConfiguration(false));
		renderer = new Renderer((String) null);
	}

	/**
	 * The resume file name contains the resume hash, so the same file must get
	 * the same hash when its folder is built again (e.g. after a restart).
	 */
	@Test
	public void testResumeHashIsStableAcrossContainers() throws IOException {
		File movie = Files.createFile(tempDir.resolve("movie.mkv")).toFile();

		RealFile first = addToNewContainer(movie);
		RealFile second = addToNewContainer(movie);

		assertNotEquals(0, first.resumeHash());
		assertEquals(first.resumeHash(), second.resumeHash());
	}

	@Test
	public void testResumeHashDiffersForSameNameInOtherFolder() throws IOException {
		File movie = Files.createFile(tempDir.resolve("episode.mkv")).toFile();
		File otherMovie = Files.createFile(Files.createDirectory(tempDir.resolve("other")).resolve("episode.mkv")).toFile();

		assertNotEquals(addToNewContainer(movie).resumeHash(), addToNewContainer(otherMovie).resumeHash());
	}

	private static RealFile addToNewContainer(File file) {
		StoreContainer container = new StoreContainer(renderer, "folder", null);
		RealFile item = new RealFile(renderer, file);
		container.addChild(item, true, false);
		return item;
	}

}
