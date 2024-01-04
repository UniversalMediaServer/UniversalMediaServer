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
package net.pms.configuration;

import ch.qos.logback.classic.LoggerContext;
import java.util.*;
import net.pms.PMS;
import net.pms.util.SortedHeaderMap;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Test the RendererConfiguration class
 */
public class RendererConfigurationTest {
	UmsConfiguration prevConf;

	@BeforeAll
	public static void SetUPClass() {
		PMS.configureJNA();
	}

	@BeforeEach
	public void setUp() {
		// Silence all log messages from the PMS code that is being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.reset();

		// Set locale to EN to ignore translations for renderers
		Locale.setDefault(Locale.ENGLISH);
		PMS.setLocale(Locale.ENGLISH);
		prevConf = PMS.getConfiguration();
	}

	@AfterEach
	public void tearDown() {
		PMS.setConfiguration(prevConf);
	}

	/**
	 * Test the RendererConfiguration class and the consistency of the renderer
	 * .conf files it reads. This is done by feeding it known headers and
	 * checking whether it recognizes the correct renderer.
	 * @throws ConfigurationException
	 * @throws InterruptedException
	 */
	@Test
	public void testKnownHeaders() throws ConfigurationException, InterruptedException {
		UmsConfiguration pmsConf = new UmsConfiguration(false);

		// Initialize the RendererConfiguration
		PMS.setConfiguration(pmsConf);
		RendererConfigurations.loadRendererConfigurations();

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

		testUPNPDetails("Denon AVR-4311CI", "manufacturer=DENON, modelName=AVR-4311");

		testUPNPDetails("Denon AVR-X4200W", "manufacturer=Denon, modelName=*AVR-X4200W");

		testHeaders("Fetch TV", "User-Agent: Takin/3.0.0 (Linux arm ; U; en), FetchTV_STB_BCM7252S/3.7.7244 (FetchTV, M616T, Wireless)");

		testHeaders    ("LG Blu-ray Player (BP)", "User-Agent: LG-BP350");
		testUPNPDetails("LG Blu-ray Player (BP)", "friendlyName=LG-BP350");

		testHeaders    ("LG Blu-ray Player (BDP)", "User-Agent: LG-BDP450");
		testUPNPDetails("LG Blu-ray Player (BDP)", "friendlyName=LG-BDP450");

		testHeaders    ("LG Blu-ray Player (BP550)", "User-Agent: LG-BP550-1");
		testUPNPDetails("LG Blu-ray Player (BP550)", "friendlyName=LG-BP550-1");

		testUPNPDetails("LG EG910V", "modelDescription=webOS TV EG910V");

		testUPNPDetails("LG LCD TV (2014)", "friendlyName=[TV][LG]42LB5700-ZB");

		testUPNPDetails("LG LM660", "friendlyName=[TV]42LM660S-ZA");

		testUPNPDetails("LG LS5700", "friendlyName=[TV]42LS5700-SB");

		testUPNPDetails("LG OLED", "modelNumber=OLED65C9PUA");
		testUPNPDetails("LG OLED", "friendlyName=[LG] webOS TV OLED65C9PUA");
		testUPNPDetails("LG OLED", "modelNumber=OLED55E9PUA");
		testUPNPDetails("LG OLED", "modelNumber=OLED55C9PUA");
		testUPNPDetails("LG OLED", "modelNumber=OLED55C9AUA");
		testUPNPDetails("LG OLED", "modelNumber=OLED55B9PUA");
		testUPNPDetails("LG OLED", "modelNumber=OLED55B9PUB");
		testUPNPDetails("LG OLED", "modelNumber=OLED65E9PUA");
		testUPNPDetails("LG OLED", "modelNumber=OLED65E9AUA");
		testUPNPDetails("LG OLED", "modelNumber=OLED65C9PUA");
		testUPNPDetails("LG OLED", "modelNumber=OLED65C9AUA");
		testUPNPDetails("LG OLED", "modelNumber=OLED65B9PUA");
		testUPNPDetails("LG OLED", "modelNumber=OLED65B9PUB");
		testUPNPDetails("LG OLED", "modelNumber=OLED77C9PUB");
		testUPNPDetails("LG OLED", "modelNumber=OLED77C9AUB");
		testUPNPDetails("LG OLED", "modelNumber=OLED77B9PUA");
		testUPNPDetails("LG OLED", "modelNumber=OLED55B9SLA");
		testUPNPDetails("LG OLED", "friendlyName=[LG] webOS TV OLED55B9SLA");

		testUPNPDetails("LG OLED 2020+", "modelNumber=OLED65C24LA");

		testUPNPDetails("LG TV 2023+", "modelNumber=UR73003LA");
		testUPNPDetails("LG TV 2023+", "friendlyName=[LG] webOS TV OLED65C3AUA");

		testHeaders    ("LG UB820V", "User-Agent: Linux/3.0.13 UPnP/1.0 LGE_DLNA_SDK/1.6.0 [TV][LG]42UB820V-ZH/04.02.00 DLNADOC/1.50");

		testUPNPDetails("LG UH770", "friendlyName=[LG] webOS TV UH770V");

		testHeaders    ("LG WebOS TV", "User-Agent: Linux/3.10.19-32.afro.4 UPnP/1.0 LGE WebOS TV LGE_DLNA_SDK/1.6.0/04.30.13 DLNADOC/1.50");
		testUPNPDetails("LG WebOS TV", "modelDescription=LG WebOSTV DMRplus");
		testUPNPDetails("LG WebOS TV", "friendlyName=LG-webOSTV");
		testUPNPDetails("LG WebOS TV", "friendlyName=[LG] webOS TV");
		testUPNPDetails("LG WebOS TV", "DLNADeviceName.lge.com=LG-webOSTV");

		testUPNPDetails("Panasonic AS650", "modelNumber=TC-50AS650U");

		testUPNPDetails(
			"Panasonic DX",
			"friendlyName=55DX640_Series, address=192.168.1.7, udn=uuid:4D454930-0100-1000-8001-A81374A2AA5D, manufacturer=Panasonic, modelName=Panasonic VIErA, modelNumber=TH-55DX640Z"
		);

		testHeaders("Philips Aurea", "User-Agent: Allegro-Software-WebClient/4.61 DLNADOC/1.00");

		testHeaders("Philips PUS TV", "User-Agent: 49PUS8503/12");
		testUPNPDetails("Philips PUS TV", "friendlyName=49PUS8503/12");

		testHeaders("Philips 6500 Series TV", "User-Agent: 50PUS6523/12");
		testUPNPDetails("Philips 6500 Series TV", "friendlyName=50PUS6523/12");

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
			"Roku (DVP 10)",
			"User-Agent: Roku/DVP-10.5"
		);

