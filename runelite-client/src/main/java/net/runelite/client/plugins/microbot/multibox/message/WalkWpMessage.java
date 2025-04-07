package net.runelite.client.plugins.microbot.multibox.message;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint; // Import WorldPoint

@Getter
public class WalkWpMessage extends BaseMessage {
    private final int worldX;
    private final int worldY;
    private final int plane;

    public WalkWpMessage(int worldX, int worldY, int plane) {
        super(MessageType.WALK_WP);
        this.worldX = worldX;
        this.worldY = worldY;
        this.plane = plane;
    }

    // Convenience method to get WorldPoint
    public WorldPoint getWorldPoint() {
        return new WorldPoint(this.worldX, this.worldY, this.plane);
    }
}