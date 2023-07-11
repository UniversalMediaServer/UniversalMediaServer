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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UMSUtilsTest {
	/**
	 * Set up testing conditions before running the tests.
	 * @throws ConfigurationException
	 */
	@SuppressWarnings("static-method")
	@BeforeEach
	public final void setUp() throws ConfigurationException, InterruptedException {
		// Silence all log messages from the UMS code that are being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.OFF);
		PMS.get();
		PMS.setConfiguration(new UmsConfiguration(false));
	}

	@Test
	public void testPlayedDurationStr() throws Exception {
		assertEquals(UMSUtils.playedDurationStr("01:23:45:67.89", "01:23:45:67"), "23:45:67 / 23:45:67");
		assertEquals(UMSUtils.playedDurationStr("01:23", "01:23:45"), "1:23 / 1:23:45");
		assertEquals(UMSUtils.playedDurationStr(":12", "59"), "0:12 / 0:59");
	}
}
