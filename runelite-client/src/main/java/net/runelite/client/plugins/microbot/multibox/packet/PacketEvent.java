package net.runelite.client.plugins.microbot.multibox.packet;

import lombok.Getter;

@Getter
public class PacketEvent {
    private final PacketType type;
    private final int sceneX;
    private final int sceneY;
    private final boolean ctrlDown;

    public PacketEvent(PacketType type, int sceneX, int sceneY, boolean ctrlDown) {
        this.type = type;
        this.sceneX = sceneX;
        this.sceneY = sceneY;
        this.ctrlDown = ctrlDown;
    }
}
