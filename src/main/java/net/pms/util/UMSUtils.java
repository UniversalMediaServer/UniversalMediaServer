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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapperImpl;
import net.pms.media.MediaInfo;
import net.pms.media.audio.metadata.MediaAudioMetadata;
import net.pms.renderers.Renderer;
import net.pms.store.StoreResource;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

public class UMSUtils {
	private static final Pattern YOUTUBE_URL_PATTERN = Pattern.compile("(youtube\\.|youtu\\.be)");

	/**
	 * This class is not meant to be instantiated.
	 */
	private UMSUtils() {
	}

	/**
	 * Return the first resources that SystemName match the searched systemName.
	 *
	 * @param resources
	 * @param systemName
	 */
	public static StoreResource getFirstResourceWithSystemName(List<StoreResource> resources, String systemName) {
		if (resources != null && systemName != null) {
			for (StoreResource resource : resources) {
				if (systemName.equals(resource.getSystemName())) {
					return resource;
				}
			}
		}
		return null;
	}

	/**
	 * Filters the list of resources in-place by removing all items that do not
	 * match or contain searchString.
	 *
	 * @param resources
	 * @param searchString
	 * @param isExpectOneResult whether to only return one result
	 * @param isExactMatch whether to only return exact matches
	 */
	public static void filterResourcesByName(List<StoreResource> resources, String searchString, boolean isExpectOneResult, boolean isExactMatch) {
		if (resources == null || searchString == null) {
			return;
		}
		String normalizedSearchString = searchString.toLowerCase(Locale.ROOT);
		if (isExpectOneResult) {
			for (int i = resources.size() - 1; i >= 0; i--) {
				StoreResource resource = resources.get(i);
				if (!resource.isSearched() && resourceMatches(resource, normalizedSearchString, isExactMatch)) {
					resources.clear();
					resources.add(resource);
					return;
				}
			}
			List<StoreResource> searchedResources = new ArrayList<>();
			for (StoreResource resource : resources) {
				if (resource.isSearched()) {
					searchedResources.add(resource);
				}
			}
			resources.clear();
			resources.addAll(searchedResources);
			return;
		}

		List<StoreResource> matchingResources = new ArrayList<>(resources.size());
		for (StoreResource resource : resources) {
			if (resource.isSearched() || resourceMatches(resource, normalizedSearchString, isExactMatch)) {
				matchingResources.add(resource);
			}
		}
		resources.clear();
		resources.addAll(matchingResources);
	}

	private static boolean resourceMatches(StoreResource resource, String searchString, boolean exactMatch) {
		String resourceName = resource.getName().toLowerCase(Locale.ROOT);
		if (exactMatch ? resourceName.equals(searchString) : resourceName.contains(searchString)) {
			return true;
		}

		MediaInfo media = resource.getMediaInfo();
		if (media == null || !media.hasAudioMetadata()) {
			return false;
		}
		MediaAudioMetadata audioMetadata = media.getAudioMetadata();
		return containsIgnoreCase(audioMetadata.getAlbum(), searchString) ||
			containsIgnoreCase(audioMetadata.getArtist(), searchString) ||
			containsIgnoreCase(audioMetadata.getSongname(), searchString);
	}

