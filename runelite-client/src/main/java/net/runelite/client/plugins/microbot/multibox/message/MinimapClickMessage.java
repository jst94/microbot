package net.runelite.client.plugins.microbot.multibox.message;

import lombok.Getter;
import java.awt.Point;

@Getter
public class MinimapClickMessage extends BaseMessage {
    private final int x;
    private final int y;

    public MinimapClickMessage(int x, int y) {
        super(MessageType.MINIMAP_CLICK);
        this.x = x;
        this.y = y;
    }

    public Point getPoint() {
        return new Point(x, y);
    }

    @Override
    public String toString() {
        return String.format("MinimapClickMessage(x=%d, y=%d)", x, y);
    }
}
