package org.example.agents.multitool.tools;

import com.google.adk.tools.Annotations.Schema;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.example.agents.multitool.shared.SharedStateService;

/**
 * Memory tools to read/write session-scoped state.
 * These mirror the Python tools (memorize, memorize_list, forget) but accept sessionId explicitly.
 */
public class MemoryTools {

    private static final Logger LOG = Logger.getLogger(MemoryTools.class.getName());

    private final SharedStateService stateService;

    public MemoryTools(SharedStateService stateService) {
        this.stateService = stateService;
    }

    public Map<String, Object> memorize(
            @Schema(name = "sessionId", description = "Session id to scope the memory") String sessionId,
            @Schema(name = "key", description = "Label indexing the memory") String key,
            @Schema(name = "value", description = "Value to store") String value) {
        var state = stateService.getOrInit(sessionId);
        state.put(key, value);
        return Map.of("status", String.format("Stored \"%s\": \"%s\"", key, value));
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> memorizeList(
            @Schema(name = "sessionId", description = "Session id to scope the memory") String sessionId,
            @Schema(name = "key", description = "Label indexing the list") String key,
            @Schema(name = "value", description = "Value to append if not present") String value) {
        var state = stateService.getOrInit(sessionId);
        Object existing = state.get(key);
        List<String> list;
        if (existing instanceof List) {
            list = (List<String>) existing;
        } else if (existing == null) {
            list = new ArrayList<>();
            state.put(key, list);
        } else {
            // Replace non-list with a new list to keep it simple for the sample
            list = new ArrayList<>();
            state.put(key, list);
            LOG.fine(() -> "Overwriting non-list state for key=" + key + " with a new list");
        }
        if (!list.contains(value)) {
            list.add(value);
        }
        return Map.of("status", String.format("Stored \"%s\": \"%s\"", key, value));
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> forget(
            @Schema(name = "sessionId", description = "Session id to scope the memory") String sessionId,
            @Schema(name = "key", description = "Label indexing the list") String key,
            @Schema(name = "value", description = "Value to remove if present") String value) {
        var state = stateService.getOrInit(sessionId);
        Object existing = state.get(key);
        if (existing instanceof List) {
            List<String> list = (List<String>) existing;
            list.remove(value);
        }
        return Map.of("status", String.format("Removed \"%s\": \"%s\"", key, value));
    }
}
