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
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.*;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.dlna.*;
import net.pms.encoders.Engine;
import net.pms.encoders.EngineFactory;
import net.pms.formats.Format;
import net.pms.formats.v2.SubtitleType;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapperImpl;
import net.pms.renderers.Renderer;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UMSUtils {

	private static final Collator COLLATOR;
	private static final Logger LOGGER = LoggerFactory.getLogger(UMSUtils.class);

	static {
		COLLATOR = Collator.getInstance();
		COLLATOR.setStrength(Collator.PRIMARY);
	}

	/**
	 * This class is not meant to be instantiated.
	 */
	private UMSUtils() {
	}

	/**
	 * Filters the list of resources in-place by removing all items that do
	 * not match or contain searchString.
	 *
	 * @param resources
	 * @param searchString
	 * @param isExpectOneResult whether to only return one result
	 * @param isExactMatch whether to only return exact matches
	 */
	public static void filterResourcesByName(List<DLNAResource> resources, String searchString, boolean isExpectOneResult, boolean isExactMatch) {
		if (resources == null || searchString == null) {
			return;
		}
		searchString = searchString.toLowerCase();
		for (int i = resources.size() - 1; i >= 0; i--) {
			DLNAResource res = resources.get(i);

			if (res.isSearched()) {
				continue;
			}

			boolean keep;
			if (isExactMatch) {
				keep = res.getName().toLowerCase().equals(searchString);
			} else {
				keep = res.getName().toLowerCase().contains(searchString);
			}

			final DLNAMediaInfo media = res.getMedia();

			if (keep && isExpectOneResult) {
				resources.clear();
				resources.add(res);
				break;
			}

			if (!keep) {
				if (media != null && media.getAudioTracksList() != null) {
					for (int j = 0; j < media.getAudioTracksList().size(); j++) {
						DLNAMediaAudio audio = media.getAudioTracksList().get(j);
						if (audio.getAlbum() != null) {
							keep |= audio.getAlbum().toLowerCase().contains(searchString);
						}
						//TODO maciekberry: check whether it makes sense to use Album Artist
						if (audio.getArtist() != null) {
							keep |= audio.getArtist().toLowerCase().contains(searchString);
						}
						if (audio.getSongname() != null) {
							keep |= audio.getSongname().toLowerCase().contains(searchString);
						}
					}
				}
				resources.remove(i);
			}
		}
	}

	// Sort constants
	public static final int SORT_LOC_SENS = 0;
	public static final int SORT_MOD_NEW = 1;
	public static final int SORT_MOD_OLD = 2;
	public static final int SORT_INS_ASCII = 3;
	public static final int SORT_LOC_NAT = 4;
	public static final int SORT_RANDOM = 5;
	public static final int SORT_NO_SORT = 6;

	/**
	 * Sorts a list of files using a custom method when the files are not episodes
	 * within TV series folders of the Media Library.
	 *
	 * @param files
	 * @param method
	 * @see #sort(java.util.ArrayList, int)
	 */
	public static void sortFiles(List<File> files, int method) {
		UMSUtils.sortFiles(files, method, false);
	}

	/**
	 * Sorts a list of files using a custom method.
	 *
	 * @param files
	 * @param method
	 * @param isEpisodeWithinTVSeriesFolder
	 * @see #sort(java.util.ArrayList, int)
	 */
	public static void sortFiles(List<File> files, int method, final boolean isEpisodeWithinTVSeriesFolder) {
		switch (method) {
			case SORT_NO_SORT: // no sorting
				break;
			case SORT_LOC_NAT: // Locale-sensitive natural sort
				Collections.sort(files, (File f1, File f2) -> {
					String filename1ToSort = FileUtil.renameForSorting(f1.getName(), isEpisodeWithinTVSeriesFolder, f1.getAbsolutePath());
					String filename2ToSort = FileUtil.renameForSorting(f2.getName(), isEpisodeWithinTVSeriesFolder, f2.getAbsolutePath());
					return NaturalComparator.compareNatural(COLLATOR, filename1ToSort, filename2ToSort);
				});
				break;
			case SORT_INS_ASCII: // Case-insensitive ASCIIbetical sort
				Collections.sort(files, (File f1, File f2) -> {
					String filename1ToSort = FileUtil.renameForSorting(f1.getName(), isEpisodeWithinTVSeriesFolder, f1.getAbsolutePath());
					String filename2ToSort = FileUtil.renameForSorting(f2.getName(), isEpisodeWithinTVSeriesFolder, f2.getAbsolutePath());
					return filename1ToSort.compareToIgnoreCase(filename2ToSort);
				});
				break;
			case SORT_MOD_OLD: // Sort by modified date, oldest first
				Collections.sort(files, (File f1, File f2) -> Long.valueOf(f1.lastModified()).compareTo(f2.lastModified()));
				break;
			case SORT_MOD_NEW: // Sort by modified date, newest first
				Collections.sort(files, (File f1, File f2) -> Long.valueOf(f2.lastModified()).compareTo(f1.lastModified()));
				break;
			case SORT_RANDOM: // Random
				Collections.shuffle(files, new Random(System.currentTimeMillis()));
				break;
			case SORT_LOC_SENS: // Same as default
			default: // Locale-sensitive A-Z
				Collections.sort(files, (File f1, File f2) -> {
					String filename1ToSort = FileUtil.renameForSorting(f1.getName(), isEpisodeWithinTVSeriesFolder, f1.getAbsolutePath());
					String filename2ToSort = FileUtil.renameForSorting(f2.getName(), isEpisodeWithinTVSeriesFolder, f2.getAbsolutePath());
					return COLLATOR.compare(filename1ToSort, filename2ToSort);
				});
				break;
		}
	}

	/**
	 * Sorts a list of strings using a custom method.
	 *
	 * @param inputStrings
	 * @param method
	 * @see #sortFiles(java.util.List, int)
	 */
	public static void sortStrings(List<String> inputStrings, int method) {
		switch (method) {
			case SORT_NO_SORT: // no sorting
				break;
			case SORT_LOC_NAT: // Locale-sensitive natural sort
				Collections.sort(inputStrings, (String s1, String s2) -> {
					String filename1ToSort = FileUtil.renameForSorting(s1);
					String filename2ToSort = FileUtil.renameForSorting(s2);
					return NaturalComparator.compareNatural(COLLATOR, filename1ToSort, filename2ToSort);
				});
				break;
			case SORT_INS_ASCII: // Case-insensitive ASCIIbetical sort
				Collections.sort(inputStrings, (String s1, String s2) -> {
					String filename1ToSort = FileUtil.renameForSorting(s1);
					String filename2ToSort = FileUtil.renameForSorting(s2);
					return filename1ToSort.compareToIgnoreCase(filename2ToSort);
				});
				break;
			case SORT_RANDOM: // Random
				Collections.shuffle(inputStrings, new Random(System.currentTimeMillis()));
				break;
			case SORT_LOC_SENS: // Same as default
			default: // Locale-sensitive A-Z
				Collections.sort(inputStrings, (String s1, String s2) -> {
					String filename1ToSort = FileUtil.renameForSorting(s1);
					String filename2ToSort = FileUtil.renameForSorting(s2);
					return COLLATOR.compare(filename1ToSort, filename2ToSort);
				});
				break;
		}
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

	public static String getLangList(Renderer renderer) {
		return getLangList(renderer, false);
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
	 * A DLNAResource list with built-in file i/o.
	 */
	public static class IOList extends ArrayList<DLNAResource> {
		/**
		 * Bitwise constants relating to playlist management.
		 */
		public static final int PERMANENT = 1;
		public static final int AUTOSAVE = 2;
		public static final int AUTOREMOVE = 4;
		private static final long serialVersionUID = 8042924548275374060L;
		private File file;
		private final int mode;

		public IOList(String uri, int mode) {
			this.mode = mode;
			if (!StringUtils.isBlank(uri)) {
				file = new File(uri);
				load(file);
			} else {
				file = null;
			}
		}

		@Override
		public boolean add(DLNAResource d) {
			super.add(d);
			if (isMode(AUTOSAVE)) {
				save();
			}
			return true;
		}

		@Override
		public DLNAResource remove(int index) {
			DLNAResource d = super.remove(index);
			if (isMode(AUTOSAVE)) {
				save();
			}
			return d;
		}

		public boolean isMode(int m) {
			return (mode & m) == m;
		}

		public File getFile() {
			return file;
		}

		public final void load(File f) {
			if (f.exists()) {
				file = f;
				clear();
				try {
					read(this, f);
				} catch (IOException e) {
					LOGGER.debug("Error loading resource list '{}': {}", f, e);
				}
				if (PMS.getConfiguration().getSortMethod(f) == UMSUtils.SORT_RANDOM) {
					Collections.shuffle(this);
				}
			}
		}

		public void save() {
			if (file != null) {
				save(file);
			}
		}

		public void save(File f) {
			if (!isEmpty()) {
				try {
					write(this, f);
				} catch (IOException e) {
					LOGGER.debug("Error saving resource list to '{}': {}", f, e);
				}
			} else if (f != null && f.exists()) {
				// Save = delete for empty playlists
				f.delete();
			}
		}

		public static void write(List<DLNAResource> playlist, File f) throws IOException {
			Date now = new Date();
			try (FileWriter out = new FileWriter(f)) {
				StringBuilder sb = new StringBuilder();
				sb.append("######\n");
				sb.append("## __UPS__\n");
				sb.append("## NOTE!!!!!\n");
				sb.append("## This file is auto generated\n");
				sb.append("## Edit with EXTREME care\n");
				sb.append("## Generated: ");
				sb.append(now.toString());
				sb.append("\n");
				for (DLNAResource r : playlist) {
					String data = r.write();
					if (!StringUtils.isEmpty(data) && sb.indexOf(data) == -1) {
						String id = "internal:" + r.getClass().getName();

						sb.append("master:").append(id).append(';');
						if (r.getEngine() != null) {
							sb.append("player:").append(r.getEngine().toString()).append(';');
						}
						if (r.isResume()) {
							sb.append("resume");
							sb.append(r.getResume().getResumeFile().getAbsolutePath());
							sb.append(';');
						}
						if (r.getMediaSubtitle() != null) {
							DLNAMediaSubtitle sub = r.getMediaSubtitle();
							if (
								sub.getLang() != null &&
								(
									(
										sub.isExternal() &&
										sub.getExternalFile() != null
									) || (
										sub.isEmbedded() &&
										sub.getId() != -1
									)
								)
							) {
								sb.append("sub");
								sb.append(sub.getLang());
								sb.append(',');
								if (sub.isExternal()) {
									sb.append("file:");
									sb.append(sub.getExternalFile().getPath());
								} else {
									sb.append("id:");
									sb.append("").append(sub.getId());
								}
								sb.append(';');
							}
						}
						sb.append(data);
						sb.append("\n");
					}
				}
				out.write(sb.toString());
				out.flush();
			}
		}

		private static Engine findPlayerByName(String playerName, boolean onlyEnabled, boolean onlyAvailable) {
			for (Engine player : EngineFactory.getEngines(onlyEnabled, onlyAvailable)) {
				if (playerName.equals(player.getName())) {
					return player;
				}
			}
			return null;
		}

		private static DLNAResource parse(String clazz, String data) {
			boolean error = false;
			if (clazz.contains("RealFile")) {
				if (data.contains(">")) {
					String[] tmp = data.split(">");
					return new RealFile(new File(tmp[1]), tmp[0]);
				}
				error = true;
			}
			if (clazz.contains("SevenZipEntry")) {
				if (data.contains(">")) {
					String[] tmp = data.split(">");
					long len = Long.parseLong(tmp[2]);
					return new SevenZipEntry(new File(tmp[1]), tmp[0], len);
				}
				error = true;
			}
			if (clazz.contains("ZippedEntry")) {
				if (data.contains(">")) {
					String[] tmp = data.split(">");
					long len = Long.parseLong(tmp[2]);
					return new ZippedEntry(new File(tmp[1]), tmp[0], len);
				}
				error = true;
			}
			if (clazz.contains("WebStream")) {
				if (data.contains(">")) {
					String[] tmp = data.split(">");
					int type;
					try {
						type = Integer.parseInt(tmp[3]);
					} catch (NumberFormatException e) {
						type = Format.UNKNOWN;
					}
					return new WebStream(tmp[0], tmp[1], tmp[2], type);
				}
				error = true;
			}

			if (error) {
				LOGGER.debug("Error parsing playlist resource:");
				LOGGER.debug("clazz: " + clazz);
				LOGGER.debug("data:" + data);
			}

			return null;
		}

		public static void read(List<DLNAResource> playlist, File f) throws IOException {
			if (!f.exists()) {
				return;
			}
			try (BufferedReader in = new BufferedReader(new FileReader(f))) {
				String str;

				while ((str = in.readLine()) != null) {
					if (org.apache.commons.lang.StringUtils.isEmpty(str)) {
						continue;
					}
					if (str.startsWith("#")) {
						continue;
					}
					str = str.trim();
					if (!str.startsWith("master:")) {
						continue;
					}
					str = str.substring(7);
					int pos = str.indexOf(';');
					if (pos == -1) {
						continue;
					}
					String master = str.substring(0, pos);
					str = str.substring(pos + 1);
					pos = str.indexOf(';');
					String subData = null;
					String resData = null;
					DLNAResource res = null;
					Engine player = null;
					while (pos != -1) {
						if (str.startsWith("player:")) {
							// find last player
							player = findPlayerByName(str.substring(7, pos), true, true);
						}
						if (str.startsWith("resume")) {
							// resume data
							resData = str.substring(6, pos);
						}
						if (str.startsWith("sub")) {
							// subs data
							subData = str.substring(3, pos);
						}
						str = str.substring(pos + 1);
						pos = str.indexOf(';');
					}
					LOGGER.debug("master is " + master + " str " + str);
					if (master.startsWith("internal:")) {
						res = parse(master.substring(9), str);
					} else {
						LOGGER.warn("Unknown master parents: {}", master);
					}
					if (res != null) {
						if (resData != null) {
							ResumeObj r = new ResumeObj(new File(resData));
							if (!r.isDone()) {
								r.read();
								res.setResume(r);
							}
						}
						res.setEngine(player);
						if (subData != null) {
							DLNAMediaSubtitle s = res.getMediaSubtitle();
							if (s == null) {
								s = new DLNAMediaSubtitle();
								res.setMediaSubtitle(s);
							}
							String[] tmp = subData.split(",");
							s.setLang(tmp[0]);
							subData = tmp[1];
							if (subData.startsWith("file:")) {
								String sFile = subData.substring(5);
								s.setExternalFile(new File(sFile));
								SubtitleType t = SubtitleType.valueOfFileExtension(FileUtil.getExtension(sFile));
								s.setType(t);
							} else if (subData.startsWith("id:")) {
								s.setId(Integer.parseInt(subData.substring(3)));
							}
						}
						playlist.add(res);
					}
				}
			}
		}
	}

	/**
	 * Check available GPU decoding acceleration methods possibly used by
	 * FFmpeg.
	 *
	 * @param configuration in which the available GPU acceleration methods will
	 *            be stored
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
			new String[] {configuration.getFFmpegPath(), "-hwaccels"}, false, outputParams, true, false);
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
				if (line.equals("Hardware acceleration methods:")) {
					continue;
				} else {
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

		if ((a == null && b != null) || (a != null && b == null) || (a.size() != b.size())) {
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
	 * The keys are in the format Mantine expects, which can
	 * be confusing - "value" in Mantine is what is usually
	 * referred to as the "key".
	 *
	 * @param values
	 * @param labels
	 * @param jsonArray optional array to add to
	 * @return an array of objects in the format:
	 * [
	 *   {
	 *     "value": "bar",
	 *     "label": "foo"
	 *   },
	 *   ...
	 * ]
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
	 * The keys are in the format Mantine expects, which can
	 * be confusing - "value" in Mantine is what is usually
	 * referred to as the "key".
	 *
	 * @param values
	 * @param labels
	 * @param jsonArray optional array to add to
	 * @return an array of objects in the format:
	 * [
	 *   {
	 *     "value": "bar",
	 *     "label": "foo"
	 *   },
	 *   ...
	 * ]
	 */
	public static synchronized JsonArray getListsAsJsonArrayOfObjects(List<String> values, List<String> labels, JsonArray jsonArray) {
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
}
