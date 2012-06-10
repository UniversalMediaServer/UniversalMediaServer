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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class OutputConsumer extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(OutputConsumer.class);
	protected InputStream inputStream;

	public OutputConsumer(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	@Deprecated
	public void destroy() {
		try {
			inputStream.close();
		} catch (IOException e) {
			LOGGER.debug("Failed to close stream", e);
		}
	}

	public abstract BufferedOutputFile getBuffer();

	public abstract List<String> getResults();
}
