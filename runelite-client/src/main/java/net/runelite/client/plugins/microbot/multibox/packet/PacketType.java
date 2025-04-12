package net.runelite.client.plugins.microbot.multibox.packet;

public enum PacketType {
    MOVEMENT(0x01),
    INTERACTION(0x02),
    MINIMAP_CLICK(0x03),
    KEY_PRESS(0x04),
    KEY_RELEASE(0x05),
    STATE_UPDATE(0x06),
    ERROR(0xFF);

    private final byte opcode;

    PacketType(int opcode) {
        this.opcode = (byte) opcode;
    }

    public byte getOpcode() {
        return opcode;
    }

    public static PacketType fromOpcode(byte opcode) {
        for (PacketType type : values()) {
            if (type.opcode == opcode) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown packet opcode: " + opcode);
    }
}
