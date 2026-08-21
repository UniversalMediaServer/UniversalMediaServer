package net.pms.network.mediaserver.jupnp.support.contentdirectory.updateobject;

import java.util.HashSet;
import java.util.Set;
import net.pms.PMS;
import net.pms.TestHelper;
import net.pms.configuration.UmsConfiguration;
import net.pms.store.StoreResource;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupnp.support.contentdirectory.ContentDirectoryException;
import org.w3c.dom.NodeList;

public class UpdateObjectTest {

	@BeforeEach
	public final void setUp() throws ConfigurationException, InterruptedException {
		TestHelper.setLoggingOff();
		PMS.get();
		PMS.setConfiguration(new UmsConfiguration(false));
	}

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
	public void testRatingIsAddedWhenUnrated() throws Exception {
		RatableResource resource = new RatableResource(null, false);
		handleRating(resource, "", "<upnp:rating>3</upnp:rating>");
		assertEquals(Integer.valueOf(3), resource.getRating());
	}

	@Test
	public void testRatingIsUpdated() throws Exception {
		RatableResource resource = new RatableResource(3, false);
		handleRating(resource, "<upnp:rating>3</upnp:rating>", "<upnp:rating>5</upnp:rating>");
		assertEquals(Integer.valueOf(5), resource.getRating());
	}

	/**
	 * An empty NewTagValue removes the rating.
	 */
	@Test
	public void testRatingIsCleared() throws Exception {
		RatableResource resource = new RatableResource(3, false);
		handleRating(resource, "<upnp:rating>3</upnp:rating>", "<upnp:rating />");
		assertNull(resource.getRating());
	}

	@Test
	public void testRatingOutOfBoundsIsRejected() {
		RatableResource resource = new RatableResource(3, false);
		ContentDirectoryException e = assertThrows(ContentDirectoryException.class,
			() -> handleRating(resource, "<upnp:rating>3</upnp:rating>", "<upnp:rating>7</upnp:rating>"));
		assertEquals(703, e.getErrorCode());
		assertEquals(Integer.valueOf(3), resource.getRating());
	}

	@Test
	public void testOutdatedCurrentValueIsRejected() {
		RatableResource resource = new RatableResource(3, false);
		ContentDirectoryException e = assertThrows(ContentDirectoryException.class,
			() -> handleRating(resource, "<upnp:rating>2</upnp:rating>", "<upnp:rating>5</upnp:rating>"));
		assertEquals(702, e.getErrorCode());
		assertEquals(Integer.valueOf(3), resource.getRating());
	}

	/**
	 * A NULL model rating used to be dereferenced, which surfaced as error 712.
	 */
	@Test
	public void testCurrentValueOnUnratedResourceIsRejected() {
		RatableResource resource = new RatableResource(null, false);
		ContentDirectoryException e = assertThrows(ContentDirectoryException.class,
			() -> handleRating(resource, "<upnp:rating>2</upnp:rating>", "<upnp:rating>5</upnp:rating>"));
		assertEquals(702, e.getErrorCode());
		assertNull(resource.getRating());
	}

	@Test
	public void testUnparsableValueIsRejected() {
		RatableResource resource = new RatableResource(3, false);
		ContentDirectoryException e = assertThrows(ContentDirectoryException.class,
			() -> handleRating(resource, "<upnp:rating>three</upnp:rating>", "<upnp:rating>5</upnp:rating>"));
		assertEquals(712, e.getErrorCode());
		assertEquals(Integer.valueOf(3), resource.getRating());
	}

	@Test
	public void testFolderCanBeRated() throws Exception {
		RatableResource folder = new RatableResource(null, true);
		handleRating(folder, "", "<upnp:rating>4</upnp:rating>");
		assertEquals(Integer.valueOf(4), folder.getRating());
	}

	private static void handleRating(StoreResource resource, String currentTagValue, String newTagValue) throws ContentDirectoryException {
		IUpdateObjectHandler handler = UpdateObjectFactory.getUpdateObjectHandler(resource, currentTagValue, newTagValue);
		assertNotNull(handler);
		handler.handle();
	}

	/**
	 * A minimal STORERESOURCE holding its rating in memory, so the
	 * handler can be tested without a database.
	 */
	private static class RatableResource extends StoreResource {

		private final boolean folder;
		private Integer rating;

		RatableResource(Integer rating, boolean folder) {
			super(null);
			this.rating = rating;
			this.folder = folder;
		}

		@Override
		public Integer getRating() {
			return rating;
		}

		@Override
		public void setRating(Integer rating) {
			if (rating != null && (rating < 0 || rating > 5)) {
				throw new IllegalArgumentException("rating must be between 0 and 5");
			}
			this.rating = rating;
		}

		@Override
		public String getName() {
			return "stub";
		}

		@Override
		public String getSystemName() {
			return "stub";
		}

		@Override
		public long length() {
			return 0;
		}

		@Override
		public boolean isFolder() {
			return folder;
		}

		@Override
		public boolean isValid() {
			return true;
		}
	}
}