		testHeaders(
			"Roku 3 (NSP 6-7)",
			"User-Agent: Roku/DVP-6.x",
			"User-Agent: Roku/DVP-7.x"
		);

		testHeaders("Roku TV", "User-Agent: Roku/5000X-7");

		testHeaders(
			"Roku TV (NSP 8)",
			"User-Agent: Roku/DVP-8.0 (308.00E04156A)",
			"User-Agent: Roku/DVP-8.1 (508.01E04018A)"
		);

		testHeaders(
			"Roku TV 4K",
			"User-Agent: Roku/6000X-7",
			"User-Agent: Roku/7000X-7",
			"User-Agent: Roku/8000X-7"
		);

		testUPNPDetails("Samsung 5300 Series", "modelName=UN32M5300");

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

		testUPNPDetails("Samsung EH5300", "modelName=UA32EH5300");

		testHeaders("Samsung ES8000", "User-Agent: SEC_HHP_[TV]UE46ES8000/1.0 DLNADOC/1.50");
		
		testHeaders("Samsung LED UHD", "USER-AGENT: DLNADOC/1.50 SEC_HHP_[TV] UE88KS9810/1.0 UPnP/1.0");
		testUPNPDetails("Samsung LED UHD", "modelName=UE88KS9810");

		testHeaders("Samsung SMT-G7400", "User-Agent: Linux/2.6.35 UPnP/1.0 NDS_MHF DLNADOC/1.50");

