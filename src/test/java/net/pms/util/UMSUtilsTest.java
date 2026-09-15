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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.pms.PMS;
import net.pms.TestHelper;
import net.pms.configuration.UmsConfiguration;
import net.pms.media.MediaInfo;
import net.pms.store.StoreResource;
import org.apache.commons.configuration2.ex.ConfigurationException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UMSUtilsTest {
	/**
	 * Set up testing conditions before running the tests.
	 * @throws ConfigurationException
	 */
	@SuppressWarnings("static-method")
	@BeforeEach
	public final void setUp() throws ConfigurationException, InterruptedException {
		TestHelper.setLoggingOff();
		PMS.get();
		PMS.setConfiguration(new UmsConfiguration(false));
	}

	@Test
	public void testPlayedDurationStr() throws Exception {
		assertEquals(UMSUtils.playedDurationStr("01:23:45:67.89", "01:23:45:67"), "23:45:67 / 23:45:67");
		assertEquals(UMSUtils.playedDurationStr("01:23", "01:23:45"), "1:23 / 1:23:45");
		assertEquals(UMSUtils.playedDurationStr(":12", "59"), "0:12 / 0:59");
	}

	@Test
	public void testIsYouTubeURL() throws Exception {
		assertTrue(UMSUtils.isYouTubeURL("https://www.youtube.com/watch?v=8t_dRMvMYvY"));
		assertTrue(UMSUtils.isYouTubeURL("https://youtu.be/watch?v=9egtCHGlgoM"));
		assertTrue(UMSUtils.isYouTubeURL("https://www.youtube.com/shorts/jsBOx45iBX0"));
		assertFalse(UMSUtils.isYouTubeURL("https://www.somewebsite.com/shorts/jsBOx45iBX0"));
	}

	@Test
	public void testFilterResourcesByNameDoesNotLoadMediaForNameMatch() {
		TestResource matchingResource = new TestResource("Matching resource");
		TestResource otherResource = new TestResource("Other resource");
		List<StoreResource> resources = new ArrayList<>(List.of(matchingResource, otherResource));

		UMSUtils.filterResourcesByName(resources, "matching", false, false);

		assertEquals(List.of(matchingResource), resources);
		assertEquals(0, matchingResource.getMediaInfoCalls);
		assertEquals(1, otherResource.getMediaInfoCalls);
	}

	@Test
	public void testFilterResourcesByNameUsesLocaleIndependentCaseConversion() {
		Locale previousLocale = Locale.getDefault();
		try {
			Locale.setDefault(Locale.forLanguageTag("tr-TR"));
			TestResource resource = new TestResource("TITLE");
			List<StoreResource> resources = new ArrayList<>(List.of(resource));

			UMSUtils.filterResourcesByName(resources, "title", false, true);

			assertEquals(List.of(resource), resources);
		} finally {
			Locale.setDefault(previousLocale);
		}
	}

	@Test
	public void testFilterResourcesByNameExpectOneResultKeepsLastMatch() {
		TestResource first = new TestResource("match first");
		TestResource last = new TestResource("match last");
		List<StoreResource> resources = new ArrayList<>(List.of(first, last));

		UMSUtils.filterResourcesByName(resources, "match", true, false);

		assertEquals(List.of(last), resources);
		assertEquals(0, last.getMediaInfoCalls);
	}

	private static final class TestResource extends StoreResource {
		private final String name;
		private int getMediaInfoCalls;

		private TestResource(String name) {
			super(null);
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getSystemName() {
			return name;
		}

		@Override
		public long length() {
			return 0;
		}

		@Override
		public boolean isFolder() {
			return false;
		}

		@Override
		public boolean isValid() {
			return true;
		}

		@Override
		public MediaInfo getMediaInfo() {
			getMediaInfoCalls++;
			return null;
		}
	}
}
