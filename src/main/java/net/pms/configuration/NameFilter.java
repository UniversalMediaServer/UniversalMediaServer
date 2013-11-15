package net.pms.configuration;

import java.io.File;
import net.pms.PMS;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.RealFile;
import net.pms.util.FileUtil;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;

public class NameFilter {
	private final PropertiesConfiguration configuration;
	private final ConfigurationReader configurationReader;

	public NameFilter() throws ConfigurationException {
		configuration = new PropertiesConfiguration();
		configurationReader = new ConfigurationReader(configuration, true); // true: log
		configuration.setListDelimiter((char) 0);

		String denyFile = PMS.getConfiguration().getProfileDirectory() + File.separator + "UMS.deny";
		File f = new File(denyFile);

		if (f.exists() && FileUtil.isFileReadable(f)) {
			configuration.load(denyFile);
			configuration.setPath(denyFile);
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
