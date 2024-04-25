package net.pms.store.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class WebStreamMetadataCollectorTest {

	public WebStreamMetadataCollectorTest() {
	}

	@Test
	public void testGenreList() {
		List<String> tags = new ArrayList<>();

		tags.add("pop");
		tags.add("dance");
		tags.add("edm");

		String tagsString = WebStreamMetadataCollector.getInstance().getGenres(tags);
		assertEquals("pop / dance / edm", tagsString);
	}
}
