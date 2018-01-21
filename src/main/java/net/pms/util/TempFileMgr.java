/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package net.pms.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import net.pms.PMS;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TempFileMgr {
	private static final Logger LOGGER = LoggerFactory.getLogger(TempFileMgr.class);
	private static final int DEFAULT_CLEAN_TIME = 14 * 24 * 3600 * 1000;
	private static final int INTERVAL = 24 * 3600 * 1000;
	private HashMap<File, Integer> files;

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
			LOGGER.error(
				"An error occurred while trying to add \"{}\" to temporary file management: {}",
				f.getAbsolutePath(),
				e.getMessage()
			);
			LOGGER.trace("", e);
		}
	}

	private void scan() {
		long now = System.currentTimeMillis();
		for (Iterator<Entry<File, Integer>> it = files.entrySet().iterator(); it.hasNext();) {
			Entry<File, Integer> entry = it.next();
			if (!entry.getKey().exists()) {
				it.remove();
				continue;
			}
			if ((now - entry.getKey().lastModified()) > entry.getValue()) {
				it.remove();
				if (!entry.getKey().delete()) {
					LOGGER.warn("Failed to delete temporary file \"{}\"", entry.getKey().getAbsolutePath());
				}
			}
		}
		try {
			dumpFile();
		} catch (IOException e) {
			LOGGER.error(
				"An error occurred while trying to write the temporary file management file: {}",
				e.getMessage()
			);
			LOGGER.trace("", e);
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

	private static File cleanFile() {
		return new File(PMS.getConfiguration().getDataFile("UMS.tmpmgr"));
	}

	private void parseCleanFile() {
		File f = cleanFile();
		if (!f.exists()) {
			return;
		}
		try {
			try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
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
			LOGGER.error(
				"An error occurred while trying to read the temporary file management file \"{}\": {}",
				f.getAbsolutePath(),
				e.getMessage()
			);
			LOGGER.trace("", e);
		}
	}

	private void dumpFile() throws IOException {
		try (FileOutputStream out = new FileOutputStream(cleanFile())) {
			Date now = new Date();
			String n = "## " + now.toString() + "\n";
			out.write("#########\n".getBytes(StandardCharsets.UTF_8));
			out.write(n.getBytes(StandardCharsets.UTF_8));
			for (Entry<File, Integer> entry : files.entrySet()) {
				String str = entry.getKey().getAbsolutePath() + "," + entry.getValue() + "\n";
				out.write(str.getBytes(StandardCharsets.UTF_8));
			}
			out.flush();
		}
	}
}
