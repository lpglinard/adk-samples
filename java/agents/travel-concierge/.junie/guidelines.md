Project: Travel Concierge (Google ADK sample)

Scope Restriction (Critical)
- THIS IS A BIG SET OF SAMPLES OF ADK. We are currently developing java/agents/travel-concierge so do not write files on other folders, only write on java/agents/travel-concierge and its sub-folders.
- Rationale: The repository contains many independent samples; to avoid cross-sample interference and unintended changes, confine edits to this agent’s directory tree.

Scope
- Advanced developer notes to build, test, and extend this sample agent. This repo demonstrates using Google ADK to build a travel concierge agent exposed via the Dev UI. All user interaction is intended to happen through google-adk-dev’s DEV-UI.

Environment and Build
- Toolchain: Java 21, Maven ≥ 3.9.x.
- Dependencies: google-adk 0.2.0 and google-adk-dev 0.2.0.
- Build: mvn -q -DskipTests package or simply mvn package.
- LLM access: The sample references model "gemini-2.0-flash". Set your Google GenAI API key in the environment so ADK/GenAI can auth (e.g., export GOOGLE_API_KEY=... or equivalent for your shell). Refer to your organization’s credential distribution and the google-adk/genai docs to confirm the exact variable name and auth mechanism in your environment.

Project Layout and Conventions
- Root agent: org.example.agents.multitool.TravelConciergeAgent defines a public static final BaseAgent ROOT_AGENT. This is a convention used by google-adk-dev’s Dev UI to locate the root agent at runtime.
- Agent design: Keep each agent in its own class. Encapsulate tools as static methods on the agent and register them with FunctionTool.create(...).
- Prompts/resources: Prompt instructions should reside under src/main/resources and be loaded at runtime (DRY). The current sample inlines instruction text for brevity; when extending, move it to a dedicated prompt file (e.g., src/main/resources/prompts/multi_tool_agent.txt) and load it with Files.readString in a small helper to avoid duplication.
- Principles: Apply DRY and YAGNI. Keep tool signatures minimal and typed via @Schema for clear auto-generated tool specs.

Running the Agent
- Console mode: TravelConciergeAgent contains a main method wired with InMemoryRunner for quick manual testing from the console. Run from your IDE (Java Application) with the working directory at the project root. Type quit to exit.
- Dev UI: With google-adk-dev on the classpath and ROOT_AGENT exposed, the Dev UI can introspect the project and interact with the agent. Launch the Dev UI per your team’s ADK setup (the Dev UI is not launched by this repo). Ensure the app providing the Dev UI can see this classpath and that ROOT_AGENT is accessible.
- Web server (Dev UI host): You can start the ADK web server with Maven:
  mvn exec:java -Dexec.mainClass="com.google.adk.web.AdkWebServer" -Dexec.args="--adk.agents.source-dir=src/main/java" -Dexec.classpathScope="compile"

Testing
- Framework: JUnit 5 (junit-jupiter-api + junit-jupiter-engine) is configured in pom.xml. Maven Surefire is pinned to a version compatible with Java 21.
- Run tests: mvn -q test or mvn test. To target a single class: mvn -q -Dtest=org.example.agents.multitool.SmokeTest test
- Adding tests: Place tests under src/test/java mirroring package structure. Prefer focused unit tests that don’t require network calls. For tool methods, test pure logic (e.g., timezone parsing, normalization) and edge cases.
- Example test: Create src/test/java/org/example/agents/multitool/SmokeTest.java with:

  package org.example.agents.multitool;

  import org.junit.jupiter.api.Test;
  import static org.junit.jupiter.api.Assertions.*;
  import java.util.Map;

  public class SmokeTest {
      @Test
      void timeAndWeatherBasic() {
          Map<String, String> time = TravelConciergeAgent.getCurrentTime("New York");
          assertEquals("success", time.get("status"));

          Map<String, String> weather = TravelConciergeAgent.getWeather("New York");
          assertEquals("success", weather.get("status"));
      }
  }

  Then run: mvn -q test

  Expected: Tests pass. This validates the time zone matching logic and the weather stub for New York.

Notes on Implementation Details
- Package hygiene: Ensure package names match directory layout. This repo uses org.example.agents.multitool to align with src/main/java/org/example/agents/multitool.
- Time zone lookup: getCurrentTime normalizes the city and searches ZoneId list by suffix match (/normalized_city). This works for common city mappings like America/New_York. Consider extending with a curated mapping table for ambiguous or multi-word cities, but only if a real use-case demands it (YAGNI).
- Weather tool: getWeather is a stub (success for "New York", error otherwise). Replace with a real provider only when you have clear requirements and stable interfaces. Keep tool contracts narrow and return structured results (status/report) suitable for LLM tool calling.
- Instructions: Keep LlmAgent instruction strings short and offload specifics to resource prompts. Keep them composable and reusable across agents.
- Debugging tips:
  - Enable verbose logging in ADK/GenAI if you need to inspect tool calls and model responses.
  - When integrating with Dev UI, verify that ROOT_AGENT is public, static, and final.
  - Prefer InMemoryRunner for deterministic local runs; record flows if needed.

Maven Configuration (key excerpts)
- junit-jupiter-api and junit-jupiter-engine added as test-scoped dependencies.
- maven-surefire-plugin pinned to 3.2.5 with useModulePath=false for Java 21 compatibility.

Housekeeping
- Keep sample prompts and tools minimal. Avoid adding external dependencies unless necessary for a concrete feature.
- If you temporarily add demo tests, remove them before committing if they are not intended to be part of the permanent suite. The example above was verified locally and then removed, as this repo focuses on the agent sample itself.
