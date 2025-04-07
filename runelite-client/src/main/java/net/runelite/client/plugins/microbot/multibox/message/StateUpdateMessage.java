package net.runelite.client.plugins.microbot.multibox.message;

import lombok.Getter;

@Getter
public class StateUpdateMessage extends BaseMessage {
    private final int currentHealth;
    private final int maxHealth;
    private final int currentPrayer;
    private final int maxPrayer;
    private final int worldX;
    private final int worldY;
    private final int plane;

    public StateUpdateMessage(int currentHealth, int maxHealth, int currentPrayer, int maxPrayer, int worldX, int worldY, int plane) {
        super(MessageType.STATE_UPDATE);
        this.currentHealth = currentHealth;
        this.maxHealth = maxHealth;
        this.currentPrayer = currentPrayer;
        this.maxPrayer = maxPrayer;
        this.worldX = worldX;
        this.worldY = worldY;
        this.plane = plane;
    }
}