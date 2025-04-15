package net.runelite.client.plugins.microbot.multibox.packet;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.MenuAction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Getter
@Slf4j
public class InteractionPacket {
    public static final byte OPCODE = PacketType.INTERACTION.getOpcode(); // 0x02

    private final int param0;
    private final int param1;
    private final int id;
    private final int menuActionId; // Store MenuAction ID
    private final String option;
    private final String target;
    private final String objectType;

    public InteractionPacket(int param0, int param1, int id, MenuAction menuAction, String option, String target, String objectType) {
        this.param0 = param0;
        this.param1 = param1;
        this.id = id;
        this.menuActionId = menuAction.getId(); // Store the ID
        this.option = option != null ? option : ""; // Ensure non-null
        this.target = target != null ? target : ""; // Ensure non-null
        this.objectType = objectType != null ? objectType : ""; // Ensure non-null
    }

    // Private constructor for deserialization
    private InteractionPacket(int param0, int param1, int id, int menuActionId, String option, String target, String objectType) {
        this.param0 = param0;
        this.param1 = param1;
        this.id = id;
        this.menuActionId = menuActionId;
        this.option = option;
        this.target = target;
        this.objectType = objectType;
    }

    public MenuAction getMenuAction() {
        // Reconstruct MenuAction from ID
        return MenuAction.of(menuActionId);
    }

    public byte[] serialize() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            dos.writeByte(OPCODE); // Write opcode first
            dos.writeInt(param0);
            dos.writeInt(param1);
            dos.writeInt(id);
            dos.writeInt(menuActionId);

            byte[] objectTypeBytes = objectType.getBytes(StandardCharsets.UTF_8);
            dos.writeShort(objectTypeBytes.length); // Write length of objectType string
            dos.write(objectTypeBytes);

            byte[] optionBytes = option.getBytes(StandardCharsets.UTF_8);
            dos.writeShort(optionBytes.length); // Write length of option string
            dos.write(optionBytes);

            byte[] targetBytes = target.getBytes(StandardCharsets.UTF_8);
            dos.writeShort(targetBytes.length); // Write length of target string
            dos.write(targetBytes);

            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Error serializing InteractionPacket", e);
            return new byte[]{OPCODE}; // Return minimal byte array on error
        }
    }

    public static InteractionPacket deserialize(byte[] data) throws IOException {
        if (data == null || data.length < 1 + 4 * 4 + 2 + 2 + 2) { // Opcode + 4 ints + 3 shorts (min lengths)
             throw new IllegalArgumentException("Invalid interaction packet data: too short");
        }
        if (data[0] != OPCODE) {
            throw new IllegalArgumentException("Invalid interaction packet data: incorrect opcode");
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             DataInputStream dis = new DataInputStream(bais)) {

            byte opcode = dis.readByte(); // Read and discard opcode

            int param0 = dis.readInt();
            int param1 = dis.readInt();
            int id = dis.readInt();
            int menuActionId = dis.readInt();

            int optionLen = dis.readShort();
            if (optionLen < 0 || optionLen > dis.available()) throw new IOException("Invalid option length");
            byte[] optionBytes = new byte[optionLen];
            dis.readFully(optionBytes);
            String option = new String(optionBytes, StandardCharsets.UTF_8);

            int targetLen = dis.readShort();
             if (targetLen < 0 || targetLen > dis.available()) throw new IOException("Invalid target length");
            byte[] targetBytes = new byte[targetLen];
            dis.readFully(targetBytes);
            String target = new String(targetBytes, StandardCharsets.UTF_8);

            int objectTypeLen = dis.readShort();
            if (objectTypeLen < 0 || objectTypeLen > dis.available()) throw new IOException("Invalid objectType length");
            byte[] objectTypeBytes = new byte[objectTypeLen];
            dis.readFully(objectTypeBytes);
            String objectType = new String(objectTypeBytes, StandardCharsets.UTF_8);

            return new InteractionPacket(param0, param1, id, menuActionId, option, target, objectType);
        }
    }

    @Override
    public String toString() {
        return String.format("InteractionPacket(action=%s, id=%d, option='%s', target='%s', objectType='%s', params=(%d,%d))",
                getMenuAction(), id, option, target, objectType, param0, param1);
    }
}