package org.example.agents.multitool.shared;

/**
 * Constants used as keys into session-scoped shared state for the Travel Concierge agent.
 * Mirrors the Python sample's constants.
 */
public final class Constants {
    private Constants() {}

    public static final String SYSTEM_TIME = "_time";
    public static final String ITIN_INITIALIZED = "_itin_initialized";

    public static final String ITIN_KEY = "itinerary";
    public static final String PROF_KEY = "user_profile";

    public static final String ITIN_START_DATE = "itinerary_start_date";
    public static final String ITIN_END_DATE = "itinerary_end_date";
    public static final String ITIN_DATETIME = "itinerary_datetime";

    public static final String START_DATE = "start_date";
    public static final String END_DATE = "end_date";
}
