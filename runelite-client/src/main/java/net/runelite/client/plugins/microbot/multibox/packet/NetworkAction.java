package net.runelite.client.plugins.microbot.multibox.packet;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;

@Slf4j
public class NetworkAction {
    
    public static void sendMovementPacket(int sceneX, int sceneY, boolean ctrlDown) {
        try {
            if (Microbot.getClient() == null) {
                log.warn("Client is null, can't send movement packet");
                return;
            }

            log.debug("Sending movement packet: scene({}, {}), ctrl={}", sceneX, sceneY, ctrlDown);
            PacketBuffer.sendMovement(sceneX, sceneY, ctrlDown);
        } catch (Exception e) {
            log.error("Failed to send movement packet", e);
            throw new RuntimeException("Failed to send movement packet", e);
        }
    }

    public static void sendWorldPointMovement(WorldPoint target) {
        try {
            if (Microbot.getClient() == null || target == null) {
                log.warn("Client or target is null, can't send world point movement");
                return;
            }
            
            // Convert world point to scene coordinates
            int baseX = Microbot.getClient().getBaseX();
            int baseY = Microbot.getClient().getBaseY();
            int sceneX = target.getX() - baseX;
            int sceneY = target.getY() - baseY;

            log.debug("Sending world point movement: world({}, {}) -> scene({}, {})",
                     target.getX(), target.getY(), sceneX, sceneY);
            
            PacketBuffer.sendMovement(sceneX, sceneY, false);
        } catch (Exception e) {
            log.error("Failed to send world point movement packet", e);
            throw new RuntimeException("Failed to send world point movement packet", e);
        }
    }
}
