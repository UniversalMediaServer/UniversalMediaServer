package net.pms.dlna;

import java.io.IOException;
import java.util.Map;
import net.pms.dlna.virtual.VirtualFolder;
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
			LOGGER.debug("add play sub child " + key + " rf " + rf);
			addChild(new PlaySub(OpenSubtitle.getName(key),
				OpenSubtitle.getLang(key),
				rf, (String) data.get(key))
			);
		}
	}
}
