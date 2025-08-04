package net.runelite.client.plugins.microbot.jstvale.enums;

public enum JstValeState {
    BANKING("Banking"),
    WALKING_TO_BANK("Walking to bank"),
    WALKING_TO_TOTEM("Walking to totem"),
    CONSTRUCTING_TOTEM("Constructing totem"),
    DECORATING_TOTEM("Decorating totem"),
    COLLECTING_OFFERINGS("Collecting offerings"),
    WAITING("Waiting"),
    FLECHING_LOGS("Fletching logs");

    private final String description;

    JstValeState(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}