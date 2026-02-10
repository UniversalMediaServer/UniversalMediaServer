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

import java.io.File;
import java.net.MalformedURLException;
import org.apache.commons.io.FileUtils;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class ResourceIdentifierTest {
	private static final Class<?> CLASS = ResourceIdentifierTest.class;

	public void testResourceIdentifier(String test, String uri, String expected) {
		String actual = ResourceIdentifier.getResourceIdentifier(uri);
		assertEquals(expected, actual, test);
	}

	public static File getTestFile(String testFile) {
		return FileUtils.toFile(CLASS.getResource(testFile));
	}

	@Test
	public void testResourceIdentifiers() {
		//test with file
		File file = getTestFile("/net/pms/parsers/video-h264-aac.mp4");
		String filePath = file.getAbsolutePath();
		testResourceIdentifier("file: " + filePath, file.getAbsolutePath(), "5a508e91e1f042a9");
		//test with file url
		try {
			String fileUrl = file.toURI().toURL().toString();
			testResourceIdentifier("file url: " + fileUrl, fileUrl, "5a508e91e1f042a9");
		} catch (MalformedURLException ex) {
			// Can't happen
		}
		//test with url
		testResourceIdentifier("url", "http://test.me", "60cb8b08c493ec5b");
		//test with text
		testResourceIdentifier("text", "something", "97a313603bc96153");
		//test with null
		testResourceIdentifier("null", null, null);
	}
}