		testUPNPDetails("Samsung Soundbar", "friendlyName=[AV] Samsung Soundbar Q90R");
		testUPNPDetails("Samsung Soundbar", "modelDescription=Samsung SOUNDBAR DMR");

		testHeaders("Samsung Soundbar MS750", "USER-AGENT: DLNADOC/1.50 SEC_HHP_[AV] Samsung Soundbar MS750/1.0 UPnP/1.0");
		testUPNPDetails("Samsung Soundbar MS750", "modelName=HW-MS750");

		testHeaders("Samsung Q9 Series", "USER-AGENT: DLNADOC/1.50 SEC_HHP_[TV] Samsung Q9 Series (55)/1.0 UPnP/1.0");
		testUPNPDetails("Samsung Q9 Series", "modelName=QE55Q9FNA");

		testUPNPDetails(
			"Samsung QLED 4K 2019+",
			"modelName=QN49Q70RAFXZA"
		);
		testUPNPDetails(
			"Samsung QLED 4K 2019+",
			"modelName=QN75Q90RAFXZA"
		);
		testUPNPDetails(
			"Samsung QLED 4K 2019+",
			"modelName=UE43RU7179UXZG"
		);
		testUPNPDetails(
			"Samsung QLED 4K 2019+",
			"modelName=GQ43LS03TAUXZG"
		);
		testUPNPDetails(
			"Samsung QLED 4K 2019+",
			"modelName=QE43LS03TAUXXH"
		);
		testUPNPDetails(
			"Samsung QLED 4K 2019+",
			"modelName=QE55LS03RAUXXH"
		);
		testUPNPDetails(
			"Samsung QLED 4K 2019+",
			"modelName=QN32LS03TBFXZA"
		);

		testHeaders("Sharp Aquos", "User-Agent: DLNADOC/1.50 SHARP-AQUOS-DMP/1.1W");

		testHeaders("Showtime 3", "User-Agent: Showtime 3.0", "X-AV-Client-Info: Showtime 3.0");

		testHeaders("Showtime 4", "User-Agent: Showtime PS3 4.2");

		testHeaders("Sony Bluray UBP-X800M2", "X-AV-Client-Info: av=5.0; cn=\"Sony Corporation\"; mn=\"UBP-X800M2\"; mv=\"2.0\";");

		testHeaders("Sony Bravia EX", "X-AV-Client-Info: av=5.0; cn=\"Sony Corporation\"; mn=\"BRAVIA KDL-32CX520\"; mv=\"1.7\";");

		testHeaders("Sony Bravia HX", "X-AV-Client-Info: av=5.0; cn=\"Sony Corporation\"; mn=\"BRAVIA KDL-55HX750\"; mv=\"1.7\";");

		testHeaders(
			"Sony Bravia W",
			"X-AV-Physical-Unit-Info: pa=\"BRAVIA KDL-48W600B\";",
			"X-AV-Client-Info: av=5.0; cn=\"Sony Corporation\"; mn=\"BRAVIA KDL-48W600B\"; mv=\"1.7\";"
		);

