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
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A version of OutputTextConsumer that a) logs all output to the logfile and b) doesn't store the output
 */
public class OutputTextLogger extends OutputConsumer {
	private static final Logger LOGGER = LoggerFactory.getLogger(OutputTextLogger.class);

	public OutputTextLogger(InputStream inputStream) {
		super(inputStream);
	}

	@Override
	public void run() {
		try (LineIterator it = IOUtils.lineIterator(inputStream, StandardCharsets.UTF_8)) {
			while (it.hasNext()) {
				String line = it.next();
				LOGGER.debug(line);
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

	@Override
	public BufferedOutputFile getBuffer() {
		return null;
	}

	@Override
	public List<String> getResults() {
		return null;
	}
}
