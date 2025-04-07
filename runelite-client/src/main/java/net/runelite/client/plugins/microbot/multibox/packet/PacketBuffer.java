package net.runelite.client.plugins.microbot.multibox.packet;

import net.runelite.api.Client;
import net.runelite.client.plugins.microbot.Microbot;

public class PacketBuffer {
    // Common packet opcodes
    public static final int MOVE_GAME_CLICK = 70;  // Movement opcode
    public static final int WALK_MINIMAP = 71;     // Minimap click opcode
    public static final int OBJECT_CLICK = 72;     // Object interaction opcode
    
    private Client client;
    private int opcode;
    private int[] payload;
    private int writePosition;
    
    public PacketBuffer(int opcode, int size) {
        this.client = Microbot.getClient();
        this.opcode = opcode;
        this.payload = new int[size];
        this.writePosition = 0;
    }
    
    public void writeByte(int value) {
        payload[writePosition++] = value & 0xFF;
    }
    
    public void writeShort(int value) {
        writeByte(value >> 8);
        writeByte(value);
    }
    
    public void writeInt(int value) {
        writeShort(value >> 16);
        writeShort(value);
    }
    
    public void send() {
        if (client == null) return;
        
        try {
            // Here you'll implement the actual packet sending using your game client's methods
            // For example:
            // client.getPacketWriter().sendPacket(opcode, payload);
            
            // For now we'll log what would be sent
            StringBuilder debug = new StringBuilder();
            debug.append(String.format("Packet opcode=%d payload=[", opcode));
            for (int i = 0; i < writePosition; i++) {
                if (i > 0) debug.append(", ");
                debug.append(String.format("0x%02X", payload[i]));
            }
            debug.append("]");
            System.out.println(debug.toString());
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to send packet: " + e.getMessage());
        }
    }
    
    public static void sendMovement(int sceneX, int sceneY, boolean ctrl) {
        PacketBuffer buffer = new PacketBuffer(MOVE_GAME_CLICK, 4);
        buffer.writeShort(sceneX);
        buffer.writeShort(sceneY);
        if (ctrl) {
            buffer.writeByte(2); // ctrl flag
        }
        buffer.send();
    }
}
