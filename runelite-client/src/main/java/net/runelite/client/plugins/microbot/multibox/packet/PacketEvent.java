package net.runelite.client.plugins.microbot.multibox.packet;

import lombok.Getter;

@Getter
public class PacketEvent {
    private final PacketType type;
    private final byte[] data;
    private final long timestamp;
    private final boolean isIncoming;

    public PacketEvent(PacketType type, byte[] data, boolean isIncoming) {
        this.type = type;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
        this.isIncoming = isIncoming;
    }

    public GamePacket toGamePacket() {
        return new GamePacket(type, data);
    }

    public MovementPacket toMovementPacket() {
        if (type != PacketType.MOVEMENT) {
            throw new IllegalStateException("Cannot convert non-movement packet to MovementPacket");
        }
        return MovementPacket.deserialize(data);
    }

    @Override
    public String toString() {
        return String.format("PacketEvent(%s, length=%d, incoming=%b, time=%d)", 
            type, data.length, isIncoming, timestamp);
    }
}
