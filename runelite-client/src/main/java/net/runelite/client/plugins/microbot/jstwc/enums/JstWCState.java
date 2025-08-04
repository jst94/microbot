package net.runelite.client.plugins.microbot.jstwc.enums;

public enum JstWCState {
    INITIALIZING("Initializing"),
    CHECKING_EQUIPMENT("Checking equipment"),
    BANKING_FOR_EQUIPMENT("Banking for equipment"),
    BUYING_EQUIPMENT("Buying equipment"),
    COLLECTING_PURCHASES("Collecting purchases"),
    PREPARING_FOR_TRAVEL("Preparing for travel"),
    TRAVELING_TO_LOCATION("Traveling to location"),
    SETTING_UP_AGGRESSION("Setting up aggression"),
    WORLD_HOPPING("World hopping"),
    POSITIONING("Positioning between trees"),
    TWO_TICK_CUTTING("2-tick cutting"),
    DROPPING_LOGS("Dropping logs"),
    EATING_FOOD("Eating food"),
    REGAINING_AGGRESSION("Regaining aggression"),
    WAITING("Waiting"),
    ERROR("Error"),
    RETRYING("Retrying failed action");
    
    private final String description;
    
    JstWCState(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return description;
    }
}