		testHeaders(
			"Sony Bravia XBR",
			"User-Agent: UPnP/1.0, X-AV-Physical-Unit-Info: pa=\"BRAVIA XBR-55X900A\";, X-AV-Client-Info: av=5.0; cn=\"Sony Corporation\"; mn=\"BRAVIA XBR-55X900A\"; mv=\"1.7\";",
			"User-Agent: UPnP/1.0 DLNADOC/1.50, X-AV-Physical-Unit-Info: pa=\"BRAVIA XBR-55X900A\";, X-AV-Client-Info: av=5.0; cn=\"Sony Corporation\"; mn=\"BRAVIA XBR-55X900A\"; mv=\"1.7\";",
			"User-Agent: UPnP/1.0, X-AV-Physical-Unit-Info: pa=\"BRAVIA XBR-55HX929\";, X-AV-Client-Info: av=5.0; cn=\"Sony Corporation\"; mn=\"BRAVIA XBR-55HX929\"; mv=\"1.7\";",
			"User-Agent: UPnP/1.0 DLNADOC/1.50, X-AV-Physical-Unit-Info: pa=\"BRAVIA XBR-55HX929\";, X-AV-Client-Info: av=5.0; cn=\"Sony Corporation\"; mn=\"BRAVIA XBR-55HX929\"; mv=\"1.7\";"
		);

		testUPNPDetails(
			"Sony Bravia XBR OLED",
			"modelName=XBR-65A1E"
		);

		testHeaders("Sony X Series TV", "X-AV-Client-Info: av=5.0; cn=\"Sony Corporation\"; mn=\"BRAVIA KD-50X80J\"; mv=\"3.0\";");
		testUPNPDetails(
			"Sony X Series TV",
			"{friendlyName=Security TV, address=192.168.50.183, udn=uuid:96c90ee4-6768-460a-ad31-090018db9149, manufacturer=Sony Corporation, modelName=KD-50X80J, manufacturerURL=http://www.sony.net/}"
		);

		testHeaders("Sony Xperia Z/ZL/ZQ/Z1/Z2", "X-AV-Client-Info: C6603");

		// Note: This isn't the full user-agent, just a snippet to find it
		testHeaders("Telstra T-Box", "User-Agent: telstra");

		testHeaders("VideoWeb TV", "friendlyName.dlna.org: VideoWeb");

		testHeaders("VLC for desktop", "User-Agent: 6.2.9200 2/, UPnP/1.0, Portable SDK for UPnP devices/1.6.19");
		testHeaders("VLC for desktop", "User-Agent: Linux/3.13.0-68-generic, UPnP/1.0, Portable SDK for UPnP devices/1.6.6");
		testHeaders("VLC for desktop", "User-Agent: 6.1.7601 2/Service Pack 1, UPnP/1.0, Portable SDK for UPnP devices/1.6.19 for VLC 64-bit version 2.2.4");
		testHeaders("VLC for desktop", "User-Agent: UPnP/1.0, Portable SDK for UPnP devices/1.14.13on windows");
		testHeaders("VLC for desktop", "User-Agent: VLC/3.0.19 LibVLC/3.0.19");

		testHeaders("VLC for iOS", "User-Agent: VLC%20for%20iOS/447 CFNetwork/1399 Darwin/22.1.0");
		testHeaders("VLC for iOS", "User-Agent: Darwin/22.1.0, UPnP/1.0, Portable SDK for UPnP devices/1.14.13");

		testHeaders("WD TV Live", "User-Agent: INTEL_NMPR/2.1 DLNADOC/1.50 Intel MicroStack/1.0.1423");

		testHeaders("XBMC", "User-Agent: XBMC/10.0 r35648 (Mac OS X; 11.2.0 x86_64; http://www.xbmc.org)");
		testHeaders("XBMC", "User-Agent: Platinum/0.5.3.0, DLNADOC/1.50");

		testHeaders(
			"Xbox One",
			"FriendlyName.DLNA.ORG: Xbox-SystemOS",
			"FriendlyName.DLNA.ORG: XboxOne",
			"User-Agent: NSPlayer/12.00.9600.16411 WMFSDK/12.00.9600.16411"
		);

