package net.pms.dlna;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.dlna.virtual.VirtualVideoAction;
import net.pms.util.FileUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaMonitor extends VirtualFolder {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaMonitor.class);
	private File[] dirs;
	private ArrayList<String> oldEntries;
	private PmsConfiguration config;

	public MediaMonitor(File[] dirs) {
		super(Messages.getString("VirtualFolder.2"), "images/thumbnail-video-256.png");
		this.dirs = dirs;
		oldEntries = new ArrayList<String>();
		config = PMS.getConfiguration();
		config = PMS.getConfiguration();
		parseMonitorFile();
	}

	private File monitorFile() {
		return new File(PMS.getConfiguration().getDataFile("UMS.mon"));
	}

	private void parseMonitorFile() {
		File f = monitorFile();
		if (!f.exists()) {
			return;
		}
		try {
			BufferedReader in = new BufferedReader(new FileReader(f));
			String str;

			while ((str = in.readLine()) != null) {
				if (StringUtils.isEmpty(str)) {
					continue;
				}
				str = str.trim();
				if (str.startsWith("#")) {
					continue;
				}
				if (str.startsWith("entry=")) {
					String entry = str.substring(6);
					if (!new File(entry.trim()).exists()) {
						continue;
					}
					if (!oldEntries.contains(entry.trim())) {
						oldEntries.add(entry.trim());
					}
				}
			}
			in.close();
			dumpFile();
		} catch (Exception e) {
		}
	}

	public void scanDir(File[] files, DLNAResource res) {
		final DLNAResource start = res;
		res.addChild(new VirtualVideoAction(Messages.getString("PMS.139"), true) {
			@Override
			public boolean enable() {
				for (DLNAResource r : start.getChildren()) {
					if (!(r instanceof RealFile)) {
						continue;
					}
					RealFile rf = (RealFile) r;
					if (old(rf.getFile().getAbsolutePath())) { // no duplicates!
						continue;
					}
					oldEntries.add(rf.getFile().getAbsolutePath());
				}
				start.setDiscovered(false);
				start.getChildren().clear();
				try {
					dumpFile();
				} catch (IOException e) {
				}
				return true;
			}
		});

		for (File f : files) {
			if (f.isFile()) {
				// regular file
				LOGGER.debug("file " + f + " is old? " + old(f.getAbsolutePath()));
				if (old(f.getAbsolutePath())) {
					continue;
				}
				res.addChild(new RealFile(f));
			}
			if (f.isDirectory()) {
				boolean add = true;
				if (config.isHideEmptyFolders()) {
					add = FileUtil.isFolderRelevant(f, PMS.getConfiguration());
				}
				if (add) {
					res.addChild(new MonitorEntry(f, this));
				}
			}
		}
	}

	@Override
	public void discoverChildren() {
		for (File f : dirs) {
			scanDir(f.listFiles(), this);
		}
	}

	@Override
	public boolean isRefreshNeeded() {
		return true;
	}

	private boolean monitorClass(DLNAResource res) {
		return (res instanceof MonitorEntry) || (res instanceof MediaMonitor);
	}

	public void stopped(DLNAResource res) {
		if (!(res instanceof RealFile)) {
			return;
		}
		RealFile rf = (RealFile) res;
		DLNAResource tmp = res.getParent();
		while (tmp != null) {
			if (monitorClass(tmp)) {
				if (old(rf.getFile().getAbsolutePath())) { // no duplicates!
					return;
				}
				oldEntries.add(rf.getFile().getAbsolutePath());
				setDiscovered(false);
				getChildren().clear();
				try {
					dumpFile();
				} catch (IOException e) {
				}
				return;
			}
			tmp = tmp.getParent();
		}
	}

	private boolean old(String str) {
		return oldEntries.contains(str);
	}

	private void dumpFile() throws IOException {
		File f = monitorFile();
		Date now = new Date();
		FileWriter out = new FileWriter(f);
		StringBuilder sb = new StringBuilder();
		sb.append("######\n");
		sb.append("## NOTE!!!!!\n");
		sb.append("## This file is auto generated\n");
		sb.append("## Edit with EXTREME care\n");
		sb.append("## Generated: ");
		sb.append(now.toString());
		sb.append("\n");
		for (String str : oldEntries) {
			sb.append("entry=");
			sb.append(str);
			sb.append("\n");
		}
		out.write(sb.toString());
		out.flush();
		out.close();
	}
}
