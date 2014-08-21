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

package net.pms.configuration;

import ch.qos.logback.classic.LoggerContext;
import java.util.*;
import java.util.Map.Entry;
import static net.pms.configuration.RendererConfiguration.*;
import org.apache.commons.configuration.ConfigurationException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

/**
 * Test the RendererConfiguration class
 */
public class RendererConfigurationTest {
	class TestCase {
		String UA, UAAHH, renderer;
		TestCase(String UA, String UAAHH, String renderer) {
			this.UA = UA;
			this.UAAHH = UAAHH;
			this.renderer = renderer;
		}
	}
	private final List<TestCase> testCases = new ArrayList<>();

	@Before
	public void setUp() {
		// Silence all log messages from the PMS code that is being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.reset(); 

		// Set locale to EN to ignore translations for renderers
		Locale.setDefault(Locale.ENGLISH);

		// Cases that are too generic should not match anything
		testCases.add(new TestCase("User-Agent: UPnP/1.0 DLNADOC/1.50", null, null));
		testCases.add(new TestCase("User-Agent: Unknown Renderer", null, null));
		testCases.add(new TestCase(null, "X-Unknown-Header: Unknown Content", null));

		// AirPlayer:
		testCases.add(new TestCase("User-Agent: AirPlayer/1.0.09 CFNetwork/485.13.9 Darwin/11.0.0", null, "AirPlayer"));
		testCases.add(new TestCase("User-Agent: Lavf52.54.0", null, "AirPlayer"));

		// BraviaEX:
		testCases.add(new TestCase(null, "X-AV-Client-Info: av=5.0; cn=\"Sony Corporation\"; mn=\"BRAVIA KDL-32CX520\"; mv=\"1.7\";", "Sony Bravia EX"));

		// BraviaHX:
		testCases.add(new TestCase(null, "X-AV-Client-Info: av=5.0; cn=\"Sony Corporation\"; mn=\"BRAVIA KDL-55HX750\"; mv=\"1.7\";", "Sony Bravia HX"));

		// DLinkDSM510:
		testCases.add(new TestCase("User-Agent: DLNADOC/1.50 INTEL_NMPR/2.1", null, "D-Link DSM-510"));

		// iPad-iPhone:
		testCases.add(new TestCase("User-Agent: 8player lite 2.2.3 (iPad; iPhone OS 5.0.1; nl_NL)", null, "iPad / iPhone"));
		testCases.add(new TestCase("User-Agent: yxplayer2%20lite/1.2.7 CFNetwork/485.13.9 Darwin/11.0.0", null, "iPad / iPhone"));
		testCases.add(new TestCase("User-Agent: MPlayer 1.0rc4-4.2.1", null, "iPad / iPhone"));
		testCases.add(new TestCase("User-Agent: NSPlayer/4.1.0.3856", null, "iPad / iPhone"));

		// Netgear NeoTV:
		testCases.add(new TestCase(null, "friendlyName.dlna.org: BD-Player", "Netgear NeoTV"));

		// Philips:
		testCases.add(new TestCase("User-Agent: Allegro-Software-WebClient/4.61 DLNADOC/1.00", null, "Philips Aurea"));

		// PhilipsPFL:
		testCases.add(new TestCase("User-Agent: Windows2000/0.0 UPnP/1.0 PhilipsIntelSDK/1.4 DLNADOC/1.50", null, "Philips TV"));

		// PS3:
		testCases.add(new TestCase("User-Agent: PLAYSTATION 3", "X-AV-Client-Info: av=5.0; cn=\"Sony Computer Entertainment Inc.\"; mn=\"PLAYSTATION 3\"; mv=\"1.0\"", "PlayStation 3"));

		// Realtek:
		// FIXME: Actual conflict here! Popcorn Hour is returned...
		//testCases.add(new TestCase("User-Agent: POSIX UPnP/1.0 Intel MicroStack/1.0.2718, RealtekMediaCenter, DLNADOC/1.50", null, "Realtek"));
		testCases.add(new TestCase("User-Agent: RealtekVOD neon/0.27.2", "RealtekMediaCenter: RealtekVOD", "Realtek"));

		// SamsungAllShare:
		testCases.add(new TestCase("User-Agent: SEC_HHP_[HT]D5500/1.0", null, "Samsung AllShare C/D"));
		testCases.add(new TestCase("User-Agent: SEC_HHP_[TV]UE32D5000/1.0", null, "Samsung AllShare C/D"));
		testCases.add(new TestCase("User-Agent: SEC_HHP_[TV]PS51D6900/1.0", null, "Samsung AllShare C/D"));
		testCases.add(new TestCase("User-Agent: DLNADOC/1.50 SEC_HHP_[TV]UE32D5000/1.0", null, "Samsung AllShare C/D"));
		testCases.add(new TestCase("User-Agent: DLNADOC/1.50 SEC_HHP_[TV]UN55D6050/1.0", null, "Samsung AllShare C/D"));
		testCases.add(new TestCase("User-Agent: SEC_HHP_ Family TV/1.0", null, "Samsung AllShare"));
		testCases.add(new TestCase("User-Agent: DLNADOC/1.50 SEC_HHP_ Family TV/1.0", null, "Samsung AllShare"));
		testCases.add(new TestCase("User-Agent: SEC_HHP_[TV]UE46ES8000/1.0 DLNADOC/1.50", null, "Samsung AllShare"));
		testCases.add(new TestCase("User-Agent: SEC_HHP_[TV]Samsung LED40/1.0 DLNADOC/1.50", null, "Samsung AllShare"));
		testCases.add(new TestCase("User-Agent: SEC_HHP_[TV]UN55ES6100/1.0 DLNADOC/1.50", null, "Samsung AllShare"));

		// Samsung-SMT-G7400:
		testCases.add(new TestCase("User-Agent: Linux/2.6.35 UPnP/1.0 NDS_MHF DLNADOC/1.50", null, "Samsung SMT-G7400"));

		// Sharp Aquos:
		testCases.add(new TestCase("User-Agent: DLNADOC/1.50 SHARP-AQUOS-DMP/1.1W", null, "Sharp Aquos"));

		// Showtime 3:
		testCases.add(new TestCase("User-Agent: Showtime 3.0", "X-AV-Client-Info: Showtime 3.0", "Showtime 3"));

		// Showtime 4:
		testCases.add(new TestCase("User-Agent: Showtime PS3 4.2", null, "Showtime 4"));

		// Telstra T-Box:
		// Note: This isn't the full user-agent, just a snippet to find it
		testCases.add(new TestCase("User-Agent: telstra", null, "Telstra T-Box"));

		// VideoWebTV:
		testCases.add(new TestCase(null, "friendlyName.dlna.org: VideoWeb", "VideoWeb TV"));

		// WDTVLive:
		testCases.add(new TestCase("User-Agent: INTEL_NMPR/2.1 DLNADOC/1.50 Intel MicroStack/1.0.1423", null, "WD TV Live"));

		// XBMC:
		testCases.add(new TestCase("User-Agent: XBMC/10.0 r35648 (Mac OS X; 11.2.0 x86_64; http://www.xbmc.org)", null, "XBMC"));
		testCases.add(new TestCase("User-Agent: Platinum/0.5.3.0, DLNADOC/1.50", null, "XBMC"));
	}

