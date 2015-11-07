package net.pms.dlna;

import java.io.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
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
	private Set<String> watchedEntries;
	private PmsConfiguration config;

	public MediaMonitor(File[] dirs) {
		super(Messages.getString("VirtualFolder.2"), "images/thumbnail-folder-256.png");
		this.dirs = dirs;
		watchedEntries = new HashSet<>();
		config = PMS.getConfiguration();
		parseMonitorFile();
	}

	private File monitorFile() {
		return new File(config.getDataFile("UMS.mon"));
	}

	private void parseMonitorFile() {
		File f = monitorFile();
		if (!f.exists()) {
			return;
		}
		try {
			try (BufferedReader in = new BufferedReader(new FileReader(f))) {
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
						watchedEntries.add(entry.trim());
					}
				}
			}
			dumpFile();
		} catch (IOException e) {
		}
	}

	public void scanDir(File[] files, final DLNAResource res) {
		final DLNAResource mm = this;
		res.addChild(new VirtualVideoAction(Messages.getString("PMS.150"), true) {
			@Override
			public boolean enable() {
				for (DLNAResource r : res.getChildren()) {
					if (!(r instanceof RealFile)) {
						continue;
					}
					RealFile rf = (RealFile) r;
					watchedEntries.add(rf.getFile().getAbsolutePath());
				}
				mm.setDiscovered(false);
				mm.getChildren().clear();
				try {
					dumpFile();
				} catch (IOException e) {
				}
				return true;
			}
		});

		for (File f : files) {
			if (f.isFile()) {
				if (isWatched(f.getAbsolutePath())) {
					continue;
				}
				res.addChild(new RealFile(f));
			}
			if (f.isDirectory()) {
				boolean add = true;
				if (config.isHideEmptyFolders()) {
					add = FileUtil.isFolderRelevant(f, config, watchedEntries);
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

	private boolean isMonitorClass(DLNAResource res) {
		return (res instanceof MonitorEntry) || (res instanceof MediaMonitor);
	}

	public void stopped(DLNAResource res) {
		if (!(res instanceof RealFile)) {
			return;
		}

		RealFile rf = (RealFile) res;
		DLNAResource tmp = res.getParent();
		while (tmp != null) {
			if (isMonitorClass(tmp)) {
				if (isWatched(rf.getFile().getAbsolutePath())) { // no duplicates!
					return;
				}
				watchedEntries.add(rf.getFile().getAbsolutePath());
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

	private boolean isWatched(String str) {
		return watchedEntries.contains(str);
	}

	/**
	 * Populates UMS.mon with a list of completely watched videos.
	 *
	 * @throws IOException 
	 */
	private void dumpFile() throws IOException {
		File f = monitorFile();
		Date now = new Date();
		try (FileWriter out = new FileWriter(f)) {
			StringBuilder sb = new StringBuilder();
			sb.append("######\n");
			sb.append("## NOTE!!!!!\n");
			sb.append("## This file is auto generated\n");
			sb.append("## Edit with EXTREME care\n");
			sb.append("## Generated: ");
			sb.append(now.toString());
			sb.append("\n");
			for (String str : watchedEntries) {
				if (sb.indexOf(str) == -1) {
					sb.append("entry=");
					sb.append(str);
					sb.append("\n");
				}
			}
			out.write(sb.toString());
			out.flush();
		}
	}
}
