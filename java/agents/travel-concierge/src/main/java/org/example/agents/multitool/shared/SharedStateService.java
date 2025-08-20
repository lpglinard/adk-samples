package org.example.agents.multitool.shared;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple in-memory, session-scoped key-value store.
 *
 * This service keeps a per-session Map<String, Object> that can be read/written by tools and agents.
 * For production, back this with a shared data store (e.g., Redis) and keep using the same API.
 */
public class SharedStateService {

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> store = new ConcurrentHashMap<>();

    public Map<String, Object> getOrInit(String sessionId) {
        return store.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>());
    }

    public void put(String sessionId, String key, Object value) {
        getOrInit(sessionId).put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String sessionId, String key, Class<T> type) {
        Object v = getOrInit(sessionId).get(key);
        if (v == null) return null;
        return (T) v; // caller responsibility to request correct type
    }

    public void clear(String sessionId) {
        store.remove(sessionId);
    }
}
