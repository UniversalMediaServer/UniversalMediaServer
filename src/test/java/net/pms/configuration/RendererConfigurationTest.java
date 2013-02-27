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
import java.io.IOException;
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
	private final Map<String, String> testCases = new HashMap<String, String>();

	@Before
	public void setUp() {
		// Silence all log messages from the PMS code that is being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.reset(); 

		// Set locale to EN to ignore translations for renderers
		Locale.setDefault(Locale.ENGLISH);

		// Cases that are too generic should not match anything
		testCases.put("User-Agent: UPnP/1.0 DLNADOC/1.50", null);
		testCases.put("User-Agent: Unknown Renderer", null);
		testCases.put("X-Unknown-Header: Unknown Content", null);

		// AirPlayer:
		testCases.put("User-Agent: AirPlayer/1.0.09 CFNetwork/485.13.9 Darwin/11.0.0", "AirPlayer");
		testCases.put("User-Agent: Lavf52.54.0", "AirPlayer");

		// BraviaEX:
		testCases.put("X-AV-Client-Info: av=5.0; cn=\"Sony Corporation\"; mn=\"BRAVIA KDL-32CX520\"; mv=\"1.7\";", "Sony Bravia EX");

		// BraviaHX:
		testCases.put("X-AV-Client-Info: av=5.0; cn=\"Sony Corporation\"; mn=\"BRAVIA KDL-55HX750\"; mv=\"1.7\";", "Sony Bravia HX");

		// DLinkDSM510:
		testCases.put("User-Agent: DLNADOC/1.50 INTEL_NMPR/2.1", "D-Link DSM-510");

		// iPad-iPhone:
		testCases.put("User-Agent: 8player lite 2.2.3 (iPad; iPhone OS 5.0.1; nl_NL)", "iPad / iPhone");
		testCases.put("User-Agent: yxplayer2%20lite/1.2.7 CFNetwork/485.13.9 Darwin/11.0.0", "iPad / iPhone");
		testCases.put("User-Agent: MPlayer 1.0rc4-4.2.1", "iPad / iPhone");
		testCases.put("User-Agent: NSPlayer/4.1.0.3856", "iPad / iPhone");

		// Netgear NeoTV:
		testCases.put("User-Agent: IPI/1.0 UPnP/1.0 DLNADOC/1.50, friendlyName.dlna.org: BD-Player", "Netgear NeoTV");

		// Philips:
		testCases.put("User-Agent: Allegro-Software-WebClient/4.61 DLNADOC/1.00", "Philips Aurea");

		// PhilipsPFL:
		testCases.put("User-Agent: Windows2000/0.0 UPnP/1.0 PhilipsIntelSDK/1.4 DLNADOC/1.50", "Philips TV");

		// PS3:
		testCases.put("User-Agent: PLAYSTATION 3", "PlayStation 3");
		testCases.put("X-AV-Client-Info: av=5.0; cn=\"Sony Computer Entertainment Inc.\"; mn=\"PLAYSTATION 3\"; mv=\"1.0\"", "PlayStation 3");

		// Realtek:
		// FIXME: Actual conflict here! Popcorn Hour is returned...
		//testCases.put("User-Agent: POSIX UPnP/1.0 Intel MicroStack/1.0.2718, RealtekMediaCenter, DLNADOC/1.50", "Realtek");
		testCases.put("User-Agent: RealtekVOD neon/0.27.2", "Realtek");

		// SamsungAllShare:
		testCases.put("User-Agent: SEC_HHP_[HT]D5500/1.0", "Samsung AllShare");
		testCases.put("User-Agent: SEC_HHP_[TV]UE32D5000/1.0", "Samsung AllShare");
		testCases.put("User-Agent: SEC_HHP_ Family TV/1.0", "Samsung AllShare");
		testCases.put("User-Agent: SEC_HHP_[TV]PS51D6900/1.0", "Samsung AllShare");
		testCases.put("User-Agent: DLNADOC/1.50 SEC_HHP_[TV]UE32D5000/1.0", "Samsung AllShare");
		testCases.put("User-Agent: DLNADOC/1.50 SEC_HHP_[TV]UN55D6050/1.0", "Samsung AllShare");
		testCases.put("User-Agent: DLNADOC/1.50 SEC_HHP_ Family TV/1.0", "Samsung AllShare");

		// Samsung-SMT-G7400:
		testCases.put("User-Agent: Linux/2.6.35 UPnP/1.0 NDS_MHF DLNADOC/1.50", "Samsung SMT-G7400");

		// Sharp Aquos:
		testCases.put("User-Agent: DLNADOC/1.50 SHARP-AQUOS-DMP/1.1W", "Sharp Aquos");

		// Showtime 3:
		testCases.put("User-Agent: Showtime 3.0", "Showtime 3");

		// Showtime 4:
		testCases.put("User-Agent: Showtime PS3 4.2", "Showtime 4");

		// Telstra T-Box:
		// Note: This isn't the full user-agent, just a snippet to find it
		testCases.put("User-Agent: telstra", "Telstra T-Box");

		// VideoWebTV:
		testCases.put("friendlyName.dlna.org: VideoWeb", "VideoWeb TV");

		// WDTVLive:
		testCases.put("User-Agent: INTEL_NMPR/2.1 DLNADOC/1.50 Intel MicroStack/1.0.1423", "WD TV Live");

		// XBMC:
		testCases.put("User-Agent: XBMC/10.0 r35648 (Mac OS X; 11.2.0 x86_64; http://www.xbmc.org)", "XBMC");
		testCases.put("User-Agent: Platinum/0.5.3.0, DLNADOC/1.50", "XBMC");
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
		} catch (IOException e) {
			// This should be impossible since no configuration file will be loaded.
		} catch (ConfigurationException e) {
			// This should be impossible since no configuration file will be loaded.
		}

		// Initialize the RendererConfiguration
		loadRendererConfigurations(pmsConf);

		// Test all header test cases
		Set<Entry<String, String>> set = testCases.entrySet();
		Iterator<Entry<String, String>> i = set.iterator();

		while (i.hasNext()) {
			Entry<String, String> entry = (Entry<String, String>) i.next();
			testHeader(entry.getKey(), entry.getValue());
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
		} catch (IOException e) {
			// This should be impossible since no configuration file will be loaded.
		} catch (ConfigurationException e) {
			// This should be impossible since no configuration file will be loaded.
		}

		// Set default to PlayStation 3
		pmsConf.setRendererDefault("PlayStation 3");
		pmsConf.setRendererForceDefault(true);

		// Initialize the RendererConfiguration
		loadRendererConfigurations(pmsConf);

		// Known and unknown renderers should always return default
		testHeader("User-Agent: AirPlayer/1.0.09 CFNetwork/485.13.9 Darwin/11.0.0", "PlayStation 3");
		testHeader("User-Agent: Unknown Renderer", "PlayStation 3");
		testHeader("X-Unknown-Header: Unknown Content", "PlayStation 3");
	}

	/**
	 * Test recognition with a forced bogus default renderer configured.
	 */
	@Test
	public void testBogusDefault() {
		PmsConfiguration pmsConf = null;

		try {
			pmsConf = new PmsConfiguration(false);
		} catch (IOException e) {
			// This should be impossible since no configuration file will be loaded.
		} catch (ConfigurationException e) {
			// This should be impossible since no configuration file will be loaded.
		}

		// Set default to non existent renderer
		pmsConf.setRendererDefault("Bogus Renderer");
		pmsConf.setRendererForceDefault(true);

		// Initialize the RendererConfiguration
		loadRendererConfigurations(pmsConf);

		// Known and unknown renderers should return "Unknown renderer"
		testHeader("User-Agent: AirPlayer/1.0.09 CFNetwork/485.13.9 Darwin/11.0.0", "Unknown renderer");
		testHeader("User-Agent: Unknown Renderer", "Unknown renderer");
		testHeader("X-Unknown-Header: Unknown Content", "Unknown renderer");
	}

	/**
	 * Test one particular header line to see if it returns the correct
	 * renderer. Set the correct renderer name to <code>null</code> to require
	 * that nothing matches at all.
	 * 
	 * @param headerLine
	 *            The header line to recognize.
	 * @param correctRendererName
	 *            The name of the renderer.
	 */
	private void testHeader(String headerLine, String correctRendererName) {
		if (correctRendererName != null) {
			// Header is supposed to match a particular renderer
			if (headerLine != null && headerLine.toLowerCase().startsWith("user-agent")) {
				// Match by User-Agent
					RendererConfiguration rc = getRendererConfigurationByUA(headerLine);
					assertNotNull("Recognized renderer for header \"" + headerLine + "\"", rc);
					assertEquals("Expected renderer \"" + correctRendererName + "\", "
							+ "instead renderer \"" + rc.getRendererName() + "\" was returned for header \""
							+ headerLine + "\"", correctRendererName, rc.getRendererName());
			} else {
				// Match by additional header
					RendererConfiguration rc = getRendererConfigurationByUAAHH(headerLine);
					assertNotNull("Recognized renderer for header \"" + headerLine + "\"", rc);
					assertEquals("Expected renderer \"" + correctRendererName + "\" to be recognized, "
							+ "instead renderer \"" + rc.getRendererName() + "\" was returned for header \""
							+ headerLine + "\"", correctRendererName, rc.getRendererName());
			}
		} else {
			// Header is supposed to match no renderer at all
			if (headerLine != null && headerLine.toLowerCase().startsWith("user-agent")) {
				// Match by User-Agent
					RendererConfiguration rc = getRendererConfigurationByUA(headerLine);
					assertEquals("Expected no matching renderer to be found for header \"" + headerLine
							+ "\", instead renderer \"" + (rc != null ? rc.getRendererName() : "")
							+ "\" was recognized.", null,
							rc);
			} else {
				// Match by additional header
					RendererConfiguration rc = getRendererConfigurationByUAAHH(headerLine);
					assertEquals("Expected no matching renderer to be found for header \"" + headerLine
							+ "\", instead renderer \"" + (rc != null ? rc.getRendererName() : "")
							+ "\" was recognized.", null, rc);
			}
		}
	}
}
