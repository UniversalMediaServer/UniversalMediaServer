package net.pms.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


public class PairTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void testPair() {
		// Test constructor, getters
		Pair<String, Double> pair = new Pair<>("Pair", Double.valueOf(99.9));
		Pair<String, Double> pair2 = new Pair<>("Another pair", Double.valueOf(99.9));
		Pair<String, Double> pair3 = new Pair<>("Pair", Double.valueOf(10d));

		assertEquals("Pair", pair.first);
		assertEquals("Pair", pair.getFirst());
		assertEquals("Pair", pair.getKey());
		assertEquals("Pair", pair.getLeft());
		assertEquals(Double.valueOf(99.9), pair.second);
		assertEquals(Double.valueOf(99.9), pair.getSecond());
		assertEquals(Double.valueOf(99.9), pair.getValue());
		assertEquals(Double.valueOf(99.9), pair.getRight());
		assertEquals(Double.valueOf(99.9), pair.getLast());
		assertEquals("Another pair", pair2.first);
		assertEquals("Another pair", pair2.getFirst());
		assertEquals("Another pair", pair2.getKey());
		assertEquals("Another pair", pair2.getLeft());
		assertEquals(Double.valueOf(99.9), pair2.second);
		assertEquals(Double.valueOf(99.9), pair2.getSecond());
		assertEquals(Double.valueOf(99.9), pair2.getValue());
		assertEquals(Double.valueOf(99.9), pair2.getRight());
		assertEquals(Double.valueOf(99.9), pair2.getLast());
		assertEquals("Pair", pair3.first);
		assertEquals("Pair", pair3.getFirst());
		assertEquals("Pair", pair3.getKey());
		assertEquals("Pair", pair3.getLeft());
		assertEquals(Double.valueOf(10.0), pair3.second);
		assertEquals(Double.valueOf(10.0), pair3.getSecond());
		assertEquals(Double.valueOf(10.0), pair3.getValue());
		assertEquals(Double.valueOf(10.0), pair3.getRight());
		assertEquals(Double.valueOf(10.0), pair3.getLast());

		// Test equals
		assertFalse(pair.equals(pair2));
		assertFalse(pair.equals(pair3));
		assertFalse(pair2.equals(pair));
		assertFalse(pair2.equals(pair3));
		assertFalse(pair3.equals(pair));
		assertFalse(pair3.equals(pair2));
		assertTrue(pair.equals(pair));
		assertTrue(pair2.equals(pair2));
		assertTrue(pair3.equals(pair3));
		assertTrue(pair.equals(new Pair<String, Double>("Pair", Double.valueOf(99.9))));
		assertFalse(pair.equals(new Pair<String, Float>("Pair", Float.valueOf(99.9f))));

		// Test Map.Entry interface
		Map<String, Double> map = new HashMap<>();
		map.put("Pair", Double.valueOf(99.9));
		for (Entry<String, Double> entry : map.entrySet()) {
			assertTrue(pair.equals(entry));
			assertTrue(entry.equals(pair));
		}

		// Test Comparable interface
		List<Pair<String, Double>> list = new ArrayList<>();
		list.add(pair);
		list.add(pair2);
		list.add(pair3);
		Collections.sort(list);
		assertEquals(pair2, list.get(0));
		assertEquals(pair3, list.get(1));
		assertEquals(pair, list.get(2));

		// Test toString()
		assertEquals("Pair[\"Pair\", 99.9]", pair.toString());
		assertEquals("Pair[\"Another pair\", 99.9]", pair2.toString());
		assertEquals("Pair[\"Pair\", 10.0]", pair3.toString());
		assertEquals("Pair[null, 10.0]", new Pair<String, Double>(null, Double.valueOf(10d)).toString());
		assertEquals("Pair[\"Pair\", null]", new Pair<String, Double>("Pair", null).toString());
		assertEquals("Pair[null, null]", new Pair<String, Double>(null, null).toString());
	}

	@Test
	public void testImmutable() {
		exception.expect(UnsupportedOperationException.class);
		exception.expectMessage("Pair is immutable");
		Pair<String, Double> pair = new Pair<>("Pair", Double.valueOf(99.9));
		pair.setValue(Double.valueOf(20d));
	}
}
