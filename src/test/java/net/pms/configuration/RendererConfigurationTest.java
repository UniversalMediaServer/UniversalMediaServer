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
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration.SortedHeaderMap;
import static net.pms.configuration.RendererConfiguration.getRendererConfigurationByHeaders;
import static net.pms.configuration.RendererConfiguration.getRendererConfigurationByUPNPDetails;
import static net.pms.configuration.RendererConfiguration.loadRendererConfigurations;
import org.apache.commons.configuration.ConfigurationException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

/**
 * Test the RendererConfiguration class
 */
public class RendererConfigurationTest {
	@BeforeClass
	public static void SetUPClass() {
		PMS.configureJNA();
	}
	
	@Before
	public void setUp() {
		// Silence all log messages from the PMS code that is being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.reset();

		// Set locale to EN to ignore translations for renderers
		Locale.setDefault(Locale.ENGLISH);
		PMS.setLocale(Locale.ENGLISH);
	}

	/**
	 * Test the RendererConfiguration class and the consistency of the renderer
	 * .conf files it reads. This is done by feeding it known headers and
	 * checking whether it recognizes the correct renderer.
	 * @throws ConfigurationException
	 */
	@Test
	public void testKnownHeaders() throws ConfigurationException {
		PmsConfiguration pmsConf = null;

		pmsConf = new PmsConfiguration(false);

		// Initialize the RendererConfiguration
		loadRendererConfigurations(pmsConf);

		// Cases that are too generic should not match anything
		testHeaders(
			null,
			"User-Agent: Microsoft-Windows/6.2 UPnP/1.0 Microsoft-DLNA DLNADOC/1.50",
			"User-Agent: UPnP/1.0 DLNADOC/1.50",
			"User-Agent: Unknown Renderer",
			"X-Unknown-Header: Unknown Content"
		);

		// Known headers
		testHeaders(
			"AirPlayer",
			"User-Agent: AirPlayer/1.0.09 CFNetwork/485.13.9 Darwin/11.0.0",
			"User-Agent: Lavf52.54.0",
			"User-Agent: VLC%20for%20iOS/22 CFNetwork/711.5.6 Darwin/14.0.0"
		);

		testHeaders(
			"Apple iPad / iPhone",
			"User-Agent: 8player lite 2.2.3 (iPad; iPhone OS 5.0.1; nl_NL)",
			"User-Agent: yxplayer2%20lite/1.2.7 CFNetwork/485.13.9 Darwin/11.0.0",
			"User-Agent: MPlayer 1.0rc4-4.2.1",
			"User-Agent: NSPlayer/4.1.0.3856"
		);

		testHeaders("D-Link DSM-510", "User-Agent: DLNADOC/1.50 INTEL_NMPR/2.1");

		testHeaders("Fetch TV", "User-Agent: Takin/3.0.0 (Linux arm ; U; en), FetchTV_STB_BCM7252S/3.7.7244 (FetchTV, M616T, Wireless)");

		testHeaders    ("LG WebOS TV", "User-Agent: Linux/3.10.19-32.afro.4 UPnP/1.0 LGE WebOS TV LGE_DLNA_SDK/1.6.0/04.30.13 DLNADOC/1.50");
		testUPNPDetails("LG WebOS TV", "modelDescription=LG WebOSTV DMRplus");
		testUPNPDetails("LG WebOS TV", "friendlyName=LG-webOSTV");
		testUPNPDetails("LG WebOS TV", "friendlyName=[LG] webOS TV");
		testUPNPDetails("LG WebOS TV", "DLNADeviceName.lge.com=LG-webOSTV");

		testUPNPDetails("Panasonic AS650", "modelNumber=TC-50AS650U");

		testHeaders("Philips Aurea", "User-Agent: Allegro-Software-WebClient/4.61 DLNADOC/1.00");

		testHeaders(
			"Philips TV",
			"User-Agent: Windows2000/0.0 UPnP/1.0 PhilipsIntelSDK/1.4 DLNADOC/1.50",
			"User-Agent: Streamium/1.0"
		);

		testHeaders(
			"PlayStation 3",
			"User-Agent: PLAYSTATION 3",
			"X-AV-Client-Info: av=5.0; cn=\"Sony Computer Entertainment Inc.\"; mn=\"PLAYSTATION 3\"; mv=\"1.0\";"
		);

		testHeaders(
			"PlayStation 4",
			"User-Agent: PS4Application libhttp/1.000 (PS4) libhttp/2.51 (PlayStation 4)",
			"User-Agent: libhttp/2.51 (PlayStation 4)"
		);

		// FIXME: Actual conflict here! Popcorn Hour is returned...
		//testHeaders("Realtek", "User-Agent: POSIX UPnP/1.0 Intel MicroStack/1.0.2718, RealtekMediaCenter, DLNADOC/1.50");
		testHeaders(
			"Realtek",
			"User-Agent: RealtekVOD neon/0.27.2",
			"RealtekMediaCenter: RealtekVOD"
		);

		testHeaders(
			"Roku 3 (NSP 6-7)",
			"User-Agent: Roku/DVP-6.x",
			"User-Agent: Roku/DVP-7.x"
		);

		testHeaders(
			"Roku TV (NSP 8)",
			"User-Agent: Roku/DVP-8.0 (308.00E04156A)",
			"User-Agent: Roku/DVP-8.1 (508.01E04018A)"
		);

		testHeaders(
			"Samsung C/D Series",
			"User-Agent: SEC_HHP_[HT]D5500/1.0",
			"User-Agent: SEC_HHP_[TV]UE32D5000/1.0",
			"User-Agent: SEC_HHP_[TV]PS51D6900/1.0",
			"User-Agent: DLNADOC/1.50 SEC_HHP_[TV]UE32D5000/1.0",
			"User-Agent: DLNADOC/1.50 SEC_HHP_[TV]UN55D6050/1.0"
		);

		testHeaders(
			"Samsung E+ Series",
			"User-Agent: SEC_HHP_ Family TV/1.0",
			"User-Agent: DLNADOC/1.50 SEC_HHP_ Family TV/1.0",
			"User-Agent: SEC_HHP_[TV]Samsung LED40/1.0 DLNADOC/1.50",
			"User-Agent: SEC_HHP_[TV]UN55ES6100/1.0 DLNADOC/1.50"
		);

		testHeaders("Samsung ES8000", "User-Agent: SEC_HHP_[TV]UE46ES8000/1.0 DLNADOC/1.50");

		testHeaders("Samsung SMT-G7400", "User-Agent: Linux/2.6.35 UPnP/1.0 NDS_MHF DLNADOC/1.50");

		testHeaders("Samsung LED UHD", "USER-AGENT: DLNADOC/1.50 SEC_HHP_[TV] UE88KS9810/1.0 UPnP/1.0");
		testUPNPDetails("Samsung LED UHD", "modelName=UE88KS9810");

		testHeaders("Sharp Aquos", "User-Agent: DLNADOC/1.50 SHARP-AQUOS-DMP/1.1W");

		testHeaders("Showtime 3", "User-Agent: Showtime 3.0", "X-AV-Client-Info: Showtime 3.0");

		testHeaders("Showtime 4", "User-Agent: Showtime PS3 4.2");

		testHeaders("Sony Bravia EX", "X-AV-Client-Info: av=5.0; cn=\"Sony Corporation\"; mn=\"BRAVIA KDL-32CX520\"; mv=\"1.7\";");

		testHeaders("Sony Bravia HX", "X-AV-Client-Info: av=5.0; cn=\"Sony Corporation\"; mn=\"BRAVIA KDL-55HX750\"; mv=\"1.7\";");

		testHeaders("Sony Bravia W",
			"X-AV-Physical-Unit-Info: pa=\"BRAVIA KDL-48W600B\";",
			"X-AV-Client-Info: av=5.0; cn=\"Sony Corporation\"; mn=\"BRAVIA KDL-48W600B\"; mv=\"1.7\";"
		);

		testHeaders("Sony Xperia Z/ZL/ZQ/Z1/Z2", "X-AV-Client-Info: C6603");

		// Note: This isn't the full user-agent, just a snippet to find it
		testHeaders("Telstra T-Box", "User-Agent: telstra");

		testHeaders("VideoWeb TV", "friendlyName.dlna.org: VideoWeb");

		testHeaders("WD TV Live", "User-Agent: INTEL_NMPR/2.1 DLNADOC/1.50 Intel MicroStack/1.0.1423");

		testHeaders("XBMC", "User-Agent: XBMC/10.0 r35648 (Mac OS X; 11.2.0 x86_64; http://www.xbmc.org)");
		testHeaders("XBMC", "User-Agent: Platinum/0.5.3.0, DLNADOC/1.50");

		testHeaders(
			"Xbox One",
			"FriendlyName.DLNA.ORG: Xbox-SystemOS",
			"FriendlyName.DLNA.ORG: XboxOne",
			"User-Agent: NSPlayer/12.00.9600.16411 WMFSDK/12.00.9600.16411"
		);
	}

