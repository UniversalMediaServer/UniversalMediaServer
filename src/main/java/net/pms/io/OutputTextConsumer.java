/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
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
package net.pms.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutputTextConsumer extends OutputConsumer {
	private static final Logger LOGGER = LoggerFactory.getLogger(OutputTextConsumer.class);
	private List<String> lines = new ArrayList<String>();
	private Object linesLock = new Object();
	private boolean log;

	public OutputTextConsumer(InputStream inputStream, boolean log) {
		super(inputStream);
		linesLock = new Object();
		this.log = log;
	}

	public void run() {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
			String line = null;
			int authorized = 10;
			while ((line = br.readLine()) != null) {
				if (line.length() > 0 && line.startsWith("[") && authorized > 0) {
					addLine(line);
					if (log) {
						LOGGER.trace(line);
					}
					authorized--;
				} else if (line.length() > 0 && !line.startsWith("[") && !line.startsWith("100") && !line.startsWith("size") && !line.startsWith("frame") && !line.startsWith("Pos") && !line.startsWith("ERROR:") && !line.startsWith("BUFFER") && !line.startsWith("INITV")) {
					addLine(line);
					if (log) {
						LOGGER.trace(line);
					}
				}
			}
		} catch (IOException ioe) {
			LOGGER.debug("Error consuming stream of spawned process: " + ioe.getMessage());
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					LOGGER.debug("Caught exception", e);
				}
			}
		}
	}

	private void addLine(String line) {
		synchronized (linesLock) {
			lines.add(line);
		}
	}

	public BufferedOutputFile getBuffer() {
		return null;
	}

	public List<String> getResults() {
		List<String> clonedResults = new ArrayList<String>();
		synchronized (linesLock) {
			clonedResults.addAll(lines);
		}
		return clonedResults;
	}
}
