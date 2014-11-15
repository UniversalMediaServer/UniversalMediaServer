package net.pms.dlna;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.dlna.virtual.VirtualVideoAction;
import net.pms.encoders.Player;
import net.pms.encoders.PlayerFactory;
import net.pms.external.ExternalFactory;
import net.pms.external.ExternalListener;
import net.pms.formats.Format;
import net.pms.formats.v2.SubtitleType;
import net.pms.util.FileUtil;
import net.pms.util.UMSUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecentlyPlayed extends VirtualFolder {
	private static final Logger LOGGER = LoggerFactory.getLogger(RecentlyPlayed.class);
	private static final int MAX_LIST_SIZE = 250;
	private static final int DEF_LIST_SIZE = 50;
	private List<DLNAResource> list;

	public RecentlyPlayed() {
		super(Messages.getString("VirtualFolder.1"), "images/thumbnail-folder-256.png");
		list = Collections.synchronizedList(new ArrayList<DLNAResource>());
		parseLastFile();
	}

	private File lastFile() {
		return new File(PMS.getConfiguration().getDataFile("UMS.last"));
	}

	private int getMax() {
		String str = (String) PMS.getConfiguration().getCustomProperty("last_play_limit");
		if (str != null) {
			if (StringUtils.isEmpty(str)) {
				return DEF_LIST_SIZE;
			}
			int tmp = 0;
			try {
				tmp = Integer.parseInt(str);
			} catch (Exception e) {
			}
			if (tmp <= 0) { // use default
				return DEF_LIST_SIZE;
			}
			if (tmp > MAX_LIST_SIZE) {
				return MAX_LIST_SIZE;
			}
			// value seems to be ok, return it
			return tmp;
		}
		return DEF_LIST_SIZE;
	}

	public void add(DLNAResource res) {
		DLNAResource res1;
		LOGGER.debug("add " + res + " to last played " + res.getParent() + " " + this);
		if (res instanceof VirtualVideoAction) {
			// don't add these
			return;
		}
		if (res.getParent() == this) {
			res1 = res; // best guess
			for (DLNAResource r : list) {
				if (r.getName().equals(res.getName())
					&& r.getSystemName().equals(res.getSystemName())) {
					res1 = r;
					break;
				}
			}
		} else {
			String data = res.write();
			if (!StringUtils.isEmpty(data)
				&& res.getMasterParent() != null) {
				res1 = UMSUtils.resolveCreateMethod(res.getMasterParent(), data);
				res1.setMasterParent(res.getMasterParent());
				res1.setMediaSubtitle(res.getMediaSubtitle());
				res1.setResume(res.getResume());
			} else {
				res1 = res.clone();
				res1.setMediaSubtitle(res.getMediaSubtitle());
				res1.setResume(res.getResume());
			}
		}
		int max = getMax();
		list.remove(res1);
		if (list.size() == max) {
			list.remove(max - 1);
		}
		list.add(0, res1);
		setDiscovered(false);
		getChildren().clear();
		try {
			UMSUtils.writeResourcesToFile(lastFile(), list);
		} catch (IOException e) {
			LOGGER.debug("error dumping last file " + e);
		}
	}

	@Override
	public void discoverChildren() {
		for (DLNAResource r : list) {
			// addchild might clear the masterparent
			// so fetch it first and readd
			ExternalListener master = r.getMasterParent();
			addChild(r);
			r.setMasterParent(master);
			if (r.isResume()) {
				// add this non resume after
				DLNAResource clone = r.clone();
				clone.setResume(null);
				addChild(clone);
				clone.setMasterParent(master);
			}
		}
	}

	public List<DLNAResource> getList() {
		return list;
	}

	public void update() {
		try {
			getChildren().clear();
			setDiscovered(false);
			UMSUtils.writeResourcesToFile(lastFile(), list);
			getChildren().clear();
			setDiscovered(false);
		} catch (IOException e) {
		}
	}

	private void parseLastFile() {
		File f = lastFile();
		try {
			UMSUtils.readResourcesFromFile(f, list);
			UMSUtils.writeResourcesToFile(f, list);
		} catch (Exception e) {
		}
	}
}
