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

import java.text.Normalizer;
import java.util.Collections;
import java.util.List;
import net.pms.PMS;
import net.pms.store.StoreResource;
import org.jupnp.support.model.SortCriterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Surf@ceS
 */
public class StoreResourceSorter {

	private static final Logger LOGGER = LoggerFactory.getLogger(StoreResourceSorter.class);

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

	public static void sortResourcesByTitle(List<StoreResource> resources) {
		sortResourcesByTitle(resources, true, null);
	}

	public static void sortResourcesByTitle(List<StoreResource> resources, String lang) {
		sortResourcesByTitle(resources, true, lang);
	}

	public static void sortResourcesByTitle(List<StoreResource> resources, boolean asc, String lang) {
		Collections.sort(resources, (StoreResource resources1, StoreResource resources2) -> {
			String str1 = resources1.getLocalizedDisplayName(lang);
			String str2 = resources2.getLocalizedDisplayName(lang);
			if (PMS.getConfiguration().isIgnoreTheWordAandThe()) {
				str1 = str1 != null ? str1.replaceAll("^(?i)A[ .]|The[ .]", "").replaceAll("\\s{2,}", " ") : null;
				str2 = str2 != null ? str2.replaceAll("^(?i)A[ .]|The[ .]", "").replaceAll("\\s{2,}", " ") : null;
			}
			return compareToNormalizedString(str1, str2, asc);
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
