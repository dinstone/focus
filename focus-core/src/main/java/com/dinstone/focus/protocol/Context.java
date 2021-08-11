package com.dinstone.focus.protocol;

import java.util.concurrent.ConcurrentHashMap;

public class Context {

    private final ConcurrentHashMap<String, Object> contentMap = new ConcurrentHashMap<>();

    /**
     * put if absent
     * 
     * @param key
     * @param value
     * 
     * @return
     */
    public Object putIfAbsent(String key, Object value) {
        return this.contentMap.putIfAbsent(key, value);
    }

    /**
     * put
     * 
     * @param key
     * @param value
     * 
     * @return
     */
    public Object put(String key, Object value) {
        return this.contentMap.put(key, value);
    }

    /**
     * get
     * 
     * @param key
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) this.contentMap.get(key);
    }

    /**
     * remove
     * 
     * @param <T>
     * @param key
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T remove(String key) {
        return (T) this.contentMap.remove(key);
    }

    /**
     * get and use default if not found
     * 
     * @param key
     * @param defaultIfNotFound
     * @param <T>
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultIfNotFound) {
        return this.contentMap.get(key) != null ? (T) this.contentMap.get(key) : defaultIfNotFound;
    }

    /**
     * clear all mappings.
     */
    public void clear() {
        this.contentMap.clear();
    }

}
