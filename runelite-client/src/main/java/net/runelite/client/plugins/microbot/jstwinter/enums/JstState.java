package net.runelite.client.plugins.microbot.jstwinter.enums;

public enum JstState {
    BANKING("Banking"),
    ENTER_ROOM("Entering room"),
    WAITING("Waiting for Wintertodt"),
    LIGHT_BRAZIER("Lighting brazier"),
    CHOP_ROOTS("Chopping roots"),
    FLETCH_KINDLING("Fletching kindling"),
    FEED_BRAZIER("Feeding brazier"),
    FIX_BRAZIER("Fixing brazier"),
    HEAL_PYROMANCER("Healing pyromancer"),
    DODGING("Dodging attack"),
    EATING("Eating food"),
    MAKE_POTIONS("Making rejuvenation potions");

    private final String description;

    JstState(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}