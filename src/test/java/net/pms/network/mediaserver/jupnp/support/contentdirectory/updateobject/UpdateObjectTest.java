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

	@Test
	public void testDataValue() {
		String tagValue = "data:image/jpeg;base64,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";

		String[] currentFragments = UpdateObjectFactory.getFragments(tagValue);
		assertEquals(1, currentFragments.length);
		NodeList n = UpdateObjectFactory.getXmlNode(currentFragments[0]);
		assertNull(n);
	}
}
