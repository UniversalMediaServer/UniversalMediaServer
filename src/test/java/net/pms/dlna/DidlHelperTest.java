package net.pms.dlna;

import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;
import javax.xml.parsers.DocumentBuilderFactory;
import net.pms.network.mediaserver.HTTPXMLHelper;
import net.pms.renderers.Renderer;
import net.pms.store.StoreContainer;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;
import static org.junit.jupiter.api.Assertions.*;

class DidlHelperTest {
	private static String encode(String input) throws Exception {
		Method method = DidlHelper.class.getDeclaredMethod("encodeXML", String.class);
		method.setAccessible(true);
		return (String) method.invoke(null, input);
	}

	@Test
	void preservesEscapingForValidXmlText() throws Exception {
		Random random = new Random(16);
		String[] symbols = {"A", "ž", "&", "<", ">", "\"", "'", "\t", "\n", "\uD83C\uDFB5", "&amp;"};
		for (int round = 0; round < 1000; round++) {
			StringBuilder input = new StringBuilder();
			for (int i = 0; i < 80; i++) { input.append(symbols[random.nextInt(symbols.length)]); }
			String original = input.toString();
			assertEquals(original.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;"), encode(original));
		}
	}

	@Test
	void removesInvalidXmlCharactersButKeepsSupplementaryCharacters() throws Exception {
		String encoded = encode("Ž\u0000\u0001\uD800x\uDC00\uFFFE\uFFFF\uD83C\uDFB5 & < >");
		String xml = "<title>" + StringEscapeUtils.unescapeXml(encoded) + "</title>";
		assertEquals("Žx\uD83C\uDFB5 & < >", DocumentBuilderFactory.newInstance().newDocumentBuilder()
			.parse(new InputSource(new StringReader(xml))).getDocumentElement().getTextContent());
	}

	@Test
	void buildsParseableContainersWithMandatoryMetadataAndEscapedAttributes() throws Exception {
		System.setProperty(net.pms.PMS.PROPERTY_RUNNING_TESTS, "true");
		net.pms.PMS.setConfiguration(new net.pms.configuration.UmsConfiguration(false));
		Renderer renderer = new Renderer((String) null) {
			@Override public boolean isSendFolderThumbnails() { return false; }
		};
		StoreContainer parent = new StoreContainer(renderer, "Root", null);
		parent.setId("0");
		StoreContainer container = new StoreContainer(renderer, "Hudba <&> \uD83C\uDFB5", null);
		container.setId("id\"<&");
		container.setParent(parent);
		String expected = StringEscapeUtils.unescapeXml(HTTPXMLHelper.DIDL_HEADER + DidlHelper.getDidlString(container) + DidlHelper.getDidlString(container) + HTTPXMLHelper.DIDL_FOOTER);
		String actual = DidlHelper.getDidlResults(List.of(container, container));
		assertEquals(expected, actual);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		var document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(actual)));
		var elements = document.getElementsByTagNameNS("urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/", "container");
		assertEquals(2, elements.getLength());
		var element = (org.w3c.dom.Element) elements.item(0);
		assertEquals("id\"<&", element.getAttribute("id"));
		assertEquals("0", element.getAttribute("parentID"));
		assertEquals("1", element.getAttribute("restricted"));
		assertEquals("Hudba <&> \uD83C\uDFB5", element.getElementsByTagNameNS("http://purl.org/dc/elements/1.1/", "title").item(0).getTextContent());
		assertEquals(1, element.getElementsByTagNameNS("urn:schemas-upnp-org:metadata-1-0/upnp/", "class").getLength());
	}
	@Test
	void audioResourceUsesBytesPerSecondAndHasProtocolInfo() throws Exception {
		System.setProperty(net.pms.PMS.PROPERTY_RUNNING_TESTS, "true");
		net.pms.PMS.setConfiguration(new net.pms.configuration.UmsConfiguration(false));
		Renderer renderer = new Renderer((String) null);
		var item = new net.pms.store.item.RealFile(renderer, new java.io.File("example.mp3")) {
			@Override public String getMediaURL() { return "http://localhost/media/example.mp3"; }
			@Override public net.pms.image.ImageInfo getThumbnailImageInfo() { return null; }
		};
		item.setId("123");
		item.setFormat(new net.pms.formats.audio.MP3());
		var media = new net.pms.media.MediaInfo() {
			@Override public boolean isMediaParsed() { return true; }
		};
		media.setBitRate(320000);
		media.addAudioTrack(new net.pms.media.audio.MediaAudio());
		item.setMediaInfo(media);
		String xml = DidlHelper.getDidlResults(List.of(item));
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		var document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
		var res = (org.w3c.dom.Element) document.getElementsByTagNameNS("urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/", "res").item(0);
		assertEquals("40000", res.getAttribute("bitrate"));
		assertTrue(res.getAttribute("protocolInfo").startsWith("http-get:*:"));
	}

}
