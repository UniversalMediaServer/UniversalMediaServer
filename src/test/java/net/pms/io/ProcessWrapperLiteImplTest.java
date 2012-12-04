/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
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
package net.pms.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;

import com.sun.jna.Platform;

/**
 * Tests for class ProcessWrapperLiteImpl. 
 */
public class ProcessWrapperLiteImplTest {
	@Before
	public void setUp() {
		// Silence all log messages from the PMS code that is being tested
//		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
//		context.reset(); 
    }

	/**
	 * Test the basics of the ProcessWrapperLiteImpl class. 
	 */
	@Test
	public void testBasics() {
		// Null command should return empty string
		ProcessWrapperLiteImpl pw = new ProcessWrapperLiteImpl(null);
		assertEquals("Result for empty command is empty string", "", pw.getResult());

		// Empty command should return empty string
		String[] command = {};
		pw = new ProcessWrapperLiteImpl(command);
		assertEquals("Result for empty command is empty string", "", pw.getResult());

		// Non-existing command should return empty string
		command = new String[] { "non-existing-command" };
		pw = new ProcessWrapperLiteImpl(command);
		assertEquals("Result for non existing command is empty string", "", pw.getResult());

		// "cd ." should return an empty string on all operating systems.
		command = new String[] { "cd", "." };
		pw = new ProcessWrapperLiteImpl(command);
		assertEquals("Result for \"cd .\" is empty string", "", pw.getResult());
	}

	/**
	 * Test commands that return output.
	 */
	@Test
	public void testCommandsWithOutput() {
		String[] command;

		if (Platform.isWindows()) {
			// Windows command that produces output
			command = new String[] { "dir", "/s" };
		} else {
			// Mac OSX and Linux command that produces output
			command = new String[] { "ls", "-a" };
		}

		ProcessWrapperLiteImpl pw = new ProcessWrapperLiteImpl(command);

		// It would probably be wise to insert a test for the actual output,
		// e.g. to see if it contains a particular string. For now, simply
		// test to see whether the result is not empty.
		assertFalse("Result for command with output is not empty", "".equals(pw.getResult()));
	}
}
