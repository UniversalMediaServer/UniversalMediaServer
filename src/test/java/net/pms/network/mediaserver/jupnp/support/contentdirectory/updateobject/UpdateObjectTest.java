package net.pms.network.mediaserver.jupnp.support.contentdirectory.updateobject;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.w3c.dom.NodeList;

public class UpdateObjectTest {

	@Test
	public void testSplitting() {
		String tagValue = "<a>value</a> ,<b>value</b>,,<d>value</d>, \n  <e>va\\,ue</e>,";

		String[] currentFragments = UpdateObjectFactory.getFragments(tagValue);
		assertEquals(6, currentFragments.length);
		assertEquals("<a>value</a>", currentFragments[0]);
		assertEquals("<b>value</b>", currentFragments[1]);
		assertTrue(currentFragments[2].isEmpty());
		assertEquals("<d>value</d>", currentFragments[3]);
		assertEquals("<e>va,ue</e>", currentFragments[4]);
		assertTrue(currentFragments[5].isEmpty());
	}

	@Test
	public void testUpnpRatingValue() {
		String tagValue = "<upnp:rating>4</upnp:rating>";

		String[] currentFragments = UpdateObjectFactory.getFragments(tagValue);
		NodeList n = UpdateObjectFactory.getXmlNode(currentFragments[0]);
		assertEquals("upnp:rating", n.item(0).getNodeName());
		assertEquals("4", n.item(0).getTextContent());
	}

	@Test
	public void testUpnpEmptyValue() {
		String tagValue = "<upnp:rating></upnp:rating>";

		String[] currentFragments = UpdateObjectFactory.getFragments(tagValue);
		NodeList n = UpdateObjectFactory.getXmlNode(currentFragments[0]);
		assertEquals("upnp:rating", n.item(0).getNodeName());
		assertTrue(StringUtils.isAllBlank(n.item(0).getTextContent()));
	}

	@Test
	public void testUpnpEmptyClosedElement() {
		String tagValue = "<upnp:rating />";

		String[] currentFragments = UpdateObjectFactory.getFragments(tagValue);
		NodeList n = UpdateObjectFactory.getXmlNode(currentFragments[0]);
		assertEquals("upnp:rating", n.item(0).getNodeName());
		assertTrue(StringUtils.isAllBlank(n.item(0).getTextContent()));
	}

	@Test
	public void testNullValue() {
		String tagValue = null;

		String[] currentFragments = UpdateObjectFactory.getFragments(tagValue);
		assertEquals(1, currentFragments.length);
		NodeList n = UpdateObjectFactory.getXmlNode(currentFragments[0]);
		assertNull(n);
	}

	@Test
	public void testEmptyValue() {
		String tagValue = "";

		String[] currentFragments = UpdateObjectFactory.getFragments(tagValue);
		assertEquals(1, currentFragments.length);
		NodeList n = UpdateObjectFactory.getXmlNode(currentFragments[0]);
		assertNull(n);
	}

	@Test
	public void testBlankValue() {
		String tagValue = "   ";

		String[] currentFragments = UpdateObjectFactory.getFragments(tagValue);
		assertEquals(1, currentFragments.length);
		NodeList n = UpdateObjectFactory.getXmlNode(currentFragments[0]);
		assertNull(n);
	}

	@Test
	public void testTwoValues() {
		String tagValue = "<upnp:genre>Swing</upnp:genre><upnp:genre>Jazz</upnp:genre>";

		String[] currentFragments = UpdateObjectFactory.getFragments(tagValue);
		assertEquals(1, currentFragments.length);

		NodeList n = UpdateObjectFactory.getXmlNode(currentFragments[0]);
		assertEquals(2, n.getLength());

		Set<String> textValues = new HashSet<>();
		textValues.add(n.item(0).getTextContent());
		textValues.add(n.item(1).getTextContent());
		assertTrue(textValues.contains("Jazz"));
		assertTrue(textValues.contains("Swing"));
	}

}
