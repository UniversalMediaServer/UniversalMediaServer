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

import com.sun.jna.Platform;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An input stream consumer that stores the consumed lines in a list and optionally logs each line.
 */
public class OutputTextConsumer extends OutputConsumer {
	private static final Logger LOGGER = LoggerFactory.getLogger(OutputTextConsumer.class);
	private static Charset charset = null;
	private final List<String> lines = new ArrayList<>();
	private final Object linesLock = new Object();
	private final boolean log;

	public OutputTextConsumer(InputStream inputStream, boolean log) {
		super(inputStream);
		this.log = log;
		if (charset == null) {
			if (Platform.isWindows() && WinUtils.getOEMCharset() != null) {
				charset = WinUtils.getOEMCharset();
			} else {
				charset = StandardCharsets.UTF_16;
			}
		}
	}

	@Override
	public void run() {
		try (LineIterator it = IOUtils.lineIterator(new InputStreamReader(inputStream, charset))) {
			while (it.hasNext()) {
				String line = it.nextLine();

				if (line.length() > 0) {
					addLine(line);
				}

				if (log) {
					LOGGER.debug(line);
				}
				if (filtered) {
					filtered = filter(line);
				}
			}
		} catch (IOException ioe) {
			LOGGER.debug("Error consuming input stream: {}", ioe.getMessage());
		} catch (IllegalStateException ise) {
			LOGGER.debug("Error reading from closed input stream: {}", ise.getMessage());
		}
	}

	private void addLine(String line) {
		synchronized (linesLock) {
			lines.add(line);
		}
	}

	@Override
	public BufferedOutputFile getBuffer() {
		return null;
	}

	@Override
	public List<String> getResults() {
		List<String> clonedResults = new ArrayList<>();

		synchronized (linesLock) {
			clonedResults.addAll(lines);
		}

		return clonedResults;
	}
}
