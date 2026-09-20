package net.pms.encoders;

import java.io.File;
import java.util.List;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.store.StoreItem;
import net.pms.store.item.RealFile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TranscodingSettingsSelectionTest {
	@BeforeAll
	public static void configure() throws Exception {
		System.setProperty(PMS.PROPERTY_RUNNING_TESTS, "true");
		PMS.setConfiguration(new UmsConfiguration(false));
	}

	private static final class CountingEngine extends FFMpegVideo {
		private final List<EncodingFormat> formats;
		private boolean accepts;
		private int itemChecks;
		CountingEngine(boolean accepts, List<EncodingFormat> formats) {
			this.accepts = accepts;
			this.formats = formats;
		}
		@Override
		public boolean isCompatible(EncodingFormat format) { return formats.contains(format); }
		@Override
		public boolean isCompatible(StoreItem item) { itemChecks++; return accepts; }
	}

	private static List<EncodingFormat> formats() {
		return List.of(EncodingFormat.VIDEO.get("MP4-H264-AAC"), EncodingFormat.VIDEO.get("MPEGTS-H264-AAC"), EncodingFormat.VIDEO.get("WMV"));
	}

	@Test
	public void enumeratesSamePairsInPriorityOrderWithOneSourceCheckPerEngine() {
		var formats = formats();
		var first = new CountingEngine(true, formats);
		var rejected = new CountingEngine(false, formats);
		var second = new CountingEngine(true, formats.subList(1, 3));
		var unused = new CountingEngine(true, List.of());
		var item = new RealFile(null, new File("video.mkv"));
		var result = TranscodingSettings.getTranscodingsSettings(item, formats, List.of(first, rejected, second, unused));
		assertEquals(5, result.size());
		int pos = 0;
		for (int i = 0; i < formats.size(); i++) {
			assertSame(first, result.get(pos).getEngine());
			assertSame(formats.get(i), result.get(pos++).getEncodingFormat());
			if (i > 0) {
				assertSame(second, result.get(pos).getEngine());
				assertSame(formats.get(i), result.get(pos++).getEncodingFormat());
			}
		}
		assertEquals(1, first.itemChecks);
		assertEquals(1, rejected.itemChecks);
		assertEquals(1, second.itemChecks);
		assertEquals(0, unused.itemChecks);
	}

	@Test
	public void bestChoicePreservesFormatPriorityAndRechecksNextCall() {
		var formats = formats();
		var rejected = new CountingEngine(false, formats);
		var laterFormat = new CountingEngine(true, formats.subList(2, 3));
		var earlierFormat = new CountingEngine(true, formats.subList(1, 3));
		var engines = List.<Engine>of(rejected, laterFormat, earlierFormat);
		var item = new RealFile(null, new File("video.mkv"));
		var best = TranscodingSettings.getBestTranscodingSettings(item, formats, engines);
		assertSame(earlierFormat, best.getEngine());
		assertSame(formats.get(1), best.getEncodingFormat());
		assertEquals(1, rejected.itemChecks);
		assertEquals(0, laterFormat.itemChecks);
		rejected.accepts = true;
		assertSame(rejected, TranscodingSettings.getBestTranscodingSettings(item, formats, engines).getEngine());
		assertEquals(2, rejected.itemChecks);
	}

	@Test
	public void incompatibleItemReturnsNoSettings() {
		var formats = formats();
		var rejected = new CountingEngine(false, formats);
		var item = new RealFile(null, new File("video.mkv"));
		assertNull(TranscodingSettings.getBestTranscodingSettings(item, formats, List.of(rejected)));
		assertEquals(1, rejected.itemChecks);
		assertTrue(TranscodingSettings.getTranscodingsSettings(item, formats, List.of(rejected)).isEmpty());
		assertEquals(2, rejected.itemChecks);
	}
}
