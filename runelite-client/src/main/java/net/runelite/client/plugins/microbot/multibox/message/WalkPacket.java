package net.runelite.client.plugins.microbot.multibox.message;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;

import java.awt.Point;

@Getter
@Setter
public class WalkPacket extends BaseMessage {
    private int sceneX;
    private int sceneY;
    private boolean ctrlDown;
    private boolean useVirtualMouse;
    private Point canvasPoint;
    private transient String errorMessage; // Not serialized, used for validation

    public WalkPacket(int sceneX, int sceneY, boolean ctrlDown) {
        super(MessageType.WALK_PACKET);
        this.sceneX = sceneX;
        this.sceneY = sceneY;
        this.ctrlDown = ctrlDown;
        this.useVirtualMouse = false;
        validateCoordinates();
    }

    // Constructor for virtual mouse method
    public WalkPacket(int sceneX, int sceneY, Point canvasPoint, boolean ctrlDown) {
        this(sceneX, sceneY, ctrlDown);
        this.canvasPoint = canvasPoint;
        this.useVirtualMouse = true;
    }

    // Factory method for world point conversion
    public static WalkPacket fromWorldPoint(WorldPoint worldPoint, boolean ctrlDown) {
        if (worldPoint == null || Microbot.getClient() == null) {
            return null;
        }

        Client client = Microbot.getClient();
        int sceneX = worldPoint.getX() - client.getBaseX();
        int sceneY = worldPoint.getY() - client.getBaseY();
        
        WalkPacket packet = new WalkPacket(sceneX, sceneY, ctrlDown);
        
        // Try to get canvas point for virtual mouse
        if (client.getCanvas() != null) {
            Point canvasPoint = client.getCanvas().getMousePosition();
            if (canvasPoint != null) {
                packet.setCanvasPoint(canvasPoint);
                packet.setUseVirtualMouse(true);
            }
        }
        
        return packet;
    }

    private void validateCoordinates() {
        if (sceneX < 0 || sceneY < 0) {
            errorMessage = "Scene coordinates cannot be negative";
            return;
        }

        Client client = Microbot.getClient();
        if (client == null || client.getScene() == null) {
            errorMessage = "Client or scene is null";
            return;
        }

        if (sceneX >= client.getScene().getTiles()[0].length || 
            sceneY >= client.getScene().getTiles().length) {
            errorMessage = String.format("Scene coordinates (%d, %d) out of bounds", sceneX, sceneY);
            return;
        }

        errorMessage = null;
    }

    public boolean isValid() {
        return errorMessage == null;
    }

    @Override
    public String toString() {
        return String.format("WalkPacket(sceneX=%d, sceneY=%d, useVirtualMouse=%s, hasCanvasPoint=%s)", 
            sceneX, sceneY, useVirtualMouse, canvasPoint != null);
    }
}
