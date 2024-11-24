package net.runelite.client.plugins.microbot.JstScurrius.Enums;

public enum ScurriusRestorePotions {
    PRAYER_POTION(2434),
    SUPER_RESTORE(3024);

    private final int itemIds;

    private ScurriusRestorePotions(int itemIds) {
        this.itemIds = itemIds;
    }

    public int getItemIds() {
        return this.itemIds;
    }
}
