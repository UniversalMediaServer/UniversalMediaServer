/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2010  A.Brochard
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
package net.pms.logging;

import ch.qos.logback.core.PropertyDefinerBase;
import net.pms.PMS;
import net.pms.configuration.ConfigurationReader;
import net.pms.configuration.PmsConfiguration;

/**
 * Logback PropertyDefiner to set the root level, path and name for the <code>default logfile</code>.
 * @author thomas@innot.de
 */
public class DebugLogPropertyDefiner extends PropertyDefinerBase {
	private static final PmsConfiguration configuration = PMS.getConfiguration();

	String key;

	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public String getPropertyValue() {
		String result = null;
		ConfigurationReader configurationReader = configuration.getConfigurationReader();
		boolean saveLogOverride = configurationReader.getLogOverrides();
		configurationReader.setLogOverrides(false);
		switch (key) {
			case "debugLogPath":
			case "logFilePath":
				result = configuration.getDefaultLogFileFolder();
				break;
			case "rootLevel":
				result = configuration.getRootLogLevel();
				break;
			case "logFileName":
				result = configuration.getDefaultLogFileName();
				break;
		}
		configurationReader.setLogOverrides(saveLogOverride);
		return result;
	}
}
