package net.runelite.client.plugins.microbot.multibox.message;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

@Getter
public class StateUpdateMessage extends BaseMessage {
    private final int currentHealth;
    private final int maxHealth;
    private final int currentPrayer;
    private final int maxPrayer;
    private final int worldX;
    private final int worldY;
    private final int plane;

    public StateUpdateMessage(int currentHealth, int maxHealth, int currentPrayer, int maxPrayer,
                            int worldX, int worldY, int plane) {
        super(MessageType.STATE_UPDATE);
        this.currentHealth = currentHealth;
        this.maxHealth = maxHealth;
        this.currentPrayer = currentPrayer;
        this.maxPrayer = maxPrayer;
        this.worldX = worldX;
        this.worldY = worldY;
        this.plane = plane;
    }

    public double getHealthPercent() {
        return maxHealth > 0 ? (double) currentHealth / maxHealth * 100 : 0;
    }

    public double getPrayerPercent() {
        return maxPrayer > 0 ? (double) currentPrayer / maxPrayer * 100 : 0;
    }

    public WorldPoint getLocation() {
        return new WorldPoint(worldX, worldY, plane);
    }

    @Override
    public String toString() {
        return String.format("StateUpdate(HP=%d/%d (%.1f%%), Prayer=%d/%d (%.1f%%), Loc=(%d,%d,%d))",
            currentHealth, maxHealth, getHealthPercent(),
            currentPrayer, maxPrayer, getPrayerPercent(),
            worldX, worldY, plane);
    }
}
