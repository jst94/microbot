package net.runelite.client.plugins.microbot.multibox.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MinimapClickMessage extends BaseMessage {
    private int x;
    private int y;

    public MinimapClickMessage(int x, int y) {
        super(MessageType.MINIMAP_CLICK);
        this.x = x;
        this.y = y;
    }
}