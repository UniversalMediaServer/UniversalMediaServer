package net.pms.dlna;

import java.io.IOException;
import java.util.*;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.formats.v2.SubtitleType;
import net.pms.util.OpenSubtitle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubSelFile extends VirtualFolder {
	private static final Logger LOGGER = LoggerFactory.getLogger(SubSelFile.class);
	private DLNAResource orig;

	public SubSelFile(DLNAResource r) {
		super(r.getDisplayName(), r.getThumbnailURL());
		orig = r;
	}

	@Override
	public void discoverChildren() {
		Map<String, Object> data;
		try {
			if (orig instanceof RealFile) {
				RealFile rf = (RealFile) orig;
				data = OpenSubtitle.findSubs(rf.getFile());
			} else {
				data = OpenSubtitle.querySubs(orig.getDisplayName());
			}
		} catch (IOException e) {
			return;
		}
		if (data == null || data.isEmpty()) {
			return;
		}
		List<String> sortedKeys = new ArrayList<String>(data.keySet());
		Collections.sort(sortedKeys, new SubSort(PMS.getConfiguration()));
		for (String key : sortedKeys) {
			LOGGER.debug("Add play subtitle child " + key + " rf " + orig);
			DLNAMediaSubtitle sub = orig.getMediaSubtitle();
			if (sub == null) {
				sub = new DLNAMediaSubtitle();
			}
			String lang = OpenSubtitle.getLang(key);
			String name = OpenSubtitle.getName(key);
			sub.setType(SubtitleType.SUBRIP);
			sub.setId(1);
			sub.setLang(lang);
			sub.setLiveSub((String) data.get(key), OpenSubtitle.subFile(name + "_" + lang));
			DLNAResource nrf = orig.clone();
			nrf.setMediaSubtitle(sub);
			nrf.setSrtFile(true);
			addChild(nrf);
		}
	}

	private class SubSort implements Comparator<String> {
		private List<String> langs;

		SubSort(PmsConfiguration configuration) {
			langs = Arrays.asList(configuration.getSubtitlesLanguages().split(","));
		}

		@Override
		public int compare(String key1, String key2) {
			if (langs == null) {
				return 0;
			}
			Integer index1 = langs.indexOf(OpenSubtitle.getLang(key1));
			Integer index2 = langs.indexOf(OpenSubtitle.getLang(key2));

			if (index1 == -1) {
				index1 = 999;
			}

			if (index2 == -1) {
				index2 = 999;
			}

			return index1.compareTo(index2);
		}
	}
}
