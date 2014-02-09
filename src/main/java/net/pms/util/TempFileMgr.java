package net.pms.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Iterator;
import net.pms.PMS;
import org.codehaus.plexus.util.StringUtils;

public class TempFileMgr {
	private static final int DEFAULT_CLEAN_TIME = 14 * 24 * 3600 * 1000;
	private static final int INTERVAL = 24 * 3600 * 1000;
	HashMap<File, Integer> files;

	public TempFileMgr() {
		files = new HashMap<>();
		parseCleanFile();
	}

	public void add(File f) {
		add(f, DEFAULT_CLEAN_TIME);
	}

	public void add(File f, String str) {
		try {
			add(f, Integer.parseInt(str));
		} catch (Exception e) {
			add(f);
		}
	}

	public void add(File f, int cleanTime) {
		files.put(f, cleanTime);
		try {
			dumpFile();
		} catch (IOException e) {
		}
	}

	private void scan() {
		long now = System.currentTimeMillis();
		for (Iterator<File> it = files.keySet().iterator(); it.hasNext();) {
			File f = it.next();
			if (!f.exists()) {
				it.remove();
				continue;
			}
			if ((now - f.lastModified()) > files.get(f)) {
				it.remove();
				f.delete();
			}
		}
		try {
			dumpFile();
		} catch (IOException e) {
		}
	}

	public void schedule() {
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				scan();
			}
		};
		Timer t = new Timer();
		t.scheduleAtFixedRate(task, 0, INTERVAL);
	}

	private File cleanFile() {
		return new File(PMS.getConfiguration().getDataFile("UMS.tmpmgr"));
	}

	private void parseCleanFile() {
		File f = cleanFile();
		if (!f.exists()) {
			return;
		}
		try {
			try (BufferedReader in = new BufferedReader(new FileReader(f))) {
				String str;

				while ((str = in.readLine()) != null) {
					if (StringUtils.isEmpty(str) || str.startsWith("#")) {
						continue;
					}
					String[] tmp = str.split(",");
					if (tmp.length > 1) {
						add(new File(tmp[0]), tmp[1]);
					} else {
						add(new File(tmp[0]));
					}
				}
			}
		} catch (IOException e) {
		}
	}

	private void dumpFile() throws IOException {
		try (FileOutputStream out = new FileOutputStream(cleanFile())) {
			Date now = new Date();
			String n = "## " + now.toString() + "\n";
			out.write("#########\n".getBytes());
			out.write(n.getBytes());
			for (File f : files.keySet()) {
				String str = f.getAbsolutePath() + "," + files.get(f) + "\n";
				out.write(str.getBytes());
			}
			out.flush();
		}
	}
}
