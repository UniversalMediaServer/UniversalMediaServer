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
package net.pms.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemErrWrapper extends OutputStream {

	private static final Logger LOGGER = LoggerFactory.getLogger(SystemErrWrapper.class);
	private int pos = 0;
	private byte[] line = new byte[5000];

	@Override
	public void write(int b) throws IOException {
		if (b == 10) {
			byte[] text = new byte[pos];
			System.arraycopy(line, 0, text, 0, pos);
			LOGGER.info(new String(text, StandardCharsets.UTF_8));
			pos = 0;
			line = new byte[5000];
		} else if (b != 13) {
			line[pos] = (byte) b;
			pos++;
		}
	}
}