	private static boolean containsIgnoreCase(String value, String normalizedSearchString) {
		return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedSearchString);
	}

	public static String playedDurationStr(String current, String duration) {
		String pos = StringUtil.shortTime(current, 4);
		String dur = StringUtil.shortTime(duration, 4);
		return pos + (pos.equals("0:00") ? "" : dur.equals("0:00") ? "" : (" / " + dur));
	}

	public static String unescape(String s) throws IllegalArgumentException {
		return StringEscapeUtils.unescapeXml(StringEscapeUtils.unescapeHtml4(URLDecoder.decode(s, StandardCharsets.UTF_8)));
	}

	private static String iso639(String s) {
		String[] tmp = s.split(",");
		StringBuilder res = new StringBuilder();
		String sep = "";
		for (String tmp1 : tmp) {
			res.append(sep).append(Iso639.getISO639_2Code(tmp1));
			sep = ",";
		}
		if (StringUtils.isNotEmpty(res)) {
			return res.toString();
		}
		return s;
	}

	public static String getLangList(Renderer renderer, boolean three) {
		String res;
		if (renderer != null) {
			res = renderer.getSubLanguage();
		} else {
			res = PMS.getConfiguration().getSubtitlesLanguages();
		}
		if (three) {
			res = iso639(res);
		}
		return res;
	}

	/**
	 * Check available GPU decoding acceleration methods possibly used by
	 * FFmpeg.
	 *
	 * @param configuration in which the available GPU acceleration methods will
	 * be stored
	 * @throws ConfigurationException
	 */
	public static void checkGPUDecodingAccelerationMethodsForFFmpeg(UmsConfiguration configuration) throws ConfigurationException {
		String ffmpegPath = configuration.getFFmpegPath();
		if (ffmpegPath == null) {
			return;
		}

		OutputParams outputParams = new OutputParams(configuration);
		outputParams.setWaitBeforeStart(0);
		outputParams.setLog(true);
		final ProcessWrapperImpl pw = new ProcessWrapperImpl(
				new String[]{configuration.getFFmpegPath(), "-hwaccels"}, false, outputParams, true, false);
		Runnable r = () -> {
			sleep(10000);

			pw.stopProcess();
		};

		Thread failsafe = new Thread(r, "Get GPU acceleration methods used by FFmpeg");
		failsafe.start();
		pw.run();
		List<String> result = pw.getOtherResults();
		List<String> availableMethods = new ArrayList<>(1);
		availableMethods.addAll(Arrays.asList("none"));
		availableMethods.add("auto");
		if (result != null) {
			for (String line : result) {
				line = line.trim();
				if (!line.equals("Hardware acceleration methods:")) {
					// fix duplicating GPU acceleration methods reported in
					// https://github.com/UniversalMediaServer/UniversalMediaServer/issues/1592
					if (!availableMethods.contains(line)) {
						availableMethods.add(line);
					}
				}
			}
		}

		configuration.setFFmpegAvailableGPUDecodingAccelerationMethods(availableMethods);
		configuration.save();
	}

	/**
	 * @see https://stackoverflow.com/a/19155453/2049714
	 * @param a first list to compare
	 * @param b second list to compare
	 * @return whether the lists are equal
	 */
	public static boolean isListsEqual(List<String> a, List<String> b) {
		// Check for sizes and nulls
		if (a == null && b == null) {
			return true;
		}

		if (a == null || b == null || (a.size() != b.size())) {
			return false;
		}

		// Sort and compare the two lists
		Collections.sort(a);
		Collections.sort(b);

		return a.equals(b);
	}

	/**
	 * Utility method to call {@link Thread#sleep(long)} without having to catch
	 * the InterruptedException.
	 *
	 * @param delay the delay
	 */
	public static void sleep(int delay) {
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
		}
	}

	/**
	 * The keys are in the format Mantine expects, which can be confusing -
	 * "value" in Mantine is what is usually referred to as the "key".
	 *
	 * @param values
	 * @param labels
	 * @param jsonArray optional array to add to
	 * @return an array of objects in the format: [ { "value": "bar", "label":
	 * "foo" }, ... ]
	 */
	public static JsonArray getArraysAsJsonArrayOfObjects(String[] values, String[] labels, JsonArray jsonArray) {
		if (jsonArray == null) {
			jsonArray = new JsonArray();
		}

		for (int i = 0; i < values.length; i++) {
			JsonObject objectGroup = new JsonObject();
			String value = values[i];
			String label = labels[i];
			objectGroup.addProperty("value", value);
			objectGroup.addProperty("label", label);
			jsonArray.add(objectGroup);
		}

		return jsonArray;
	}

	/**
	 * The keys are in the format Mantine expects, which can be confusing -
	 * "value" in Mantine is what is usually referred to as the "key".
	 *
	 * @param values
	 * @param labels
	 * @param jsonArray optional array to add to
	 * @return an array of objects in the format: [ { "value": "bar", "label":
	 * "foo" }, ... ]
	 */
	public static JsonArray getListsAsJsonArrayOfObjects(List<String> values, List<String> labels, JsonArray jsonArray) {
		if (jsonArray == null) {
			jsonArray = new JsonArray();
		}

		for (int i = 0; i < values.size(); i++) {
			JsonObject objectGroup = new JsonObject();
			String value = values.get(i);
			String label = labels.get(i);
			objectGroup.addProperty("label", label);
			objectGroup.addProperty("value", value);
			jsonArray.add(objectGroup);
		}

		return jsonArray;
	}

	public static boolean isYouTubeURL(String youTubeUrl) {
		Matcher matcher = YOUTUBE_URL_PATTERN.matcher(youTubeUrl);
		return matcher.find();
	}
}
