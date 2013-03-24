package net.pms.dlna;

import java.io.IOException;
import java.util.Map;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.formats.v2.SubtitleType;
import net.pms.util.OpenSubtitle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubSelFile extends VirtualFolder {
	private static final Logger LOGGER = LoggerFactory.getLogger(SubSelFile.class);
	private RealFile rf;

	public SubSelFile(DLNAResource r) {
		this((RealFile) r);
	}

	public SubSelFile(RealFile file) {
		super(file.getDisplayName(), file.getThumbnailURL());
		rf = file;
	}

	@Override
	public void discoverChildren() {
		Map<String, Object> data;
		try {
			data = OpenSubtitle.findSubs(rf.getFile());
		} catch (IOException e) {
			return;
		}
		if (data == null || data.isEmpty()) {
			return;
		}
		for (String key : data.keySet()) {
			LOGGER.debug("Add play subtitle child " + key + " rf " + rf);
			DLNAMediaSubtitle sub = rf.getMediaSubtitle();
			if (sub == null) {
				sub = new DLNAMediaSubtitle();
			}
			String lang = OpenSubtitle.getLang(key);
			String name = OpenSubtitle.getName(key);
			sub.setType(SubtitleType.SUBRIP);
			sub.setId(1);
			sub.setLang(lang);
			sub.setLiveSub((String) data.get(key), OpenSubtitle.subFile(name + "_" + lang));
			RealFile nrf = new RealFile(rf.getFile(), name);
			nrf.setMediaSubtitle(sub);
			nrf.setSrtFile(true);
			addChild(nrf);
		}
	}
}
