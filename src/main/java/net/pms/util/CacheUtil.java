package net.pms.util;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a utility class for Cache operations
 * @param <T>
 * @param <U>
 */
public class CacheUtil<T, U> {
    private final Map<T, U> cache;

    public CacheUtil(){
        cache = new HashMap<>();
    }

    /**
     * sets an item of type U with a key of type T to the cache
     * @param key
     * @param value
     */
    public void setItem(T key, U value){
        cache.put(key, value);
    }

    /**
     * Returns item of type U from the cache or null if no item
     * with such key exists in the cache
     * @param key
     * @return value
     */
    public U getItem(T key){
        return cache.getOrDefault(key, null);
    }

    /**
     * Clears the cache
     */
    public void clearCache(){
        cache.clear();
    }
}
