package net.pms.dlna;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import net.pms.Messages;
import net.pms.dlna.virtual.VirtualFolder;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaMonitor extends VirtualFolder {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaMonitor.class);
	private File[] dirs;
	private ArrayList<String> oldEntries;

	public MediaMonitor(File[] dirs) {
		super(Messages.getString("VirtualFolder.2"), null);
		this.dirs = dirs;
		oldEntries = new ArrayList<>();
		parseMonitorFile();
	}

	private File monitorFile() {
		return new File("UMS.mon");
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
						if (!oldEntries.contains(entry.trim())) {
							oldEntries.add(entry.trim());
						}
					}
				}
			}
			dumpFile();
		} catch (Exception e) {
		}
	}

	public void scanDir(File[] files, DLNAResource res) {
		for (int i = 0; i < files.length; i++) {
			File f = files[i];
			if (f.isFile()) {
				// regular file
				LOGGER.debug("file " + f + " is old? " + old(f.getAbsolutePath()));
				if (old(f.getAbsolutePath())) {
					continue;
				}
				res.addChild(new RealFile(f));
			}
			if (f.isDirectory()) {
				res.addChild(new MonitorEntry(f, this));
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
		try (FileWriter out = new FileWriter(f)) {
			StringBuilder sb = new StringBuilder();
			sb.append("######\n");
			sb.append("## NOTE!!!!!\n");
			sb.append("## This file is auto generated\n");
			sb.append("## Edit with EXTREME care\n");
			for (String str : oldEntries) {
				sb.append("entry=");
				sb.append(str);
				sb.append("\n");
			}
			out.write(sb.toString());
			out.flush();
		}
	}
}
