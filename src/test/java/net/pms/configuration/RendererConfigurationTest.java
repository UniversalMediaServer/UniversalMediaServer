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
import net.pms.configuration.RendererConfiguration.SortedHeaderMap;
import static net.pms.configuration.RendererConfiguration.getRendererConfigurationByHeaders;
import static net.pms.configuration.RendererConfiguration.loadRendererConfigurations;
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
	@Before
	public void setUp() {
		// Silence all log messages from the PMS code that is being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.reset(); 

		// Set locale to EN to ignore translations for renderers
		Locale.setDefault(Locale.ENGLISH);
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

		// Known headers

		// Cases that are too generic should not match anything
		testHeaders(null, "User-Agent: Microsoft-Windows/6.2 UPnP/1.0 Microsoft-DLNA DLNADOC/1.50");
		testHeaders(null, "User-Agent: UPnP/1.0 DLNADOC/1.50");
		testHeaders(null, "User-Agent: Unknown Renderer");
		testHeaders(null, "X-Unknown-Header: Unknown Content");

		// AirPlayer:
		testHeaders("AirPlayer", "User-Agent: AirPlayer/1.0.09 CFNetwork/485.13.9 Darwin/11.0.0");
		testHeaders("AirPlayer", "User-Agent: Lavf52.54.0");

		// BraviaEX:
		testHeaders("Sony Bravia EX", "X-AV-Client-Info: av=5.0; cn=\"Sony Corporation\"; mn=\"BRAVIA KDL-32CX520\"; mv=\"1.7\";");

		// BraviaHX:
		testHeaders("Sony Bravia HX", "X-AV-Client-Info: av=5.0; cn=\"Sony Corporation\"; mn=\"BRAVIA KDL-55HX750\"; mv=\"1.7\";");

		// DLinkDSM510:
		testHeaders("D-Link DSM-510", "User-Agent: DLNADOC/1.50 INTEL_NMPR/2.1");

		// iPad-iPhone:
		testHeaders("iPad / iPhone", "User-Agent: 8player lite 2.2.3 (iPad; iPhone OS 5.0.1; nl_NL)");
		testHeaders("iPad / iPhone", "User-Agent: yxplayer2%20lite/1.2.7 CFNetwork/485.13.9 Darwin/11.0.0");
		testHeaders("iPad / iPhone", "User-Agent: MPlayer 1.0rc4-4.2.1");
		testHeaders("iPad / iPhone", "User-Agent: NSPlayer/4.1.0.3856");

		// Microsoft Xbox One:
		testHeaders("Xbox One", "FriendlyName.DLNA.ORG: Xbox-SystemOS");
		testHeaders("Xbox One", "FriendlyName.DLNA.ORG: XboxOne");
		testHeaders("Xbox One", "User-Agent: NSPlayer/12.00.9600.16411 WMFSDK/12.00.9600.16411");

		// Netgear NeoTV:
		testHeaders("Netgear NeoTV", "friendlyName.dlna.org: BD-Player");

		// Philips:
		testHeaders("Philips Aurea", "User-Agent: Allegro-Software-WebClient/4.61 DLNADOC/1.00");

		// PhilipsPFL:
		testHeaders("Philips TV", "User-Agent: Windows2000/0.0 UPnP/1.0 PhilipsIntelSDK/1.4 DLNADOC/1.50");

		// PS3:
		testHeaders("PlayStation 3", "User-Agent: PLAYSTATION 3", "X-AV-Client-Info: av=5.0; cn=\"Sony Computer Entertainment Inc.\"; mn=\"PLAYSTATION 3\"; mv=\"1.0\"");

		// Realtek:
		// FIXME: Actual conflict here! Popcorn Hour is returned...
		//testHeaders("Realtek", "User-Agent: POSIX UPnP/1.0 Intel MicroStack/1.0.2718, RealtekMediaCenter, DLNADOC/1.50");
		testHeaders("Realtek", "User-Agent: RealtekVOD neon/0.27.2", "RealtekMediaCenter: RealtekVOD");

		// SamsungAllShare:
		testHeaders("Samsung AllShare C/D", "User-Agent: SEC_HHP_[HT]D5500/1.0");
		testHeaders("Samsung AllShare C/D", "User-Agent: SEC_HHP_[TV]UE32D5000/1.0");
		testHeaders("Samsung AllShare C/D", "User-Agent: SEC_HHP_[TV]PS51D6900/1.0");
		testHeaders("Samsung AllShare C/D", "User-Agent: DLNADOC/1.50 SEC_HHP_[TV]UE32D5000/1.0");
		testHeaders("Samsung AllShare C/D", "User-Agent: DLNADOC/1.50 SEC_HHP_[TV]UN55D6050/1.0");
		testHeaders("Samsung AllShare", "User-Agent: SEC_HHP_ Family TV/1.0");
		testHeaders("Samsung AllShare", "User-Agent: DLNADOC/1.50 SEC_HHP_ Family TV/1.0");
		testHeaders("Samsung ES8000", "User-Agent: SEC_HHP_[TV]UE46ES8000/1.0 DLNADOC/1.50");
		testHeaders("Samsung AllShare", "User-Agent: SEC_HHP_[TV]Samsung LED40/1.0 DLNADOC/1.50");
		testHeaders("Samsung AllShare", "User-Agent: SEC_HHP_[TV]UN55ES6100/1.0 DLNADOC/1.50");

		// Samsung-SMT-G7400:
		testHeaders("Samsung SMT-G7400", "User-Agent: Linux/2.6.35 UPnP/1.0 NDS_MHF DLNADOC/1.50");

		// Sharp Aquos:
		testHeaders("Sharp Aquos", "User-Agent: DLNADOC/1.50 SHARP-AQUOS-DMP/1.1W");

		// Showtime 3:
		testHeaders("Showtime 3", "User-Agent: Showtime 3.0", "X-AV-Client-Info: Showtime 3.0");

		// Showtime 4:
		testHeaders("Showtime 4", "User-Agent: Showtime PS3 4.2");

		// Sony Xperia:
		testHeaders("Sony Xperia Z/ZL/ZQ/Z1/Z2", "X-AV-Client-Info: C6603");

		// Telstra T-Box:
		// Note: This isn't the full user-agent, just a snippet to find it
		testHeaders("Telstra T-Box", "User-Agent: telstra");

		// VideoWebTV:
		testHeaders("VideoWeb TV", "friendlyName.dlna.org: VideoWeb");

		// WDTVLive:
		testHeaders("WD TV Live", "User-Agent: INTEL_NMPR/2.1 DLNADOC/1.50 Intel MicroStack/1.0.1423");

		// XBMC:
		testHeaders("XBMC", "User-Agent: XBMC/10.0 r35648 (Mac OS X; 11.2.0 x86_64; http://www.xbmc.org)");
		testHeaders("XBMC", "User-Agent: Platinum/0.5.3.0, DLNADOC/1.50");
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
			testHeaders("PlayStation 3", "User-Agent: AirPlayer/1.0.09 CFNetwork/485.13.9 Darwin/11.0.0");
			testHeaders("PlayStation 3", "User-Agent: Unknown Renderer");
			testHeaders("PlayStation 3", "X-Unknown-Header: Unknown Content");
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
			testHeaders("Unknown renderer", "User-Agent: AirPlayer/1.0.09 CFNetwork/485.13.9 Darwin/11.0.0");
			testHeaders("Unknown renderer", "User-Agent: Unknown Renderer");
			testHeaders("Unknown renderer", "X-Unknown-Header: Unknown Content");
		} catch (ConfigurationException e) {
			// This should be impossible since no configuration file will be loaded.
		}
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
	private void testHeaders(String correctRendererName, String... headerLines) {
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
}
