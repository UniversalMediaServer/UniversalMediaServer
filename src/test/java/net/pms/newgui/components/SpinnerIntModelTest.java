/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
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
package net.pms.newgui.components;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.apache.commons.configuration.ConfigurationException;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class SpinnerIntModelTest {


	@Before
	public void setUp() throws ConfigurationException {
		// Silence all log messages from the UMS code that is being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.OFF);
	}

	@Test
	public void testSpinnerIntModel() {
		SpinnerIntModel intModel = new SpinnerIntModel(65,50,250,100);

		assertEquals("InitValue", intModel.getIntValue(), 65);
		assertEquals("LowerLimit", intModel.getMinimum(), 50);
		assertEquals("UpperLimit", intModel.getMaximum(), 250);
		assertEquals("StepSize", intModel.getStepSize(), 100);
		assertEquals("NextValue", intModel.getNextValue(), 165);
		assertEquals("PrevValue", intModel.getPreviousValue(), 50);
		intModel.setIntValue(50);
		assertEquals("NextValue", intModel.getNextValue(), 100);
		assertEquals("PrevValue", intModel.getPreviousValue(), 50);
		assertEquals("CurrValue", intModel.getValue(), 50);
		intModel.setValue(intModel.getNextValue());
		intModel.setValue(intModel.getNextValue());
		intModel.setValue(intModel.getNextValue());
		assertEquals("NextValue", intModel.getNextValue(), 250);
		assertEquals("PrevValue", intModel.getPreviousValue(), 200);
		assertEquals("CurrValue", intModel.getValue(), 250);
	}
}
