package net.runelite.client.plugins.microbot.multibox.packet;

import lombok.extern.slf4j.Slf4j;
import java.nio.ByteBuffer;
import java.util.Arrays;

@Slf4j
public class PacketBuffer {
    private static final int INITIAL_CAPACITY = 1024;
    private static final int HEADER_SIZE = 5; // 1 byte opcode + 4 bytes length
    
    private ByteBuffer buffer;

    public PacketBuffer() {
        buffer = ByteBuffer.allocate(INITIAL_CAPACITY);
    }

    public void write(byte[] data) {
        ensureCapacity(data.length + HEADER_SIZE);
        
        // Write header
        buffer.put(PacketType.MOVEMENT.getOpcode());
        buffer.putInt(data.length);
        
        // Write payload
        buffer.put(data);
    }

    public byte[] readPacket() {
        if (buffer.position() < HEADER_SIZE) {
            return null; // Not enough data for a complete header
        }

        // Read packet header
        buffer.flip();
        byte opcode = buffer.get();
        int length = buffer.getInt();

        // Validate packet
        if (buffer.remaining() < length) {
            buffer.position(buffer.limit());
            buffer.limit(buffer.capacity());
            return null; // Not enough data for complete packet
        }

        // Read payload
        byte[] data = new byte[length];
        buffer.get(data);

        // Prepare buffer for next read
        if (buffer.hasRemaining()) {
            // Compact remaining data
            buffer.compact();
        } else {
            // Reset buffer if empty
            buffer.clear();
        }

        return data;
    }

    public void clear() {
        buffer.clear();
    }

    private void ensureCapacity(int required) {
        if (buffer.remaining() < required) {
            int newCapacity = Math.max(buffer.capacity() * 2, buffer.position() + required);
            ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity);
            buffer.flip();
            newBuffer.put(buffer);
            buffer = newBuffer;
        }
    }

    public static byte[] createMovementPacket(int sceneX, int sceneY, boolean ctrlDown) {
        MovementPacket packet = new MovementPacket(sceneX, sceneY, ctrlDown);
        return packet.serialize();
    }

    public static byte[] createWorldMovementPacket(int worldX, int worldY, int plane, boolean ctrlDown) {
        MovementPacket packet = new MovementPacket(worldX, worldY, plane, ctrlDown);
        return packet.serialize();
    }

    public static boolean isValidPacket(byte[] data) {
        try {
            if (data == null || data.length < HEADER_SIZE) {
                return false;
            }
            
            PacketType.fromOpcode(data[0]); // Validate opcode
            int length = ByteBuffer.wrap(Arrays.copyOfRange(data, 1, HEADER_SIZE)).getInt();
            return data.length == length + HEADER_SIZE;
            
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
