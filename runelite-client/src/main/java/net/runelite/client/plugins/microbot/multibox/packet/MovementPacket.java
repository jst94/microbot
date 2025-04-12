package net.runelite.client.plugins.microbot.multibox.packet;

import lombok.Getter;

import lombok.extern.slf4j.Slf4j; // Added for logging

@Getter
@Slf4j // Added for logging
public class MovementPacket {
    public static final byte OPCODE = 0x01; // Movement packet opcode

    private final int sceneX;
    private final int sceneY;
    private final int worldX;
    private final int worldY;
    private final int plane;
    private final boolean ctrlDown;

    public MovementPacket(int sceneX, int sceneY, boolean ctrlDown) {
        this.sceneX = sceneX;
        this.sceneY = sceneY;
        this.worldX = -1;  // Will be set when needed
        this.worldY = -1;
        this.plane = -1;
        this.ctrlDown = ctrlDown;
    }

    public MovementPacket(int worldX, int worldY, int plane, boolean ctrlDown) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.plane = plane;
        this.sceneX = -1;  // Will be calculated from world coords
        this.sceneY = -1;
        this.ctrlDown = ctrlDown;
    }

    public byte[] serialize() {
        // Size: opcode(1) + sceneX(4) + sceneY(4) + worldX(4) + worldY(4) + plane(4) + ctrlDown(1) = 22 bytes
        byte[] data = new byte[22];
        data[0] = OPCODE;
        writeInt(data, 1, sceneX);      // Offset 1, Length 4
        writeInt(data, 5, sceneY);      // Offset 5, Length 4
        writeInt(data, 9, worldX);      // Offset 9, Length 4 (Changed from byte)
        writeInt(data, 13, worldY);     // Offset 13, Length 4 (Changed from byte)
        writeInt(data, 17, plane);      // Offset 17, Length 4 (Changed from byte)
        data[21] = (byte) (ctrlDown ? 1 : 0); // Offset 21, Length 1
        return data;
    }

    public static MovementPacket deserialize(byte[] data) {
        // Add logging just before the check
        log.debug("Deserializing MovementPacket: length={}, firstByte={}",
            (data != null ? data.length : "null"),
            (data != null && data.length > 0 ? String.format("0x%02X", data[0]) : "N/A"));
            
        // Check new size (22 bytes) and update exception message
        if (data == null || data[0] != OPCODE || data.length != 22) {
            throw new IllegalArgumentException(String.format("Invalid movement packet: Opcode=%02X, Length=%d (Expected Opcode=%02X, Length=22)",
                (data != null && data.length > 0 ? data[0] : -1), (data != null ? data.length : -1), OPCODE));
        }

        int sceneX = readInt(data, 1);
        int sceneY = readInt(data, 5);
        // World coords are now ints at different offsets
        int worldX = readInt(data, 9);
        int worldY = readInt(data, 13);
        int plane = readInt(data, 17);
        boolean ctrlDown = data[21] == 1; // ctrlDown is now at offset 21

        if (sceneX != -1 && sceneY != -1) {
            return new MovementPacket(sceneX, sceneY, ctrlDown);
        } else {
            return new MovementPacket(worldX, worldY, plane, ctrlDown);
        }
    }

    private static void writeInt(byte[] data, int offset, int value) {
        data[offset] = (byte) (value >> 24);
        data[offset + 1] = (byte) (value >> 16);
        data[offset + 2] = (byte) (value >> 8);
        data[offset + 3] = (byte) value;
    }

    private static int readInt(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24) |
               ((data[offset + 1] & 0xFF) << 16) |
               ((data[offset + 2] & 0xFF) << 8) |
               (data[offset + 3] & 0xFF);
    }

    @Override
    public String toString() {
        if (sceneX != -1 && sceneY != -1) {
            return String.format("MovementPacket(scene=(%d,%d), ctrl=%b)", sceneX, sceneY, ctrlDown);
        } else {
            return String.format("MovementPacket(world=(%d,%d,%d), ctrl=%b)", worldX, worldY, plane, ctrlDown);
        }
    }
}
