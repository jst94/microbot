package net.runelite.client.plugins.microbot.JstScurrius.Enums;

public enum PotionType {
    SUPER_ATTACK("Super attack", 4),
    SUPER_STRENGTH("Super strength", 4),
    PRAYER("Prayer potion", 4),
    SUPER_COMBAT("Super combat potion", 4),
    RANGING("Ranging potion", 4);

    private final String name;
    private final int maxDose;

    private PotionType(String name, int maxDose) {
        this.name = name;
        this.maxDose = maxDose;
    }

    public String getName() {
        return this.name;
    }

    public int getMaxDose() {
        return this.maxDose;
    }
}
