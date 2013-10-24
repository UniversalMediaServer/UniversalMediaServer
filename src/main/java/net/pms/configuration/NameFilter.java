package net.pms.configuration;

import net.pms.PMS;
import net.pms.util.FileUtil;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

public class NameFilter {

    private final PropertiesConfiguration configuration;
    private final ConfigurationReader configurationReader;

    public NameFilter() throws ConfigurationException {
        configuration = new PropertiesConfiguration();
        configurationReader = new ConfigurationReader(configuration, true); // true: log
        configuration.setListDelimiter((char) 0);

        String denyFile = PMS.getConfiguration().getProfileDirectory() + File.separator + "UMS.deny";
        File f = new File(denyFile);

        if(f.exists() && FileUtil.isFileReadable(f)) {
            configuration.load(denyFile);
            configuration.setPath(denyFile);
        }
    }

    public boolean filter(String tag, String name) {
        String str = configurationReader.getNonBlankConfigurationString(tag, null);
        if(StringUtils.isBlank(str)) {
            return false;
        }
        // if regexp matches we'll filter this out
        return name.matches(str);
    }
}
