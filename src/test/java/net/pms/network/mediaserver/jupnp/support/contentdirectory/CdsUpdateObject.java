package net.pms.network.mediaserver.jupnp.support.contentdirectory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.w3c.dom.NodeList;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.updateobject.UpdateObjectFactory;

public class CdsUpdateObject {

	public CdsUpdateObject() {
	}

	@Test
	public void testSplitting() {
		UmsContentDirectoryService cds = new UmsContentDirectoryService();
		String myXml = "<a>value</a> ,<b>value</b>,,<d>value</d>, \n  <e>va\\,ue</e>,";

		String[] currentFragments = cds.splitCsvList(myXml);
		int i = 0;
		for (String string : currentFragments) {
			assertEquals("<a>value</a>", StringUtils.trim(currentFragments[0]));
			assertEquals("<b>value</b>", StringUtils.trim(currentFragments[1]));
			assertTrue(StringUtils.isAllBlank(currentFragments[2]));
			assertEquals("<d>value</d>", StringUtils.trim(currentFragments[3]));
			assertEquals("<e>va,ue</e>", StringUtils.trim(currentFragments[4]));
			assertTrue(StringUtils.isAllBlank(currentFragments[5]));
		}

	}

	@Test
	public void testUpnpRatingValue() {
		UmsContentDirectoryService cds = new UmsContentDirectoryService();
		String myXml = "<upnp:rating>4</upnp:rating>";

		String[] currentFragments = cds.splitCsvList(myXml);
		NodeList n = UpdateObjectFactory.getXmlNode(currentFragments[0]);
		assertEquals("upnp:rating", n.item(0).getNodeName());
		assertEquals("4", n.item(0).getTextContent());
	}

	@Test
	public void testUpnpEmptyValue() {
		UmsContentDirectoryService cds = new UmsContentDirectoryService();
		String myXml = "<upnp:rating></upnp:rating>";

		String[] currentFragments = cds.splitCsvList(myXml);
		NodeList n = UpdateObjectFactory.getXmlNode(currentFragments[0]);
		assertEquals("upnp:rating", n.item(0).getNodeName());
		assertTrue(StringUtils.isAllBlank(n.item(0).getTextContent()));
	}

	@Test
	public void testEmptyValue() {
		UmsContentDirectoryService cds = new UmsContentDirectoryService();
		String myXml = "";

		String[] currentFragments = cds.splitCsvList(myXml);
		assertEquals(currentFragments.length, 0);
		NodeList n = UpdateObjectFactory.getXmlNode("");
		assertNull(n);
		n = UpdateObjectFactory.getXmlNode(null);
		assertNull(n);
	}

	@Test
	public void testTwoValues() {
		UmsContentDirectoryService cds = new UmsContentDirectoryService();
		String myXml = "<upnp:genre>Swing</upnp:genre><upnp:genre>Jazz</upnp:genre>";

		String[] currentFragments = cds.splitCsvList(myXml);
		assertEquals(currentFragments.length, 1);

		NodeList n = UpdateObjectFactory.getXmlNode(currentFragments[0]);
		assertEquals(n.getLength(), 2);

		Set<String> textValues = new HashSet<>();
		textValues.add(n.item(0).getTextContent());
		textValues.add(n.item(1).getTextContent());
		assertTrue(textValues.contains("Jazz"));
		assertTrue(textValues.contains("Swing"));
	}
}
