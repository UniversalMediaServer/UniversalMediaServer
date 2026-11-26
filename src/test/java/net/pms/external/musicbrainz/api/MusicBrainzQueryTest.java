package net.pms.external.musicbrainz.api;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag;
import org.junit.jupiter.api.Test;

public class MusicBrainzQueryTest {
	@org.junit.jupiter.api.BeforeAll
	public static void configure() throws Exception {
		System.setProperty(net.pms.PMS.PROPERTY_RUNNING_TESTS, "true");
		net.pms.PMS.setConfiguration(new net.pms.configuration.UmsConfiguration(false));
	}

	@Test
	public void emptyAndArtistOnlyReleaseQueriesAreSkipped() throws Exception {
		var tag = new VorbisCommentTag();
		for (boolean fuzzy : new boolean[]{false, true}) {
			assertNull(MusicBrainzUtil.buildMBReleaseQuery(new MusicBrainzTagInfo(tag), fuzzy));
			assertNull(MusicBrainzUtil.buildMBRecordingQuery(new MusicBrainzTagInfo(tag), fuzzy));
		}
		tag.setField(FieldKey.ARTIST, "ABBA");
		for (boolean fuzzy : new boolean[]{false, true}) {
			assertNull(MusicBrainzUtil.buildMBReleaseQuery(new MusicBrainzTagInfo(tag), fuzzy));
			assertTrue(decode(MusicBrainzUtil.buildMBRecordingQuery(new MusicBrainzTagInfo(tag), fuzzy)).contains("artistname:"));
		}
	}

	@Test
	public void artistAndYearAreSeparated() throws Exception {
		var tag = new VorbisCommentTag();
		tag.setField(FieldKey.ARTIST, "ABBA");
		tag.setField(FieldKey.YEAR, "1980");
		assertEquals("recording/?query=artistname:\"ABBA\" AND date:1980*", decode(MusicBrainzUtil.buildMBRecordingQuery(new MusicBrainzTagInfo(tag), false)));
	}

	@Test
	public void albumTrackIdAndYearRemainSearchable() throws Exception {
		var tag = new VorbisCommentTag();
		tag.setField(FieldKey.ALBUM, "The Album");
		assertEquals("release/?query=\"The Album\"", decode(MusicBrainzUtil.buildMBReleaseQuery(new MusicBrainzTagInfo(tag), false)));
		tag = new VorbisCommentTag();
		tag.setField(FieldKey.MUSICBRAINZ_TRACK_ID, "test-id");
		assertEquals("release/?query=tid:test-id", decode(MusicBrainzUtil.buildMBReleaseQuery(new MusicBrainzTagInfo(tag), false)));
		assertEquals("recording/?query=tid:test-id", decode(MusicBrainzUtil.buildMBRecordingQuery(new MusicBrainzTagInfo(tag), false)));
		tag = new VorbisCommentTag();
		tag.setField(FieldKey.YEAR, "1980");
		assertEquals("release/?query=date:1980*", decode(MusicBrainzUtil.buildMBReleaseQuery(new MusicBrainzTagInfo(tag), false)));
		assertNull(MusicBrainzUtil.buildMBReleaseQuery(new MusicBrainzTagInfo(tag), true));
		assertNull(MusicBrainzUtil.buildMBRecordingQuery(new MusicBrainzTagInfo(tag), true));
	}

	private static String decode(String query) {
		return URLDecoder.decode(query, StandardCharsets.UTF_8);
	}
}