	/**
	 * Test the RendererConfiguration class and the consistency of the renderer
	 * .conf files it reads. This is done by feeding it known headers and
	 * checking whether it recognizes the correct renderer.
	 */
	@Test
	public void testKnownHeaders() {
		PmsConfiguration pmsConf = null;

		try {
			pmsConf = new PmsConfiguration(false);
		} catch (ConfigurationException e) {
			// This should be impossible since no configuration file will be loaded.
		}

		// Initialize the RendererConfiguration
		loadRendererConfigurations(pmsConf);

		// Test all header test cases
		Iterator<TestCase> i = testCases.iterator();

		while (i.hasNext()) {
			TestCase item = i.next();
			testHeaders(item.UA, item.UAAHH, item.renderer);
		}
	}

	/**
	 * Test recognition with a forced default renderer configured.
	 */
	@Test
	public void testForcedDefault() {
		PmsConfiguration pmsConf = null;

		try {
			pmsConf = new PmsConfiguration(false);

			// Set default to PlayStation 3
			pmsConf.setRendererDefault("PlayStation 3");
			pmsConf.setRendererForceDefault(true);

			// Initialize the RendererConfiguration
			loadRendererConfigurations(pmsConf);

			// Known and unknown renderers should always return default
			testHeaders("User-Agent: AirPlayer/1.0.09 CFNetwork/485.13.9 Darwin/11.0.0", null, "PlayStation 3");
			testHeaders("User-Agent: Unknown Renderer", null, "PlayStation 3");
			testHeaders(null, "X-Unknown-Header: Unknown Content", "PlayStation 3");
		} catch (ConfigurationException e) {
			// This should be impossible since no configuration file will be loaded.
		}
	}

	/**
	 * Test recognition with a forced bogus default renderer configured.
	 */
	@Test
	public void testBogusDefault() {
		PmsConfiguration pmsConf = null;

		try {
			pmsConf = new PmsConfiguration(false);

			// Set default to non existent renderer
			pmsConf.setRendererDefault("Bogus Renderer");
			pmsConf.setRendererForceDefault(true);

			// Initialize the RendererConfiguration
			loadRendererConfigurations(pmsConf);

			// Known and unknown renderers should return "Unknown renderer"
			testHeaders("User-Agent: AirPlayer/1.0.09 CFNetwork/485.13.9 Darwin/11.0.0", null, "Unknown renderer");
			testHeaders("User-Agent: Unknown Renderer", null, "Unknown renderer");
			testHeaders(null, "X-Unknown-Header: Unknown Content", "Unknown renderer");
		} catch (ConfigurationException e) {
			// This should be impossible since no configuration file will be loaded.
		}
	}

	/**
	 * Test a particular set of headers to see if it returns the correct
	 * renderer. Set the correct renderer name to <code>null</code> to require
	 * that nothing matches at all.
	 * 
	 * @param UA
	 *            The raw User-Agent header line to recognize, can be null
	 * @param UAAHH
	 *            The raw additional header line to recognize, can be null
	 * @param correctRendererName
	 *            The name of the renderer.
	 */
	private void testHeaders(String UA, String UAAHH, String correctRendererName) {
		SortedHeaderMap headers = new SortedHeaderMap();
		headers.put(UA);
		headers.put(UAAHH);
		RendererConfiguration rc = getRendererConfigurationByHeaders(headers);
		if (correctRendererName != null) {
			// Headers are supposed to match a particular renderer
			assertNotNull("Recognized renderer for header \"" + headers + "\"", rc);
			assertEquals("Expected renderer \"" + correctRendererName + "\", " +
				"instead renderer \"" + rc.getRendererName() + "\" was returned for header(s) \"" +
				headers + "\"", correctRendererName, rc.getRendererName());
		} else {
			// Headers are supposed to match no renderer at all
			assertEquals("Expected no matching renderer to be found for header(s) \"" + headers +
				"\", instead renderer \"" + (rc != null ? rc.getRendererName() : "") +
				"\" was recognized.", null,
				rc);
		}
	}
}
