package net.runelite.client.plugins.microbot.multibox.packet;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.mouse.VirtualMouse;
import net.runelite.client.plugins.microbot.multibox.MultiboxConfig;

import java.awt.Point;
import java.awt.Canvas;

@Slf4j
public class NetworkAction {

    private static final int MAX_SCENE_SIZE = 104; // RuneScape scene size is 104x104
    private static final int CHUNK_SIZE = 8; // Scene chunks are 8x8
    private static final int TILE_SIZE = 128; // Local coordinate size of a tile

    public static void sendMovementPacket(int sceneX, int sceneY, boolean ctrlDown) {
        try {
            if (Microbot.getClient() == null) {
                log.warn("Client is null, can't send movement packet");
                return;
            }

            // Normalize scene coordinates to be within valid range
            sceneX = Math.floorMod(sceneX, MAX_SCENE_SIZE);
            sceneY = Math.floorMod(sceneY, MAX_SCENE_SIZE);

            // Convert scene coordinates to local point
            LocalPoint localPoint = LocalPoint.fromScene(sceneX, sceneY);
            if (localPoint == null) {
                log.error("Could not convert scene coordinates to local point");
                return;
            }

            Point canvasPoint = Microbot.getClient().getCanvas().getMousePosition();
            if (canvasPoint == null) {
                // If no current mouse position, calculate based on center of tile
                canvasPoint = getCanvasLocation(sceneX, sceneY);
            }

            if (canvasPoint == null) {
                log.error("Could not determine canvas point for movement");
                return;
            }

            final Point finalCanvasPoint = canvasPoint;
            Microbot.getClientThread().invoke(() -> {
                try {
                    VirtualMouse virtualMouse = new VirtualMouse();
                    virtualMouse.click(finalCanvasPoint.x, finalCanvasPoint.y);
                    log.debug("Sent movement via virtual mouse to canvas point ({}, {})", finalCanvasPoint.x, finalCanvasPoint.y);
                } catch (Exception e) {
                    log.error("Error during mouse movement", e);
                }
            });
            
        } catch (Exception e) {
            log.error("Failed to send movement packet", e);
            throw new RuntimeException("Failed to send movement packet", e);
        }
    }

    private static Point getCanvasLocation(int sceneX, int sceneY) {
        Client client = Microbot.getClient();
        if (client == null) return null;

        // Get the canvas dimensions
        Canvas canvas = client.getCanvas();
        if (canvas == null) return null;

        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        // Calculate viewport center
        int centerX = canvasWidth / 2;
        int centerY = canvasHeight / 2;

        // Get local tile coordinates
        LocalPoint localPoint = LocalPoint.fromScene(sceneX, sceneY);
        if (localPoint == null) return null;

        // Adjust for tile size and viewport
        int x = centerX + (localPoint.getX() / TILE_SIZE);
        int y = centerY + (localPoint.getY() / TILE_SIZE);

        // Ensure point is within canvas bounds
        x = Math.max(0, Math.min(x, canvasWidth - 1));
        y = Math.max(0, Math.min(y, canvasHeight - 1));

        return new Point(x, y);
    }

    public static void sendWorldPointMovement(WorldPoint target) {
        try {
            if (Microbot.getClient() == null || target == null) {
                log.warn("Client or target is null, can't send world point movement");
                return;
            }
            
            // Convert world coordinates to scene coordinates
            Client client = Microbot.getClient();
            int baseX = client.getBaseX();
            int baseY = client.getBaseY();
            int sceneX = Math.floorMod(target.getX() - baseX, MAX_SCENE_SIZE);
            int sceneY = Math.floorMod(target.getY() - baseY, MAX_SCENE_SIZE);
            
            sendMovementPacket(sceneX, sceneY, false);
            
        } catch (Exception e) {
            log.error("Failed to send world point movement packet", e);
            throw new RuntimeException("Failed to send world point movement packet", e);
        }
    }
    
    public static LocalPoint worldToLocal(WorldPoint worldPoint) {
        if (Microbot.getClient() == null || worldPoint == null) return null;
        return LocalPoint.fromWorld(Microbot.getClient(), worldPoint);
    }

    public static boolean isValidSceneLocation(int sceneX, int sceneY) {
        return sceneX >= 0 && sceneY >= 0 && 
               sceneX < MAX_SCENE_SIZE && sceneY < MAX_SCENE_SIZE;
    }
}
