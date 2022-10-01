/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
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
package net.pms.platform.linux;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.pms.platform.PlatformUtils;
import net.pms.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Linux specific platform code.
 * Only to be instantiated by {@link PlatformUtils#createInstance()}.
 */
public class LinuxUtils extends PlatformUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(LinuxUtils.class);

	@Override
	public void moveToTrash(File file) throws IOException {
		try {
			FreedesktopTrash.moveToTrash(file);
		} catch (FileUtil.InvalidFileSystemException e) {
			throw new IOException(e);
		}
	}

	@Override
	public boolean isAdmin() {
		synchronized (IS_ADMIN_LOCK) {
			if (isAdmin != null) {
				return isAdmin;
			}
			try {
				final String command = "id -Gn";
				LOGGER.trace("isAdmin: Executing \"{}\"", command);
				Process p = Runtime.getRuntime().exec(command);
				InputStream is = p.getInputStream();
				InputStreamReader isr = new InputStreamReader(is, StandardCharsets.US_ASCII);
				int exitValue;
				String exitLine;
				try (BufferedReader br = new BufferedReader(isr)) {
					p.waitFor();
					exitValue = p.exitValue();
					exitLine = br.readLine();
				}

				if (exitValue != 0 || exitLine == null || exitLine.isEmpty()) {
					LOGGER.error("Could not determine root privileges, \"{}\" ended with exit code: {}", command, exitValue);
					isAdmin = false;
					return false;
				}

				LOGGER.trace("isAdmin: \"{}\" returned {}", command, exitLine);
				if (exitLine.matches(".*\\broot\\b.*")) {
					LOGGER.trace("isAdmin: UMS has root privileges");
					isAdmin = true;
					return true;
				}

				LOGGER.trace("isAdmin: UMS does not have root privileges");
				isAdmin = false;
				return false;
			} catch (IOException | InterruptedException e) {
				LOGGER.error(
					"An error prevented UMS from checking Linux permissions: {}",
					e.getMessage()
				);
			}
			isAdmin = false;
			return false;
		}
	}

	@Override
	public String getDefaultFontPath() {
		// get Linux default font
		String font = getAbsolutePath("/usr/share/fonts/truetype/msttcorefonts/", "Arial.ttf");
		if (font == null) {
			font = getAbsolutePath("/usr/share/fonts/truetype/ttf-bitstream-veras/", "Vera.ttf");
		}
		if (font == null) {
			font = getAbsolutePath("/usr/share/fonts/truetype/ttf-dejavu/", "DejaVuSans.ttf");
		}
		return font;
	}

}
