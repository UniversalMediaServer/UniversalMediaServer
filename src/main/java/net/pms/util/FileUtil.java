package net.pms.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.pms.PMS;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAMediaSubtitle;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

	public static File isFileExists(String f, String ext) {
		return isFileExists(new File(f), ext);
	}

	public static String getExtension(String f) {
		int point = f.lastIndexOf(".");
		if (point == -1) {
			return null;
		}
		return f.substring(point + 1);
	}

	public static String getFileNameWithoutExtension(String f) {
		int point = f.lastIndexOf(".");
		if (point == -1) {
			point = f.length();
		}
		return f.substring(0, point);
	}

	public static File getFileNameWitNewExtension(File parent, File f, String ext) {
		File ff = isFileExists(new File(parent, f.getName()), ext);
		if (ff != null && ff.exists()) {
			return ff;
		}
		return null;
	}

	public static File getFileNameWitAddedExtension(File parent, File f, String ext) {
		File ff = new File(parent, f.getName() + ext);
		if (ff.exists()) {
			return ff;
		}
		return null;
	}

	public static File isFileExists(File f, String ext) {
		int point = f.getName().lastIndexOf(".");
		if (point == -1) {
			point = f.getName().length();
		}
		File lowerCasedFilename = new File(f.getParentFile(), f.getName().substring(0, point) + "." + ext.toLowerCase());
		if (lowerCasedFilename.exists()) {
			return lowerCasedFilename;
		}

		File upperCasedFilename = new File(f.getParentFile(), f.getName().substring(0, point) + "." + ext.toUpperCase());
		if (upperCasedFilename.exists()) {
			return upperCasedFilename;
		}
		return null;
	}

	public static boolean doesSubtitlesExists(File file, DLNAMediaInfo media) {
		return doesSubtitlesExists(file, media, true);
	}

	public static boolean doesSubtitlesExists(File file, DLNAMediaInfo media, boolean usecache) {
		boolean found = browseFolderForSubtitles(file.getParentFile(), file, media, usecache);
		String alternate = PMS.getConfiguration().getAlternateSubsFolder();
		if (StringUtils.isNotBlank(alternate)) { // https://code.google.com/p/ps3mediaserver/issues/detail?id=737#c5
			File subFolder = new File(alternate);
			if (!subFolder.isAbsolute()) {
				subFolder = new File(file.getParent() + "/" + alternate);
				try {
					subFolder = subFolder.getCanonicalFile();
				} catch (IOException e) {
					LOGGER.debug("Caught exception", e);
				}
			}
			if (subFolder.exists()) {
				found = found || browseFolderForSubtitles(subFolder, file, media, usecache);
			}
		}
		return found;
	}
	static Map<File, File[]> cache;

	private synchronized static boolean browseFolderForSubtitles(File subFolder, File file, DLNAMediaInfo media, boolean usecache) {
		boolean found = false;
		if (!usecache) {
			cache = null;
		}
		if (cache == null) {
			cache = new HashMap<File, File[]>();
		}
		File allSubs[] = cache.get(subFolder);
		if (allSubs == null) {
			allSubs = subFolder.listFiles();
			if (allSubs != null) {
				cache.put(subFolder, allSubs);
			}
		}
		String fileName = getFileNameWithoutExtension(file.getName()).toLowerCase();
		if (allSubs != null) {
			for (File f : allSubs) {
				if (f.isFile() && !f.isHidden()) {
					String fName = f.getName().toLowerCase();
					for (int i = 0; i < DLNAMediaSubtitle.subExtensions.length; i++) {
						String ext = DLNAMediaSubtitle.subExtensions[i];
						if (fName.length() > ext.length() && fName.startsWith(fileName) && fName.endsWith("." + ext)) {
							int a = fileName.length();
							int b = fName.length() - ext.length() - 1;
							String code = "";
							if (a <= b) // handling case with several dots: <video>..<extension>
							{
								code = fName.substring(a, b);
							}
							if (code.startsWith(".")) {
								code = code.substring(1);
							}

							boolean exists = false;
							if (media != null) {
								for (DLNAMediaSubtitle sub : media.getSubtitlesCodes()) {
									if (f.equals(sub.getFile())) {
										exists = true;
									} else if (ext.equals("idx") && sub.getType() == DLNAMediaSubtitle.MICRODVD) { // VOBSUB
										sub.setType(DLNAMediaSubtitle.VOBSUB);
										exists = true;
									} else if (ext.equals("sub") && sub.getType() == DLNAMediaSubtitle.VOBSUB) { // VOBSUB
										sub.setFile(f);
										exists = true;
									}
								}
							}
							if (!exists) {
								DLNAMediaSubtitle sub = new DLNAMediaSubtitle();
								sub.setId(100 + (media == null ? 0 : media.getSubtitlesCodes().size())); // fake id, not used
								sub.setFile(f);
								sub.checkUnicode();
								if (code.length() == 0 || !Iso639.getCodeList().contains(code)) {
									sub.setLang(DLNAMediaSubtitle.UND);
									sub.setType(i + 1);
									if (code.length() > 0) {
										sub.setFlavor(code);
										if (sub.getFlavor().contains("-")) {
											String flavorLang = sub.getFlavor().substring(0, sub.getFlavor().indexOf("-"));
											String flavorTitle = sub.getFlavor().substring(sub.getFlavor().indexOf("-") + 1);
											if (Iso639.getCodeList().contains(flavorLang)) {
												sub.setLang(flavorLang);
												sub.setFlavor(flavorTitle);
											}
										}
									}
								} else {
									sub.setLang(code);
									sub.setType(i + 1);
								}
								found = true;
								if (media != null) {
									media.getSubtitlesCodes().add(sub);
								}
							}
						}
					}
				}
			}
		}
		return found;
	}
}
