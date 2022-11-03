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
package net.pms.formats;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class FormatsExtensionTest {

	@Test
	public void testDuplicateExtensions() throws Exception {
		Map<Class<? extends Object>, String[]> extensions = new HashMap<>();

		for (Format format : FormatFactory.FORMATS) {
			if (format.getSupportedExtensions() != null) {
				extensions.put(format.getClass(), format.getSupportedExtensions());
			}
		}

		for (Entry<Class<? extends Object>, String[]> entry : extensions.entrySet()) {
			for (String extension : entry.getValue()) {
				for (Entry<Class<? extends Object>, String[]> otherEntry : extensions.entrySet()) {
					if (!entry.getKey().equals(otherEntry.getKey())) {
						for (String otherExtension : otherEntry.getValue()) {
							if (extension.equals(otherExtension)) {
								fail(String.format(
									"Extension \"%s\" exists for both format \"%s\" and format \"%s\"",
									extension,
									entry.getKey().getName(),
									otherEntry.getKey().getName()
								));
							}
						}
					}
				}
			}
		}
	}
}
