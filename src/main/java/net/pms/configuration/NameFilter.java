package net.pms.configuration;

import java.io.File;
import java.io.FileNotFoundException;
import net.pms.PMS;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.RealFile;
import net.pms.util.FileUtil;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NameFilter {
	private final PropertiesConfiguration configuration;
	private final ConfigurationReader configurationReader;
	private static final Logger LOGGER = LoggerFactory.getLogger(NameFilter.class);


	public NameFilter() throws ConfigurationException {
		configuration = new PropertiesConfiguration();
		configurationReader = new ConfigurationReader(configuration, true); // true: log
		configuration.setListDelimiter((char) 0);

		File denyFile = new File(PMS.getConfiguration().getProfileDirectory(), "UMS.deny");

		try {
			if (FileUtil.getFilePermissions(denyFile).isReadable()) {
				configuration.load(denyFile);
				configuration.setPath(denyFile.getAbsolutePath());
			}
		} catch (FileNotFoundException e) {
			LOGGER.trace("Could not read name filter: {}", e.getMessage());
			throw new ConfigurationException("Could not read NameFilter " + e.getMessage(), e);
		}
	}

	private boolean doFilter(String tag, String name, String what) {
		String str = configurationReader.getNonBlankConfigurationString(tag + what, null);
		if (StringUtils.isBlank(str)) {
			return false;
		}
		// if regexp matches we'll filter this out
		return name.matches(str);
	}

	/*
	 * This function is here to make it possible
	 * filter more types of files
	 */
	private boolean doFile(String tag, DLNAResource res) {
		if (res instanceof RealFile) {
			RealFile rf = (RealFile) res;
			return doFilter(tag, rf.getFile().getAbsolutePath(), ".file");
		} else {
			return false;
		}
	}

	public boolean filter(String tag, DLNAResource res) {
		return doFilter(tag, res.getName(), ".name") ||
			doFile(tag, res) ||
			doFilter(tag, res.getSystemName(), ".sys") ||
			// name is done twice here without an explicit what
			doFilter(tag, res.getName(), "");
	}
}
