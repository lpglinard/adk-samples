package org.example.agents.multitool;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.adk.tools.Annotations.Schema;
import com.google.adk.tools.FunctionTool;
import com.google.genai.types.Content;
import com.google.genai.types.FunctionResponse;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.example.agents.multitool.shared.SharedStateService;
import org.example.agents.multitool.tools.MemoryBootstrap;
import org.example.agents.multitool.tools.MemoryTools;

public class TravelConciergeAgent {

    public static final String DESCRIPTION = "Agent to answer questions about the time and weather in a city.";
    public static final String INSTRUCTION = loadInstruction();
    public static final String MODEL = "gemini-2.5-flash";

    private static final Logger ADK_LOGGER = Logger.getLogger(TravelConciergeAgent.class.getName());

    private static String USER_ID = "student";
    private static String NAME = "multi_tool_agent"; // Keeping name stable for Dev UI; rename if desired

    private static String loadInstruction() {
        String fallback = "You are a helpful agent who can answer user questions about the time and weather in a city.";
        String resourcePath = "prompts/travel_concierge_agent.txt";
        try (var in = TravelConciergeAgent.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                ADK_LOGGER.warning("Instruction prompt resource not found at: " + resourcePath + ". Using fallback instruction.");
                return fallback;
            }
            String instruction = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
            ADK_LOGGER.fine("Loaded instruction from resource. Length=" + instruction.length());
            return instruction;
        } catch (IOException e) {
            ADK_LOGGER.log(Level.WARNING, "Failed to load instruction prompt. Using fallback.", e);
            return fallback;
        }
    }

    // To run your agent with Dev UI, the ROOT_AGENT should be a global public static final variable.
    public static final BaseAgent ROOT_AGENT = initAgent();

    // Shared memory service and tools (instance-based) so we can share state across tools/agents
    private static final SharedStateService SHARED_STATE = new SharedStateService();
    private static final MemoryTools MEMORY_TOOLS = new MemoryTools(SHARED_STATE);
    private static final MemoryBootstrap MEMORY_BOOTSTRAP = new MemoryBootstrap(SHARED_STATE);

    public static BaseAgent initAgent() {
        ADK_LOGGER.info("Initializing TravelConciergeAgent with model=" + MODEL + ", name=" + NAME);
        BaseAgent agent = LlmAgent.builder()
                .name(NAME)
                .model(MODEL)
                .description(DESCRIPTION)
                .instruction(INSTRUCTION)
                .tools(
                        FunctionTool.create(TravelConciergeAgent.class, "getCurrentTime"),
                        FunctionTool.create(TravelConciergeAgent.class, "getWeather"),
                        // memory tools (session-scoped state)
                        FunctionTool.create(MEMORY_TOOLS, "memorize"),
                        FunctionTool.create(MEMORY_TOOLS, "memorizeList"),
                        FunctionTool.create(MEMORY_TOOLS, "forget"))
                .build();
        ADK_LOGGER.info("TravelConciergeAgent initialized. Tools registered: getCurrentTime, getWeather, memorize, memorizeList, forget");
        return agent;
    }

    public static Map<String, String> getCurrentTime(
            @Schema(name = "city",
                    description = "The name of the city for which to retrieve the current time")
            String city) {
        ADK_LOGGER.fine("getCurrentTime called with city='" + city + "'");
        String normalizedCity =
                Normalizer.normalize(city, Normalizer.Form.NFD)
                        .trim()
                        .toLowerCase()
                        .replaceAll("(\\p{IsM}+|\\p{IsP}+)", "")
                        .replaceAll("\\s+", "_");
        ADK_LOGGER.fine("Normalized city='" + normalizedCity + "'");

        return ZoneId.getAvailableZoneIds().stream()
                .filter(zid -> zid.toLowerCase().endsWith("/" + normalizedCity))
                .findFirst()
                .map(
                        zid -> {
                            String time = ZonedDateTime.now(ZoneId.of(zid))
                                    .format(DateTimeFormatter.ofPattern("HH:mm"));
                            ADK_LOGGER.info("Matched ZoneId='" + zid + "' for city='" + city + "', time=" + time);
                            return Map.of(
                                    "status",
                                    "success",
                                    "report",
                                    "The current time in " + city + " is " + time + ".");
                        })
                .orElseGet(() -> {
                    ADK_LOGGER.warning("No timezone information found for city='" + city + "' (normalized='" + normalizedCity + "')");
                    return Map.of(
                            "status",
                            "error",
                            "report",
                            "Sorry, I don't have timezone information for " + city + ".");
                });
    }

    public static Map<String, String> getWeather(
            @Schema(name = "city",
                    description = "The name of the city for which to retrieve the weather report")
            String city) {
        ADK_LOGGER.fine("getWeather called with city='" + city + "'");
        if (city.toLowerCase().equals("new york")) {
            ADK_LOGGER.info("Returning canned weather for New York.");
            return Map.of(
                    "status",
                    "success",
                    "report",
                    "The weather in New York is sunny with a temperature of 25 degrees Celsius (77 degrees"
                            + " Fahrenheit).");

        } else {
            ADK_LOGGER.warning("Weather information not available for city='" + city + "'");
            return Map.of(
                    "status", "error", "report", "Weather information for " + city + " is not available.");
        }
    }

    public static void main(String[] args) throws Exception {
        ADK_LOGGER.setLevel(Level.WARNING);
        InMemoryRunner runner = new InMemoryRunner(ROOT_AGENT);

        Session session =
                runner
                        .sessionService()
                        .createSession(NAME, USER_ID)
                        .blockingGet();
        ADK_LOGGER.info("Session created for user='" + USER_ID + "', sessionId=" + session.id());

        // Bootstrap the session-scoped memory (mimics Python before_agent callback)
        try {
            MEMORY_BOOTSTRAP.bootstrap(session.id());
        } catch (Exception e) {
            ADK_LOGGER.log(Level.WARNING, "Memory bootstrap failed; continuing with empty state.", e);
        }

        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            while (true) {
                System.out.print("\nYou > ");
                String userInput = scanner.nextLine();

                if (userInput == null) {
                    ADK_LOGGER.warning("Received null user input. Exiting.");
                    break;
                }
                if ("quit".equalsIgnoreCase(userInput.trim())) {
                    ADK_LOGGER.info("User requested to quit.");
                    break;
                }
                if (userInput.trim().isEmpty()) {
                    ADK_LOGGER.fine("Ignoring empty user input.");
                    continue;
                }

                Content userMsg = Content.fromParts(Part.fromText(userInput));
                ADK_LOGGER.fine("Dispatching user input to agent. length=" + userInput.length());
                Flowable<Event> events = runner.runAsync(USER_ID, session.id(), userMsg);

                System.out.print("\nAgent > ");
                final AtomicBoolean toolCalledInTurn = new AtomicBoolean(false);
                final AtomicBoolean toolErroredInTurn = new AtomicBoolean(false);

                events.blockingForEach(event -> {
                    if (event.content().isPresent()) {
                        event.content().get().parts().ifPresent(parts -> {
                            for (Part part : parts) {
                                if (part.text().isPresent()) {
                                    System.out.println(part.text().get());
                                }
                                if (part.functionCall().isPresent()) {
                                    toolCalledInTurn.set(true);
                                }
                                if (part.functionResponse().isPresent()) {
                                    FunctionResponse fr = part.functionResponse().get();
                                    fr.response().ifPresent(responseMap -> {
                                        if (responseMap.containsKey("error")
                                                || (responseMap.containsKey("status") && "error".equalsIgnoreCase(String.valueOf(responseMap.get("status"))))) {
                                            toolErroredInTurn.set(true);
                                        }
                                    });
                                }
                            }
                        });
                    }
                    if (event.errorCode().isPresent() || event.errorMessage().isPresent()) {
                        toolErroredInTurn.set(true);
                        ADK_LOGGER.warning("Agent event contained an error: code=" + event.errorCode().orElse(null) + ", message=" + event.errorMessage().orElse(null));
                    }
                });

                if (toolCalledInTurn.get() && !toolErroredInTurn.get()) {
                    ADK_LOGGER.fine("A tool was used successfully in this turn.");
                }
                if (toolErroredInTurn.get()) {
                    ADK_LOGGER.warning("An error occurred during tool execution or in the agent's response processing.");
                }
            }
        }
    }
}
