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
import java.text.Normalizer;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
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
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();

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
		List<SortCriterion> sortCriterionsList = List.of(sortCriterions);
		Collections.reverse(sortCriterionsList);
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
		int sortMethod = CONFIGURATION.getSortMethod();
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

	public static void sortResourcesByTitle(List<StoreResource> resources, String lang) {
		sortResourcesByTitle(resources, true, lang);
	}

	public static void sortResourcesByTitle(List<StoreResource> resources, boolean asc, String lang) {
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
				String str1 = resources1.getLocalizedDisplayName(lang);
				String str2 = resources2.getLocalizedDisplayName(lang);
				if (PMS.getConfiguration().isIgnoreTheWordAandThe()) {
					str1 = str1 != null ? str1.replaceAll("^(?i)A[ .]|The[ .]", "").replaceAll("\\s{2,}", " ") : null;
					str2 = str2 != null ? str2.replaceAll("^(?i)A[ .]|The[ .]", "").replaceAll("\\s{2,}", " ") : null;
				}
				return compareToNormalizedString(str1, str2, asc);
			} else {
				return 0;
			}
		});
	}

	public static void sortResourcesByModifiedDate(List<StoreResource> resources, boolean asc) {
		Collections.sort(resources, (StoreResource resources1, StoreResource resources2) -> {
			if (resources1 instanceof SystemFileResource systemFileResource1 && resources2 instanceof SystemFileResource systemFileResource2) {
				if (resources1 instanceof StoreItem && resources2 instanceof StoreContainer) {
					return 1;
				} else if (resources1 instanceof StoreContainer && resources2 instanceof StoreItem) {
					return -1;
				}
				File file1 = systemFileResource1.getSystemFile();
				File file2 = systemFileResource2.getSystemFile();
				if (file1 == null || file2 == null) {
					return 0;
				}
				if (asc) {
					return Long.compare(file1.lastModified(), file2.lastModified());
				} else {
					return Long.compare(file2.lastModified(), file1.lastModified());
				}
			} else {
				return 0;
			}
		});
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

	private static int compareToNormalizedString(String str1, String str2, boolean asc) {
		if (str2 == null) {
			return 1;
		} else if (str1 == null) {
			return -1;
		}
		str1 = Normalizer.normalize(str1, Normalizer.Form.NFKD);
		str2 = Normalizer.normalize(str2, Normalizer.Form.NFKD);
		if (asc) {
			return str1.compareToIgnoreCase(str2);
		} else {
			return str2.compareToIgnoreCase(str1);
		}
	}

}
