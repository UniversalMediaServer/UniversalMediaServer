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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;
import net.pms.PMS;
import net.pms.store.StoreContainer;
import net.pms.store.StoreItem;
import net.pms.store.StoreResource;
import net.pms.store.SystemFileResource;
import org.jupnp.support.model.SortCriterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Surf@ceS
 */
public class StoreResourceSorter {

	private static final Logger LOGGER = LoggerFactory.getLogger(StoreResourceSorter.class);
	private static final Pattern TITLE_ARTICLES = Pattern.compile("^(?i)A[ .]|The[ .]");
	private static final Pattern REPEATED_WHITESPACE = Pattern.compile("\\s{2,}");

	// Sort constants
	// Sort by title ascending, with compatibility decomposition (all accent/special char handled).
	public static final int SORT_TITLE_ASC = 0;
	// Sort by modified date, newest first
	public static final int SORT_DATE_MOD_DESC = 1;
	// Sort by modified date, oldest first
	public static final int SORT_DATE_MOD_ASC = 2;
	// Randomly sort
	public static final int SORT_RANDOM = 5;
	// No sorting
	public static final int SORT_NO_SORT = 6;

	/**
	 * This class is not meant to be instantiated.
	 */
	private StoreResourceSorter() {
	}

	public static void sortResources(List<StoreResource> resources, SortCriterion[] sortCriterions) {
		sortResources(resources, sortCriterions, null);
	}

	public static void sortResources(List<StoreResource> resources, SortCriterion[] sortCriterions, String lang) {
		Collections.reverse(Arrays.asList(sortCriterions));
		List<SortCriterion> sortCriterionsList = List.of(sortCriterions);
		for (SortCriterion sortCriterion : sortCriterionsList) {
			switch (sortCriterion.getPropertyName()) {
				case "dc:title" -> {
					sortResourcesByTitle(resources, true, lang);
				}
				case "upnp:class" -> {
					sortResourcesByUpnpClass(resources, true, lang);
				}
				case "dc:creator" -> {
					sortResourcesByCreator(resources, true, lang);
				}
				case "upnp:artist" -> {
					sortResourcesByArtist(resources, true, lang);
				}
				case "upnp:album" -> {
					sortResourcesByAlbum(resources, true, lang);
				}
				case "upnp:genre" -> {
					sortResourcesByGenre(resources, true, lang);
				}
				default -> {
					LOGGER.debug("StoreResourceSorter unhandled criterion \"{}\"", sortCriterion.getPropertyName());
				}
			}
		}
	}

	public static void sortResourcesByDefault(List<StoreResource> resources) {
		sortResourcesByDefault(resources, null);
	}

	public static void sortResourcesByDefault(List<StoreResource> resources, String lang) {
		int sortMethod = PMS.getConfiguration().getSortMethod();
		switch (sortMethod) {
			case SORT_TITLE_ASC -> {
				// Default value.
				// By title ascending, with compatibility decomposition (all accent/special char handled).
				sortResourcesByTitle(resources, lang);
			}
			case SORT_NO_SORT -> {
				// no sorting
			}
			case SORT_DATE_MOD_DESC -> {
				// Sort by modified date, newest first
				sortResourcesByModifiedDate(resources, false);
			}
			case SORT_DATE_MOD_ASC -> {
				// Sort by modified date, oldest first
				sortResourcesByModifiedDate(resources, true);
			}
			case SORT_RANDOM -> {
				// Random
				Collections.shuffle(resources, new Random(System.currentTimeMillis()));
			}
			default -> {
				// default.
				sortResourcesByTitle(resources, lang);
			}
		}
	}

	public static void sortResourcesByTitle(List<StoreResource> resources) {
		sortResourcesByTitle(resources, true, null);
	}

	private static void sortResourcesByTitle(List<StoreResource> resources, String lang) {
		sortResourcesByTitle(resources, true, lang);
	}

	private static void sortResourcesByTitle(List<StoreResource> resources, boolean asc, String lang) {
		Map<StoreResource, String> titles = new IdentityHashMap<>();
		boolean ignoreArticles = PMS.getConfiguration().isIgnoreTheWordAandThe();
		Collections.sort(resources, (StoreResource resources1, StoreResource resources2) -> {
			if (resources1 instanceof StoreResource && resources2 instanceof StoreResource) {
				if (resources1 instanceof StoreItem && resources2 instanceof StoreContainer) {
					return 1;
				} else if (resources1 instanceof StoreContainer && resources2 instanceof StoreItem) {
					return -1;
				}
				if (!resources1.isSortable()) {
					if (!resources2.isSortable()) {
						return 0;
					}
					return asc ? -1 : 1;
				} else if (!resources2.isSortable()) {
					return asc ? 1 : -1;
				}
				String str1 = getTitleSortKey(resources1, lang, ignoreArticles, titles);
				String str2 = getTitleSortKey(resources2, lang, ignoreArticles, titles);
				return compareStrings(str1, str2, asc);
			} else {
				return 0;
			}
		});
	}

