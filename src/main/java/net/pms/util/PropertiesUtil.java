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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesUtil {
	/**
	 * Logs messages to all different channels.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesUtil.class);

	/**
	 * General properties for the UMS project.
	 */
	private static final PropertiesWrapper PROJECT_PROPERTIES = new PropertiesWrapper();

	/**
	 * This class is not meant to be instantiated.
	 */
	private PropertiesUtil() {
	}

	static {
		try {
			// Read project properties resource file.
			PROJECT_PROPERTIES.loadFromResourceFile("/resources/project.properties");
		} catch (IOException e) {
			LOGGER.error("Could not load project.properties");
		}
	}

	/**
	 * Returns the project properties object that is constructed from the
	 * "project.properties" file.
	 * <p>
	 * Note that in the Maven "test" phase (e.g. when running UMS from Eclipse)
	 * the file "src/test/resources/project.properties" is used, whereas in
	 * other phases, the file "src/main/resources/project.properties" (e.g. when
	 * packaging the final build) will be used.
	 *
	 * @return The properties object.
	 */
	public static PropertiesWrapper getProjectProperties() {
		return PROJECT_PROPERTIES;
	}
}
