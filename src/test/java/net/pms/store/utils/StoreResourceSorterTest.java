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
package net.pms.store.utils;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.pms.PMS;
import net.pms.TestHelper;
import net.pms.configuration.UmsConfiguration;
import net.pms.store.StoreContainer;
import net.pms.store.StoreItem;
import net.pms.store.StoreResource;
import net.pms.store.SystemFileResource;
import org.jupnp.support.model.SortCriterion;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StoreResourceSorterTest {
	private static UmsConfiguration configuration;
	@TempDir
	Path directory;

	@BeforeAll
	static void initialize() throws Exception {
		TestHelper.setLoggingOff();
		PMS.get();
		configuration = new UmsConfiguration(false);
		PMS.setConfiguration(configuration);
	}

	@BeforeEach
	void settings() {
		configuration.setIgnoreTheWordAandThe(true);
		configuration.setSortMethod(StoreResourceSorter.SORT_DATE_MOD_ASC);
	}

	@Test
	void titleOrderPreservesNullsUnsortableItemsAndStableTies() {
		var zebra = new Resource("The Zebra", null);
		var apple = new Resource("A Apple", null);
		var tie = new Resource("apple", null);
		var missing = new Resource(null, null);
		var heading = new Resource("Not sortable", null);
		heading.setSortable(false);
		var folder = new StoreContainer(null, "folder", null);
		List<StoreResource> resources = new ArrayList<>(List.of(zebra, apple, tie, missing, heading, folder));
		StoreResourceSorter.sortResourcesByTitle(resources);
		assertEquals(List.of(folder, heading, missing, apple, tie, zebra), resources);
		for (Resource resource : List.of(zebra, apple, tie, missing)) {
			assertEquals(1, resource.nameReads);
		}
		assertEquals(0, heading.nameReads);
	}

	@Test
	void normalizedTitleOrderMatchesPreviousRules() {
		for (boolean ignoreArticles : List.of(false, true)) {
			configuration.setIgnoreTheWordAandThe(ignoreArticles);
			List<StoreResource> resources = new ArrayList<>();
			for (String name : Arrays.asList("The Zebra", "Ａ  title", "A. Éclair", "Éclair", "E\u0301clair", "İstanbul", "istanbul", "Some The Title", "the  fox", null)) {
				resources.add(new Resource(name, null));
			}
			Collections.shuffle(resources, new Random(41));
			List<StoreResource> expected = new ArrayList<>(resources);
			expected.sort((a, b) -> legacyTitleCompare(((Resource) a).name, ((Resource) b).name, ignoreArticles));
			StoreResourceSorter.sortResourcesByTitle(resources);
			assertEquals(expected, resources);
			for (StoreResource resource : resources) {
				assertEquals(1, ((Resource) resource).nameReads);
			}
		}
	}

	@Test
	void titleKeysAreRefreshedForLanguageAndConfigurationChanges() {
		var first = new Resource("The Zebra", null);
		var second = new Resource("Apple", null);
		List<StoreResource> resources = new ArrayList<>(List.of(first, second));
		StoreResourceSorter.sortResourcesByTitle(resources);
		assertEquals(List.of(second, first), resources);
		first.name = "A Alpha";
		StoreResourceSorter.sortResourcesByTitle(resources);
		assertEquals(List.of(first, second), resources);
		first.frenchName = "Zèbre";
		second.frenchName = "Pomme";
		StoreResourceSorter.sortResources(resources, new SortCriterion[]{new SortCriterion(true, "dc:title")}, "fr");
		assertEquals(List.of(second, first), resources);
		assertEquals(3, first.nameReads);
		assertEquals(3, second.nameReads);
		configuration.setIgnoreTheWordAandThe(false);
		first.name = "The Apple";
		second.name = "Banana";
		StoreResourceSorter.sortResourcesByTitle(resources);
		assertEquals(List.of(second, first), resources);
		configuration.setIgnoreTheWordAandThe(true);
		StoreResourceSorter.sortResourcesByTitle(resources);
		assertEquals(List.of(first, second), resources);
	}

	@Test
	void dateSortReadsEachFileOnceAndRefreshesOnTheNextSort() throws Exception {
		List<Resource> expected = new ArrayList<>();
		for (int i = 0; i < 64; i++) {
			Path path = Files.createFile(directory.resolve("media-" + i));
			Files.setLastModifiedTime(path, FileTime.fromMillis(1700000000000L + i * 1000));
			expected.add(new Resource("media-" + i, new CountingFile(path)));
		}
		List<StoreResource> resources = new ArrayList<>(expected);
		Collections.shuffle(resources, new Random(29));
		StoreResourceSorter.sortResourcesByDefault(resources);
		assertEquals(expected, resources);
		assertEquals(64, expected.stream().mapToInt(r -> ((CountingFile) r.file).reads).sum());
		Files.setLastModifiedTime(directory.resolve("media-0"), FileTime.fromMillis(1700000100000L));
		configuration.setSortMethod(StoreResourceSorter.SORT_DATE_MOD_DESC);
		StoreResourceSorter.sortResourcesByDefault(resources);
		assertSame(expected.get(0), resources.get(0));
		assertSame(expected.get(63), resources.get(1));
		assertEquals(128, expected.stream().mapToInt(r -> ((CountingFile) r.file).reads).sum());
	}

	@Test
	void dateSortPreservesNullMissingFilesTiesAndFolderPriority() throws Exception {
		Path path = Files.createFile(directory.resolve("present"));
		var shared = new CountingFile(path);
		var first = new Resource("first", shared);
		var second = new Resource("second", shared);
		var missing = new Resource("missing", directory.resolve("missing").toFile());
		var noFile = new Resource("no file", null);
		var folder = new StoreContainer(null, "folder", null);
		List<StoreResource> resources = new ArrayList<>(List.of(first, second, missing, noFile, folder));
		StoreResourceSorter.sortResourcesByDefault(resources);
		assertEquals(List.of(folder, noFile, missing, first, second), resources);
		assertEquals(1, shared.reads);
	}

	static int legacyTitleCompare(String first, String second, boolean ignoreArticles) {
		if (ignoreArticles) {
			first = first == null ? null : first.replaceAll("^(?i)A[ .]|The[ .]", "").replaceAll("\\s{2,}", " ");
			second = second == null ? null : second.replaceAll("^(?i)A[ .]|The[ .]", "").replaceAll("\\s{2,}", " ");
		}
		if (second == null) {
			return first == null ? 0 : 1;
		} else if (first == null) {
			return -1;
		}
		return Normalizer.normalize(first, Normalizer.Form.NFKD).compareToIgnoreCase(Normalizer.normalize(second, Normalizer.Form.NFKD));
	}

	static class Resource extends StoreItem implements SystemFileResource {
		String name;
		String frenchName;
		final File file;
		int nameReads;

		Resource(String name, File file) {
			super(null);
			this.name = name;
			this.file = file;
		}

		@Override
		public String getLocalizedDisplayName(String lang) {
			nameReads++;
			return "fr".equals(lang) ? frenchName : name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getSystemName() {
			return name;
		}

		@Override
		public long length() {
			return 0;
		}

		@Override
		public boolean isValid() {
			return true;
		}

		@Override
		public InputStream getInputStream() {
			return null;
		}

		@Override
		public File getSystemFile() {
			return file;
		}
	}

	static final class CountingFile extends File {
		int reads;

		CountingFile(Path path) {
			super(path.toString());
		}

		@Override
		public Path toPath() {
			reads++;
			return super.toPath();
		}
	}
}
