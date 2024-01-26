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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.logging.LoggingConfig;
import net.pms.renderers.ConnectedRenderers;
import net.pms.renderers.Renderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbgPacker {

	private static final Logger LOGGER = LoggerFactory.getLogger(DbgPacker.class);

	private final LinkedHashMap<File, Object> items;
	private String defaultLogFile;

	public DbgPacker() {
		items = new LinkedHashMap<>();

		Map<String, String> logFilePaths = LoggingConfig.getLogFilePaths();
		if (!logFilePaths.isEmpty()) {
			defaultLogFile = LoggingConfig.getLogFilePaths().get("default.log");
			if (defaultLogFile == null) {
				// Just get the path of one of the files as we can't find the default
				Map.Entry<String, String> entry = logFilePaths.entrySet().iterator().next();
				defaultLogFile = entry.getValue();
			}
		}
	}

	public void poll() {
		// call the client callbacks
		UmsConfiguration configuration = PMS.getConfiguration();

		// check dbgpack property in UMS.conf
		LOGGER.debug("Checking dbgpack property in UMS.conf");
		String f = (String) configuration.getCustomProperty("dbgpack");
		if (f != null) {
			add(f.split(","));
		}

		// add confs of connected renderers
		for (Renderer renderer : ConnectedRenderers.getConnectedRenderers()) {
			add(renderer.getFile());
			if (renderer.isCustomized()) {
				add(renderer.getParentFile());
			}
		}

		// add core items with the default logfile last (LinkedHashMap preserves
		// insertion order)
		String profileDirectory = configuration.getProfileDirectory();

		add(new File(profileDirectory, "SHARED.conf"));
		add(new File(configuration.getProfilePath()));
		if (defaultLogFile != null && !defaultLogFile.isEmpty()) {
			add(new File(defaultLogFile + ".prev.zip"));
			add(new File(defaultLogFile + ".zip"));
			add(new File(defaultLogFile));
		}
	}

	private void add(String[] files) {
		for (String file : files) {
			add(new File(file));
		}
	}

	private void add(File file) {
		if (file != null) {
			LOGGER.debug("adding {}", file.getAbsolutePath());
			try {
				items.put(file.getCanonicalFile(), null);
			} catch (IOException e) {
			}
		}
	}

	public Set<File> getFiles() {
		poll();
		return items.keySet();
	}

	public Map<File, Object> getItems() {
		return items;
	}

	public static void writeToZip(ZipOutputStream out, File f) throws Exception {
		byte[] buf = new byte[1024];
		int len;
		if (!f.exists()) {
			LOGGER.debug("DbgPack file {} does not exist - ignoring", f.getAbsolutePath());
			return;
		}
		try (FileInputStream in = new FileInputStream(f)) {
			out.putNextEntry(new ZipEntry(f.getName()));
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			out.closeEntry();
		}
	}

}
