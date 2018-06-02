package net.pms.formats;

import static org.junit.Assert.fail;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.Test;


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
