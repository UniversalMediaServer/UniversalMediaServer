package net.pms.util;

import net.pms.util.FileWatcher.Listener;
import net.pms.util.FileWatcher.Watch;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class FileWatcherTest {

	private static final Listener NOOP = (String filename, String event, Watch watch, boolean isDir) -> {
		// no-op listener for testing purposes
	};

	@Test
	public void testWatchWithoutItemEqualsItself() {
		Watch watch = new Watch("/media/**", NOOP);
		assertEquals(watch, watch);
		assertEquals(watch.hashCode(), watch.hashCode());
	}

	@Test
	public void testWatchesWithSameSpecAndListenerAreEqual() {
		Watch first = new Watch("/media/**", NOOP);
		Watch second = new Watch("/media/**", NOOP);
		assertEquals(first, second);
		assertEquals(first.hashCode(), second.hashCode(), "equal watches must share a hash code");
	}

	@Test
	public void testWatchesDifferInFileSpecOrListener() {
		Watch media = new Watch("/media/**", NOOP);
		assertNotEquals(media, new Watch("/other/**", NOOP));
		assertNotEquals(media, new Watch("/media/**", (String f, String e, Watch w, boolean d) -> {
			//a different listener is a different watchpoint
		}));
	}

	/**
	 * An item makes the watch specific to that object, so two watches carrying different items must stay apart even when spec and listener match.
	 */
	@Test
	public void testWatchesWithDifferentItemsDiffer() {
		Object first = new Object();
		Object second = new Object();
		assertNotEquals(new Watch("/media/**", NOOP, first), new Watch("/media/**", NOOP, second));
		assertEquals(new Watch("/media/**", NOOP, first), new Watch("/media/**", NOOP, first));
	}

	/**
	 * A watch with an item and one without are not the same watchpoint.
	 */
	@Test
	public void testItemAndItemlessWatchDiffer() {
		assertNotEquals(new Watch("/media/**", NOOP), new Watch("/media/**", NOOP, new Object()));
	}

	/**
	 * We rely on equals to drop duplicates, which stops event multiplication.
	 */
	@Test
	public void testEqualWatchIsRecognizedInAList() {
		java.util.ArrayList<Watch> watches = new java.util.ArrayList<>();
		watches.add(new Watch("/media/**", NOOP));
		assertTrue(watches.contains(new Watch("/media/**", NOOP)), "a duplicate watch has to be recognized");
		assertTrue(watches.remove(new Watch("/media/**", NOOP)), "a watch has to be removable again");
		assertEquals(0, watches.size());
	}
}
