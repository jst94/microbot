package net.runelite.client.plugins.microbot.JstScurrius.Enums;

public enum ScurriusRunePouch {
    NORMAL(12791, "Regular Rune Pouch"),
    NORMAL_L(24416, "Regular Rune Pouch (l)"),
    DIVINE_L(27509, "Divine Rune Pouch (l)"),
    DIVINE(27281, "Divine Rune Pouch");

    private final int itemId;
    private final String pouchName;

    private ScurriusRunePouch(int itemId, String pouchName) {
        this.itemId = itemId;
        this.pouchName = pouchName;
    }

    public int getItemId() {
        return this.itemId;
    }

    public String getPouchName() {
        return this.pouchName;
    }
}
