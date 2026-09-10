package net.pms.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Translates absolute media paths into paths relative to the shared folder that
 * holds them, and back, in case the same media library is not mounted at the same place
 *
 * Relative paths always use '/' as separator, whatever platform wrote them, so
 * a backup taken on one platform can be read on another.
 */
public final class RelativeMediaPath {

	public static final String ROOT_PATH = ".";
	private static final char SEPARATOR = '/';
	private static final boolean CASE_INSENSITIVE = File.separatorChar == '\\';

	/**
	 * An absolute path expressed relative to one of the shared folders.
	 */
	public record Relative(int rootIndex, String path) {
	}

	private RelativeMediaPath() {
	}

	/**
	 * Only file system keys can be relativized.
	 * @return true if the key is an absolute file system path
	 */
	public static boolean isFileSystemPath(String key) {
		return key != null && !key.isEmpty() && new File(key).isAbsolute();
	}

	/**
	 * Expresses an absolute path relative to the shared folder that holds it.
	 *
	 * @return the relative path, or NULL if no shared folder holds the path
	 */
	public static Relative relativize(String path, List<File> roots) {
		if (path == null || roots == null) {
			return null;
		}
		String normalized = normalize(path);
		Relative best = null;
		int bestPrefixLength = -1;
		for (int index = 0; index < roots.size(); index++) {
			File root = roots.get(index);
			if (root == null) {
				continue;
			}
			String rootPath = normalize(root.getAbsolutePath());
			if (rootPath.isEmpty()) {
				continue;
			}
			String prefix = asPrefix(rootPath);
			String relative;
			if (isSamePath(normalized, rootPath)) {
				relative = ROOT_PATH;
			} else if (normalized.length() > prefix.length() && isSamePath(normalized.substring(0, prefix.length()), prefix)) {
				relative = normalized.substring(prefix.length());
			} else {
				continue;
			}
			if (prefix.length() > bestPrefixLength) {
				best = new Relative(index, relative);
				bestPrefixLength = prefix.length();
			}
		}
		return best;
	}

	/**
	 * Resolves a relative path against the shared folders that are configured
	 * now, and returns the absolute paths that really exist.
	 */
	public static List<String> resolve(String recordedRootPath, String relativePath, List<File> roots) {
		List<String> result = new ArrayList<>();
		if (relativePath == null || relativePath.isEmpty() || roots == null) {
			return result;
		}
		String recorded = recordedRootPath == null ? null : normalize(recordedRootPath);
		String recordedName = recorded == null ? null : getName(recorded);
		List<File> sameRoot = new ArrayList<>();
		List<File> sameName = new ArrayList<>();
		List<File> otherRoots = new ArrayList<>();
		for (File root : roots) {
			if (root == null) {
				continue;
			}
			String rootPath = normalize(root.getAbsolutePath());
			if (recorded != null && isSamePath(rootPath, recorded)) {
				sameRoot.add(root);
			} else if (recordedName != null && !recordedName.isEmpty() && isSamePath(getName(rootPath), recordedName)) {
				sameName.add(root);
			} else {
				otherRoots.add(root);
			}
		}
		for (List<File> tier : List.of(sameRoot, sameName, otherRoots)) {
			for (File root : tier) {
				File candidate = ROOT_PATH.equals(relativePath) ?
					root :
					new File(root, relativePath.replace(SEPARATOR, File.separatorChar));
				if (!candidate.exists()) {
					continue;
				}
				String key = ProcessUtil.getSystemPathName(candidate.getAbsolutePath());
				if (!result.contains(key)) {
					result.add(key);
				}
			}
			if (!result.isEmpty()) {
				return result;
			}
		}
		return result;
	}

	/**
	 * @return the path with '/' separators and without a trailing separator
	 */
	static String normalize(String path) {
		if (path == null) {
			return "";
		}
		String result = path.replace(File.separatorChar, SEPARATOR).replace('\\', SEPARATOR);
		while (result.length() > 1 && result.charAt(result.length() - 1) == SEPARATOR) {
			result = result.substring(0, result.length() - 1);
		}
		return result;
	}

	/**
	 * @return the normalized path ending with a separator, so it can be used as a prefix of the paths below it
	 */
	private static String asPrefix(String normalizedPath) {
		return normalizedPath.charAt(normalizedPath.length() - 1) == SEPARATOR ? normalizedPath : normalizedPath + SEPARATOR;
	}

	/**
	 * @return the last element of a normalized path
	 */
	private static String getName(String normalizedPath) {
		int separator = normalizedPath.lastIndexOf(SEPARATOR);
		return separator < 0 ? normalizedPath : normalizedPath.substring(separator + 1);
	}

	private static boolean isSamePath(String one, String other) {
		return CASE_INSENSITIVE ? one.equalsIgnoreCase(other) : one.equals(other);
	}
}

