/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
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
	private final List<String> lines = new ArrayList<>();
	private final Object linesLock = new Object();
	private final boolean log;

	public OutputTextConsumer(InputStream inputStream, boolean log) {
		super(inputStream);
		this.log = log;
	}

	@Override
	public void run() {
		try (LineIterator it = IOUtils.lineIterator(inputStream,  Charset.defaultCharset())) {
			while (it.hasNext()) {
				String line = it.next();

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
