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
package net.pms.newgui.components;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import net.pms.configuration.UmsConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class TextAreaFIFOTest {
	@BeforeEach
	public void setUp() throws ConfigurationException, InterruptedException {
		// Silence all log messages from the UMS code that are being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.OFF);
	}

	@Test
	public void testTextAreaFIFO() {
		TextAreaFIFO textArea = new TextAreaFIFO(950, 100);

		assertEquals(textArea.getMaxLines(), 950, "InitialLines");
		textArea.setMaxLines(0);
		assertEquals(textArea.getMaxLines(), UmsConfiguration.getLoggingLogsTabLinebufferMin(), "MinLines");
		textArea.setMaxLines(1000000);
		assertEquals(textArea.getMaxLines(), UmsConfiguration.getLoggingLogsTabLinebufferMax(), "MaxLines");
	}
}
