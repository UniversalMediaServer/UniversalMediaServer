package net.pms.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.pms.PMS;
import net.pms.configuration.DeviceConfiguration;
import net.pms.configuration.UmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.logging.LoggingConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbgPacker {
	protected static final Logger LOGGER = LoggerFactory.getLogger(DbgPacker.class);

	protected final LinkedHashMap<File, Object> items;
	protected String defaultLogFile;

	public DbgPacker() {
		items = new LinkedHashMap<>();

		HashMap<String, String> logFilePaths = LoggingConfig.getLogFilePaths();
		if (!logFilePaths.isEmpty()) {
			defaultLogFile = LoggingConfig.getLogFilePaths().get("default.log");
			if (defaultLogFile == null) {
				// Just get the path of one of the files as we can't find the default
				Map.Entry<String, String> entry = logFilePaths.entrySet().iterator().next();
				defaultLogFile = entry.getValue();
			}
		}
	}

	protected void poll() {
		// call the client callbacks
		UmsConfiguration configuration = PMS.getConfiguration();

		// check dbgpack property in UMS.conf
		LOGGER.debug("Checking dbgpack property in UMS.conf");
		String f = (String) configuration.getCustomProperty("dbgpack");
		if (f != null) {
			add(f.split(","));
		}

		// add confs of connected renderers
		for (RendererConfiguration r : RendererConfiguration.getConnectedRenderersConfigurations()) {
			add(r.getFile());
			if (((DeviceConfiguration) r).isCustomized()) {
				add(((DeviceConfiguration) r).getParentFile());
			}
		}

		// add core items with the default logfile last (LinkedHashMap preserves
		// insertion order)
		String profileDirectory = configuration.getProfileDirectory();

		// add virtual folders file if it exists
		String vfolders = configuration.getVirtualFoldersFile();
		if (StringUtils.isNotEmpty(vfolders)) {
			add(new File(profileDirectory, vfolders));
		}

		add(new File(profileDirectory, "WEB.conf"));
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

	public Set<File> getItems() {
		poll();
		return items.keySet();
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
