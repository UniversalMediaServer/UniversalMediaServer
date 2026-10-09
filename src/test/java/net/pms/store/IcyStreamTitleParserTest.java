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
package net.pms.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import net.pms.store.IcyStreamTitleParser.Order;
import org.junit.jupiter.api.Test;

class IcyStreamTitleParserTest {

	private static final String STATION = "Classic Vinyl HD";
	private static final String URL = "https://icy.walmradio.com:8443/classic";

	private static final String ARTIST_FIRST_LINE = "Roger Whittaker - She Believes In Me";
	private static final String STATION_SUFFIX_LINE = "She Believes In Me by Roger Whittaker - Classic Vinyl on walmradio.com";

	private static IcyStreamTitleParser parser(Order order) {
		return new IcyStreamTitleParser(() -> order, STATION, URL);
	}

	@Test
	void artistFirstIsTheAssumption() {
		NowPlayingInfo info = parser(Order.AUTO).parse(ARTIST_FIRST_LINE);
		assertEquals("Roger Whittaker", info.artist);
		assertEquals("She Believes In Me", info.title);
		assertEquals(ARTIST_FIRST_LINE, info.streamTitle);
	}

	@Test
	void aTailNamingTheStationIsDetected() {
		NowPlayingInfo info = parser(Order.AUTO).parse(STATION_SUFFIX_LINE);
		assertEquals("She Believes In Me by Roger Whittaker", info.title);
		assertEquals("Classic Vinyl on walmradio.com", info.artist);
		assertEquals(STATION_SUFFIX_LINE, info.streamTitle);
	}

	@Test
	void aTailWithoutADomainIsDetectedByTheStationName() {
		NowPlayingInfo info = new IcyStreamTitleParser(() -> Order.AUTO, STATION, URL)
			.parse("She Believes In Me by Roger Whittaker - Classic Vinyl");
		assertEquals("She Believes In Me by Roger Whittaker", info.title);
		assertEquals("Classic Vinyl", info.artist);
	}

	@Test
	void theHostNamesTheStationAsWell() {
		NowPlayingInfo info = new IcyStreamTitleParser(() -> Order.AUTO, "Some Playlist Entry", URL)
			.parse("She Believes In Me - walmradio");
		assertEquals("She Believes In Me", info.title);
		assertEquals("walmradio", info.artist);
	}

	@Test
	void theDirectiveBeatsTheDetection() {
		NowPlayingInfo info = parser(Order.ARTIST_FIRST).parse(STATION_SUFFIX_LINE);
		assertEquals("She Believes In Me by Roger Whittaker", info.artist);
		assertEquals("Classic Vinyl on walmradio.com", info.title);
		assertEquals(STATION_SUFFIX_LINE, info.streamTitle);
	}

	@Test
	void artistFirstSplitsAtTheFirstSeparator() {
		NowPlayingInfo info = parser(Order.ARTIST_FIRST).parse("A - B - C");
		assertEquals("A", info.artist);
		assertEquals("B - C", info.title);
	}

	@Test
	void titleFirstSplitsAtTheLastSeparator() {
		NowPlayingInfo info = parser(Order.TITLE_FIRST).parse("A - B - C");
		assertEquals("A - B", info.title);
		assertEquals("C", info.artist);
	}

	@Test
	void aLineWithoutASeparatorStaysWhole() {
		NowPlayingInfo info = parser(Order.AUTO).parse("Station identification");
		assertNull(info.artist);
		assertNull(info.title);
		assertEquals("Station identification", info.streamTitle);
	}

	@Test
	void aDotInsideTheTailIsNoDomain() {
		NowPlayingInfo info = parser(Order.AUTO).parse("Roger Whittaker - She Believes In Me feat. Someone");
		assertEquals("Roger Whittaker", info.artist);
		assertEquals("She Believes In Me feat. Someone", info.title);
	}

	@Test
	void anEnDashSeparatesAsWell() {
		NowPlayingInfo info = parser(Order.AUTO).parse("Roger Whittaker – She Believes In Me");
		assertEquals("Roger Whittaker", info.artist);
		assertEquals("She Believes In Me", info.title);
	}

	@Test
	void aHyphenatedNameIsNoSeparator() {
		NowPlayingInfo info = parser(Order.AUTO).parse("Jean-Michel Jarre - Oxygene");
		assertEquals("Jean-Michel Jarre", info.artist);
		assertEquals("Oxygene", info.title);
	}

	@Test
	void anEmptyHalfKeepsTheLineWhole() {
		NowPlayingInfo info = parser(Order.AUTO).parse("- She Believes In Me");
		assertNull(info.artist);
		assertEquals("- She Believes In Me", info.streamTitle);
	}

	@Test
	void aBlankLineIsUnknown() {
		assertNull(parser(Order.AUTO).parse(" "));
		assertNull(parser(Order.AUTO).parse(null));
	}

	@Test
	void theSameLineIsNotSplitTwice() {
		IcyStreamTitleParser parser = parser(Order.AUTO);
		assertSame(parser.parse(ARTIST_FIRST_LINE), parser.parse(ARTIST_FIRST_LINE));
	}

	@Test
	void switchingTheOrderTakesEffectOnTheSameLine() {
		Order[] order = {Order.AUTO};
		IcyStreamTitleParser parser = new IcyStreamTitleParser(() -> order[0], STATION, URL);
		assertEquals("She Believes In Me by Roger Whittaker", parser.parse(STATION_SUFFIX_LINE).title);
		order[0] = Order.ARTIST_FIRST;
		assertEquals("She Believes In Me by Roger Whittaker", parser.parse(STATION_SUFFIX_LINE).artist);
	}

	@Test
	void unknownDirectiveValuesFallBackToAuto() {
		assertEquals(Order.AUTO, Order.of(null));
		assertEquals(Order.AUTO, Order.of(""));
		assertEquals(Order.AUTO, Order.of("nonsense"));
		assertEquals(Order.ARTIST_FIRST, Order.of("artist-first"));
		assertEquals(Order.ARTIST_FIRST, Order.of(" ARTIST-FIRST "));
		assertEquals(Order.TITLE_FIRST, Order.of("title-first"));
		assertEquals("title-first", Order.TITLE_FIRST.directiveValue());
		assertEquals("auto", Order.AUTO.directiveValue());
	}
}
