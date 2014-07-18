package net.pms.util;

import java.io.File;
import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import net.pms.dlna.DLNAMediaAudio;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;

public class UMSUtils {
	private static final Collator collator;

	static {
		collator = Collator.getInstance();
		collator.setStrength(Collator.PRIMARY);
	}

	public static void postSearch(List<DLNAResource> files, String searchCriteria) {
		if (files == null || searchCriteria == null) {
			return;
		}
		searchCriteria = searchCriteria.toLowerCase();
		for (int i = files.size() - 1; i >= 0; i--) {
			DLNAResource res = files.get(i);

			if (res.isSearched()) {
				continue;
			}

			boolean keep = res.getName().toLowerCase().indexOf(searchCriteria) != -1;
			final DLNAMediaInfo media = res.getMedia();

			if (!keep && media != null && media.getAudioTracksList() != null) {
				for (int j = 0; j < media.getAudioTracksList().size(); j++) {
					DLNAMediaAudio audio = media.getAudioTracksList().get(j);
					if (audio.getAlbum() != null) {
						keep |= audio.getAlbum().toLowerCase().indexOf(searchCriteria) != -1;
					}
					if (audio.getArtist() != null) {
						keep |= audio.getArtist().toLowerCase().indexOf(searchCriteria) != -1;
					}
					if (audio.getSongname() != null) {
						keep |= audio.getSongname().toLowerCase().indexOf(searchCriteria) != -1;
					}
				}
			}

			if (!keep) { // dump it
				files.remove(i);
			}
		}
	}

	// Sort constants
	public static final int SORT_LOC_SENS =  0;
	public static final int SORT_MOD_NEW =   1;
	public static final int SORT_MOD_OLD =   2;
	public static final int SORT_INS_ASCII = 3;
	public static final int SORT_LOC_NAT =   4;
	public static final int SORT_RANDOM =    5;
	public static final int SORT_NO_SORT =   6;

	public static void sort(List<File> files, int method) {
		switch (method) {
			case SORT_NO_SORT: // no sorting
				break;
			case SORT_LOC_NAT: // Locale-sensitive natural sort
				Collections.sort(files, new Comparator<File>() {
					@Override
					public int compare(File f1, File f2) {
						String filename1ToSort = FileUtil.renameForSorting(f1.getName());
						String filename2ToSort = FileUtil.renameForSorting(f2.getName());

						return NaturalComparator.compareNatural(collator, filename1ToSort, filename2ToSort);
					}
				});
				break;
			case SORT_INS_ASCII: // Case-insensitive ASCIIbetical sort
				Collections.sort(files, new Comparator<File>() {
					@Override
					public int compare(File f1, File f2) {
						String filename1ToSort = FileUtil.renameForSorting(f1.getName());
						String filename2ToSort = FileUtil.renameForSorting(f2.getName());

						return filename1ToSort.compareToIgnoreCase(filename2ToSort);
					}
				});
				break;
			case SORT_MOD_OLD: // Sort by modified date, oldest first
				Collections.sort(files, new Comparator<File>() {
					@Override
					public int compare(File f1, File f2) {
						return Long.valueOf(f1.lastModified()).compareTo(Long.valueOf(f2.lastModified()));
					}
				});
				break;
			case SORT_MOD_NEW: // Sort by modified date, newest first
				Collections.sort(files, new Comparator<File>() {
					@Override
					public int compare(File f1, File f2) {
						return Long.valueOf(f2.lastModified()).compareTo(Long.valueOf(f1.lastModified()));
					}
				});
				break;
			case SORT_RANDOM: // Random
				Collections.shuffle(files, new Random(System.currentTimeMillis()));
				break;
			case SORT_LOC_SENS: // Same as default
			default: // Locale-sensitive A-Z
				Collections.sort(files, new Comparator<File>() {
					@Override
					public int compare(File f1, File f2) {
						String filename1ToSort = FileUtil.renameForSorting(f1.getName());
						String filename2ToSort = FileUtil.renameForSorting(f2.getName());

						return collator.compare(filename1ToSort, filename2ToSort);
					}
				});
				break;
		}
	}
}
