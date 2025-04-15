package net.runelite.client.plugins.microbot.multibox.packet;

import lombok.Getter;
import java.nio.ByteBuffer;

@Getter
public class GamePacket {
    private final PacketType type;
    private final byte[] data;
    private final long timestamp;

    public GamePacket(PacketType type, byte[] data) {
        this.type = type;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(data.length + 13); // type(1) + timestamp(8) + length(4) + data
        buffer.put(type.getOpcode());
        buffer.putLong(timestamp);
        buffer.putInt(data.length);
        buffer.put(data);
        return buffer.array();
    }

    public static GamePacket deserialize(byte[] rawData) {
        if (rawData == null || rawData.length < 13) {
            throw new IllegalArgumentException("Invalid packet data");
        }

        ByteBuffer buffer = ByteBuffer.wrap(rawData);
        
        // Read packet type
        byte typeOpcode = buffer.get();
        PacketType type = PacketType.fromOpcode(typeOpcode);
        
        // Read timestamp
        long timestamp = buffer.getLong();
        
        // Read data length and payload
        int dataLength = buffer.getInt();
        if (dataLength < 0 || dataLength > buffer.remaining()) {
            throw new IllegalArgumentException("Invalid data length in packet");
        }
        
        byte[] data = new byte[dataLength];
        buffer.get(data);

        // Create packet - we use a custom constructor for the timestamp
        return new GamePacket(type, timestamp, data);
    }

    // Private constructor for deserialization
    private GamePacket(PacketType type, long timestamp, byte[] data) {
        this.type = type;
        this.timestamp = timestamp;
        this.data = data;
    }

    @Override
    public String toString() {
        return String.format("GamePacket(type=%s, dataLength=%d, timestamp=%d)", 
            type, data.length, timestamp);
    }

    // Helper methods for specific packet types
    public static GamePacket createMovementPacket(int sceneX, int sceneY, boolean ctrlDown) {
        byte[] movementData = PacketBuffer.createMovementPacket(sceneX, sceneY, ctrlDown);
        return new GamePacket(PacketType.MOVEMENT, movementData);
    }

    public static GamePacket createWorldMovementPacket(int worldX, int worldY, int plane, boolean ctrlDown) {
        byte[] movementData = PacketBuffer.createWorldMovementPacket(worldX, worldY, plane, ctrlDown);
        return new GamePacket(PacketType.MOVEMENT, movementData);
    }

    public static GamePacket createInteractionPacket(int param0, int param1, int id, net.runelite.api.MenuAction menuAction, String option, String target, String objectType) {
        InteractionPacket packet = new InteractionPacket(param0, param1, id, menuAction, option, target, objectType);
        byte[] interactionData = packet.serialize();
        return new GamePacket(PacketType.INTERACTION, interactionData);
    }

    public static GamePacket createErrorPacket(String errorMessage) {
        byte[] errorData = errorMessage.getBytes();
        return new GamePacket(PacketType.ERROR, errorData);
    }
}