		testUPNPDetails(
			"Samsung 2021 QLED TV",
			"modelName=QE50QN90AATXXC"
		);
		testUPNPDetails(
			"Samsung 2021 QLED TV",
			"modelName=QE75Q80AATXXC"
		);
		testUPNPDetails(
			"Samsung 2021 AU9/Q6/43Q7/50Q7",
			"modelName=QE85Q60AAUXXC"
		);
		testUPNPDetails(
			"Samsung 2021 AU9/Q6/43Q7/50Q7",
			"modelName=UE75AU9005KXXC"
		);
		testUPNPDetails(
			"Samsung 2021 AU9/Q6/43Q7/50Q7",
			"modelName=QE50Q70AAUXXC"
		);
		testUPNPDetails(
			"Samsung 2021 AU8/AU7/BEA/32Q6",
			"modelName=UE75AU7105KXXC"
		);
		testUPNPDetails(
			"Samsung 2021 AU8/AU7/BEA/32Q6",
			"modelName=QN32Q60AAFXZA"
		);
		testUPNPDetails(
			"Samsung 2021 AU8/AU7/BEA/32Q6",
			"modelName=LH85BEAHLGUXEN"
		);
		testUPNPDetails(
			"Samsung 2021 Q5",
			"modelName=QN32Q50AAFXZC"
		);
		testUPNPDetails(
			"Samsung 2021 NEO QLED TV 8K",
			"modelName=QE65QN900ATXXC"
		);
		testUPNPDetails(
			"Samsung 2021 NEO QLED TV 8K",
			"modelName=QE85QN800ATXXC"
		);
	}

	/**
	 * Test recognition with a forced default renderer configured.
	 * @throws ConfigurationException
	 * @throws InterruptedException
	 */
	@Test
	public void testForcedDefault() throws ConfigurationException, InterruptedException {
		UmsConfiguration pmsConf = new UmsConfiguration(false);

		// Set default to PlayStation 3
		pmsConf.setRendererDefault("PlayStation 3");
		pmsConf.setRendererForceDefault(true);

		// Initialize the RendererConfiguration
		PMS.setConfiguration(pmsConf);
		RendererConfigurations.loadRendererConfigurations();

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
	 * @throws InterruptedException
	 */
	@Test
	public void testBogusDefault() throws ConfigurationException, InterruptedException {
		UmsConfiguration pmsConf = new UmsConfiguration(false);

		// Set default to non existent renderer
		pmsConf.setRendererDefault("Bogus Renderer");
		pmsConf.setRendererForceDefault(true);

		// Initialize the RendererConfiguration
		PMS.setConfiguration(pmsConf);
		RendererConfigurations.loadRendererConfigurations();

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
		RendererConfiguration rc = RendererConfigurations.getRendererConfigurationByHeaders(headers);
		if (correctRendererName != null) {
			// Headers are supposed to match a particular renderer
			assertNotNull(rc, "Recognized renderer for header \"" + headers + "\"");
			assertEquals(correctRendererName, rc.getRendererName(),
				"Expected renderer \"" + correctRendererName + "\", " +
				"instead renderer \"" + rc.getRendererName() + "\" was returned for header(s) \"" +
				headers + "\"");
		} else {
			// Headers are supposed to match no renderer at all
			assertEquals(null, rc,
				"Expected no matching renderer to be found for header(s) \"" + headers +
				"\", instead renderer \"" + (rc != null ? rc.getRendererName() : "") +
				"\" was recognized.");
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
		RendererConfiguration rc = RendererConfigurations.getRendererConfigurationByUPNPDetails(upnpDetails);
		if (correctRendererName != null) {
			// Headers are supposed to match a particular renderer
			assertNotNull(rc, "Recognized renderer for upnpDetails \"" + upnpDetails + "\"");
			assertEquals(correctRendererName, rc.getRendererName(),
				"Expected renderer \"" + correctRendererName + "\", " +
				"instead renderer \"" + rc.getRendererName() + "\" was returned for upnpDetails \"" +
				upnpDetails + "\"");
		} else {
			// Headers are supposed to match no renderer at all
			assertEquals(null, rc,
				"Expected no matching renderer to be found for upnpDetails \"" + upnpDetails +
				"\", instead renderer \"" + (rc != null ? rc.getRendererName() : "") +
				"\" was recognized.");
		}
	}
}
