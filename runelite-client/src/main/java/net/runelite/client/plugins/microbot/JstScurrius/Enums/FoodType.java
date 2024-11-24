package net.runelite.client.plugins.microbot.JstScurrius.Enums;

public enum FoodType {
    MONKFISH(7946),
    KARAMBWANS(3144),
    MANTA_RAY(391),
    SHARK(385),
    ANGLERFISH(13441);

    private final int itemId;

    private FoodType(int itemId) {
        this.itemId = itemId;
    }

    public int getItemId() {
        return this.itemId;
    }
}
