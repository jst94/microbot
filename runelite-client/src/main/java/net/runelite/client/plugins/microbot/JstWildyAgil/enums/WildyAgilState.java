package net.runelite.client.plugins.microbot.JstWildyAgil.enums;

public enum WildyAgilState {
    STARTING,
    WALKING_TO_COURSE_ENTRANCE,
    ENTERING_COURSE,
    CROSSING_PIPE,
    CROSSING_ROPE_SWING,
    CROSSING_STEPPING_STONES,
    CROSSING_LOG_BALANCE,
    CLIMBING_ROCKS,
    COLLECTING_TOKENS,
    WALKING_TO_TICKET_DISPENSER,
    HANDING_IN_TICKETS,
    LEAVING_COURSE, // Toegevoegd voor duidelijkheid na ticket inleveren als niet direct opnieuw wordt gestart
    BANKING, 
    HANDLING_DANGER, 
    FINISHED,
    ERROR
}