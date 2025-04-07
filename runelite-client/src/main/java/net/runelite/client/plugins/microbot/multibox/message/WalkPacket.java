package net.runelite.client.plugins.microbot.multibox.message;

import lombok.Getter;

@Getter
public class WalkPacket extends BaseMessage {
    private final int sceneX;
    private final int sceneY;
    private final boolean ctrlDown;

    public WalkPacket(int sceneX, int sceneY, boolean ctrlDown) {
        super(MessageType.WALK_PACKET);
        this.sceneX = sceneX;
        this.sceneY = sceneY;
        this.ctrlDown = ctrlDown;
    }
    
    // Public getters are provided by Lombok @Getter annotation
}