	/**
	 * Test recognition with a forced default renderer configured.
	 * @throws ConfigurationException
	 */
	@Test
	public void testForcedDefault() throws ConfigurationException {
		PmsConfiguration pmsConf = null;

		pmsConf = new PmsConfiguration(false);

		// Set default to PlayStation 3
		pmsConf.setRendererDefault("PlayStation 3");
		pmsConf.setRendererForceDefault(true);

		// Initialize the RendererConfiguration
		loadRendererConfigurations(pmsConf);

		// Known and unknown renderers should always return default
		testHeaders(
			"PlayStation 3",
			"User-Agent: AirPlayer/1.0.09 CFNetwork/485.13.9 Darwin/11.0.0",
			"User-Agent: Unknown Renderer",
			"X-Unknown-Header: Unknown Content"
		);
	}

	/**
	 * Test recognition with a forced bogus default renderer configured.
	 * @throws ConfigurationException
	 */
	@Test
	public void testBogusDefault() throws ConfigurationException {
		PmsConfiguration pmsConf = null;

		pmsConf = new PmsConfiguration(false);

		// Set default to non existent renderer
		pmsConf.setRendererDefault("Bogus Renderer");
		pmsConf.setRendererForceDefault(true);

		// Initialize the RendererConfiguration
		loadRendererConfigurations(pmsConf);

		// Known and unknown renderers should return "Unknown renderer"
		testHeaders(
			"Unknown renderer",
			"User-Agent: AirPlayer/1.0.09 CFNetwork/485.13.9 Darwin/11.0.0",
			"User-Agent: Unknown Renderer",
			"X-Unknown-Header: Unknown Content"
		);
	}