	private static void sortResourcesByModifiedDate(List<StoreResource> resources, boolean asc) {
		Map<File, Long> modifiedTimes = new HashMap<>();
		try {
			Collections.sort(resources, (StoreResource resources1, StoreResource resources2) -> {
				if (resources1 instanceof SystemFileResource systemFileResource1 && resources2 instanceof SystemFileResource systemFileResource2) {
					if (resources1 instanceof StoreItem && resources2 instanceof StoreContainer) {
						return 1;
					} else if (resources1 instanceof StoreContainer && resources2 instanceof StoreItem) {
						return -1;
					}
					File file1 = systemFileResource1.getSystemFile();
					File file2 = systemFileResource2.getSystemFile();
					if (file2 == null) {
						return file1 == null ? 0 : 1;
					} else if (file1 == null) {
						return -1;
					}
					long lastModified1 = modifiedTimes.computeIfAbsent(file1, StoreResourceSorter::getFileLastModifiedTime);
					long lastModified2 = modifiedTimes.computeIfAbsent(file2, StoreResourceSorter::getFileLastModifiedTime);
					if (asc) {
						return Long.compare(lastModified1, lastModified2);
					} else {
						return Long.compare(lastModified2, lastModified1);
					}
				} else {
					if (resources1 instanceof StoreItem && resources2 instanceof StoreContainer) {
						return 1;
					} else if (resources1 instanceof StoreContainer && resources2 instanceof StoreItem) {
						return -1;
					}
					return 0;
				}
			});
		} catch (IllegalArgumentException e) {
			LOGGER.trace("sortResourcesByModifiedDate error: {}", e.getMessage());
		}
	}

	public static void sortResourcesByUpnpClass(List<StoreResource> resources, boolean asc, String lang) {
		//to implement
	}

	public static void sortResourcesByCreator(List<StoreResource> resources, boolean asc, String lang) {
		//to implement
	}

	public static void sortResourcesByArtist(List<StoreResource> resources, boolean asc, String lang) {
		//to implement
	}

	public static void sortResourcesByAlbum(List<StoreResource> resources, boolean asc, String lang) {
		//to implement
	}

	public static void sortResourcesByGenre(List<StoreResource> resources, boolean asc, String lang) {
		//todo implement lang
		Collections.sort(resources, (StoreResource resources1, StoreResource resources2) -> compareToNormalizedString(resources1.getGenre(), resources2.getGenre(), asc));
	}

	private static long getFileLastModifiedTime(File file) {
		Path path;
		try {
			path = file.toPath();
		} catch (InvalidPathException e) {
			LOGGER.trace("Invalid path for file \"{}\": {}", file.toString(), e.getMessage());
			return 0;
		}
		try {
			return Files.getLastModifiedTime(path).toMillis();
		} catch (IOException | SecurityException e) {
			LOGGER.trace("getLastModifiedTime thrown an error for \"{}\" ({}): {}", path.toString(), file.toString(), e.getMessage());
			return 0;
		}
	}

	private static String getTitleSortKey(StoreResource resource, String lang, boolean ignoreArticles, Map<StoreResource, String> titles) {
		String title = titles.get(resource);
		if (title == null && !titles.containsKey(resource)) {
			title = resource.getLocalizedDisplayName(lang);
			if (title != null) {
				if (ignoreArticles) {
					title = REPEATED_WHITESPACE.matcher(TITLE_ARTICLES.matcher(title).replaceAll("")).replaceAll(" ");
				}
				title = Normalizer.normalize(title, Normalizer.Form.NFKD);
			}
			titles.put(resource, title);
		}
		return title;
	}

	private static int compareToNormalizedString(String str1, String str2, boolean asc) {
		return compareStrings(
				str1 == null ? null : Normalizer.normalize(str1, Normalizer.Form.NFKD),
				str2 == null ? null : Normalizer.normalize(str2, Normalizer.Form.NFKD), asc);
	}

	private static int compareStrings(String str1, String str2, boolean asc) {
		if (str2 == null) {
			return str1 == null ? 0 : 1;
		} else if (str1 == null) {
			return -1;
		}
		if (asc) {
			return str1.compareToIgnoreCase(str2);
		} else {
			return str2.compareToIgnoreCase(str1);
		}
	}

}
