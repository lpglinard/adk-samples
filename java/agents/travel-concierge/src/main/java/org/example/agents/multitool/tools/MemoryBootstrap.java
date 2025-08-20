package org.example.agents.multitool.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.example.agents.multitool.shared.Constants;
import org.example.agents.multitool.shared.SharedStateService;

/**
 * Bootstrap session state using the same logic/data as the Python project.
 *
 * Loads TRAVEL_CONCIERGE_SCENARIO env var if provided (filesystem path), else falls back to
 * the classpath resource: profiles/itinerary_empty_default.json. Then sets initial state keys.
 */
public class MemoryBootstrap {

    private static final Logger LOG = Logger.getLogger(MemoryBootstrap.class.getName());
    private static final String DEFAULT_RESOURCE = "profiles/itinerary_empty_default.json";

    private final SharedStateService stateService;
    private final ObjectMapper mapper = new ObjectMapper();

    public MemoryBootstrap(SharedStateService stateService) {
        this.stateService = stateService;
    }

    /**
     * Initialize per-session state once. Safe to call multiple times; guarded by _itin_initialized.
     */
    public void bootstrap(String sessionId) {
        Map<String, Object> state = stateService.getOrInit(sessionId);
        if (Boolean.TRUE.equals(state.get(Constants.ITIN_INITIALIZED))) {
            return;
        }

        Map<String, Object> sourceState = loadProfileState();
        setInitialStates(sourceState, state);
    }

    private Map<String, Object> loadProfileState() {
        String override = System.getenv("TRAVEL_CONCIERGE_SCENARIO");
        if (override != null && !override.isBlank()) {
            Path p = Path.of(override);
            if (Files.exists(p)) {
                try (InputStream in = Files.newInputStream(p)) {
                    JsonNode root = mapper.readTree(in);
                    return toMap(Objects.requireNonNull(root.get("state"), "state object missing"));
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Failed to load scenario from env path: " + override + ", falling back to default resource.", e);
                }
            } else {
                LOG.warning("TRAVEL_CONCIERGE_SCENARIO path does not exist: " + override + ". Falling back to default resource.");
            }
        }

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in == null) {
                LOG.warning("Default profile resource not found: " + DEFAULT_RESOURCE + ". Using empty state.");
                return new HashMap<>();
            }
            JsonNode root = mapper.readTree(in);
            return toMap(Objects.requireNonNull(root.get("state"), "state object missing in default profile"));
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to load default profile resource. Using empty state.", e);
            return new HashMap<>();
        }
    }

    private void setInitialStates(Map<String, Object> source, Map<String, Object> target) {
        // _time: only if not present
        target.putIfAbsent(Constants.SYSTEM_TIME, LocalDateTime.now().toString());

        if (!Boolean.TRUE.equals(target.get(Constants.ITIN_INITIALIZED))) {
            target.put(Constants.ITIN_INITIALIZED, true);
            // Merge source into target
            target.putAll(source);
            Object itinObj = source.get(Constants.ITIN_KEY);
            if (itinObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> itinerary = (Map<String, Object>) itinObj;
                Object sd = itinerary.get(Constants.START_DATE);
                Object ed = itinerary.get(Constants.END_DATE);
                if (sd != null) target.put(Constants.ITIN_START_DATE, sd);
                if (ed != null) target.put(Constants.ITIN_END_DATE, ed);
                if (sd != null) target.put(Constants.ITIN_DATETIME, sd);
            }
        }
    }

    private Map<String, Object> toMap(JsonNode node) {
        Map<String, Object> out = new HashMap<>();
        if (node == null || node.isNull()) return out;
        Iterator<String> fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
            String key = fieldNames.next();
            JsonNode v = node.get(key);
            Object val = mapper.convertValue(v, Object.class);
            out.put(key, val);
        }
        return out;
    }
}