	/**
	 * Test a particular set of headers to see if it returns the correct
	 * renderer. Set the correct renderer name to <code>null</code> to require
	 * that nothing matches at all.
	 *
	 * @param correctRendererName
	 *            The name of the renderer.
	 * @param headerLines
	 *            One or more raw header lines.
	 */
	private static void testHeaders(String correctRendererName, String... headerLines) {
		SortedHeaderMap headers = new SortedHeaderMap();
		for (String header : headerLines) {
			headers.put(header);
		}
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

	/**
	 * Test a UPnP string to see if it returns the correct
	 * renderer. Set the correct renderer name to <code>null</code> to require
	 * that nothing matches at all.
	 *
	 * @param correctRendererName The name of the renderer
	 * @param upnpDetails         One or more raw header lines
	 */
	private void testUPNPDetails(String correctRendererName, String upnpDetails) {
		RendererConfiguration rc = getRendererConfigurationByUPNPDetails(upnpDetails);
		if (correctRendererName != null) {
			// Headers are supposed to match a particular renderer
			assertNotNull("Recognized renderer for upnpDetails \"" + upnpDetails + "\"", rc);
			assertEquals("Expected renderer \"" + correctRendererName + "\", " +
				"instead renderer \"" + rc.getRendererName() + "\" was returned for upnpDetails \"" +
				upnpDetails + "\"", correctRendererName, rc.getRendererName());
		} else {
			// Headers are supposed to match no renderer at all
			assertEquals("Expected no matching renderer to be found for upnpDetails \"" + upnpDetails +
				"\", instead renderer \"" + (rc != null ? rc.getRendererName() : "") +
				"\" was recognized.", null,
				rc);
		}
	}
}
