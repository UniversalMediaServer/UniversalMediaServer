package net.pms.util;

import org.junit.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CacheUtilTest {
    @Test
    public void testSetsAndGetsItem(){
        CacheUtil<String, String> cacheUtil = new CacheUtil<>();
        cacheUtil.setItem("key", "value");
        assertEquals("value", cacheUtil.getItem("key"));
    }

    @Test
    public void testReturnsNullForAbsentObject(){
        CacheUtil<String, String> cacheUtil = new CacheUtil<>();
        assertNull(cacheUtil.getItem("absent"));
    }

    @Test
    public void testClearCache(){
        CacheUtil<String, String> cacheUtil = new CacheUtil<>();
        cacheUtil.setItem("key", "value");
        cacheUtil.clearCache();
        assertNull(cacheUtil.getItem("key"));
    }

    @Test
    public void testCompatibleWithDifferentTypes(){
        CacheUtil<String, Integer> cacheUtil = new CacheUtil<>();
        cacheUtil.setItem("first", 1);
        cacheUtil.setItem("second", 2);
        assertEquals(2, cacheUtil.getItem("second"));
        assertEquals(1, cacheUtil.getItem("first"));
        assertNull(cacheUtil.getItem("third"));
    }
}