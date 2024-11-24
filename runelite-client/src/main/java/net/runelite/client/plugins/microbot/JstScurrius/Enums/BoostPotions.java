package net.runelite.client.plugins.microbot.JstScurrius.Enums;

public enum BoostPotions {
    SUPER_COMBAT(12695),
    DIVINE_COMBAT(23685),
    RANGING(2444),
    DIVINE_RANGING(23733);

    private final int itemId;

    private BoostPotions(int itemId) {
        this.itemId = itemId;
    }

    public int getItemId() {
        return this.itemId;
    }
}
