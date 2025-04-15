package net.runelite.client.plugins.microbot.multibox.packet;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.mouse.VirtualMouse;
import net.runelite.client.plugins.microbot.multibox.MultiboxConfig;
import net.runelite.api.Tile;
import net.runelite.api.GameObject;
import net.runelite.api.ObjectComposition;
import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class PacketHandler {
    private final Client client;
    private final MultiboxConfig config;
    private static final int MAX_SCENE_SIZE = 104;

    @Inject
    public PacketHandler(Client client, MultiboxConfig config) {
        this.client = client;
        this.config = config;
    }

    public void handleMovementPacket(MovementPacket packet) {
        if (client == null) {
            log.error("Client is null, cannot handle movement packet");
            return;
        }

        try {
            log.debug("Handling MovementPacket: {}", packet); // Log incoming packet
            // Handle scene coordinates directly if available
            if (packet.getSceneX() != -1 && packet.getSceneY() != -1) {
                log.debug("Packet contains scene coordinates ({}, {}).", packet.getSceneX(), packet.getSceneY());
                handleSceneMovement(packet.getSceneX(), packet.getSceneY(), packet.isCtrlDown());
                return;
            }

            log.debug("Packet contains world coordinates ({}, {}, {}). Converting to scene.", packet.getWorldX(), packet.getWorldY(), packet.getPlane());
            // Convert world coordinates to scene coordinates
            int baseX = client.getBaseX();
            int baseY = client.getBaseY();
            int sceneX = Math.floorMod(packet.getWorldX() - baseX, MAX_SCENE_SIZE);
            int sceneY = Math.floorMod(packet.getWorldY() - baseY, MAX_SCENE_SIZE);
            log.debug("Calculated scene coordinates: ({}, {}) from base ({}, {})", sceneX, sceneY, baseX, baseY);
            handleSceneMovement(sceneX, sceneY, packet.isCtrlDown());

        } catch (Exception e) {
            log.error("Error handling movement packet: {}", e.getMessage());
        }
    }

    private void handleSceneMovement(int sceneX, int sceneY, boolean ctrlDown) {
        log.debug("Handling scene movement for ({}, {}), ctrlDown={}", sceneX, sceneY, ctrlDown);
        // Convert scene coordinates to local
        LocalPoint localPoint = LocalPoint.fromScene(sceneX, sceneY);
        if (localPoint == null) {
            log.error("Could not convert scene coordinates ({}, {}) to local", sceneX, sceneY); // Added coords
            return;
        }

        log.debug("Converted scene to local: ({}, {})", localPoint.getX(), localPoint.getY());
        WorldPoint worldPoint = WorldPoint.fromLocal(client, localPoint);
        if (worldPoint == null) {
            log.error("Could not convert local coordinates ({}, {}) to world point", localPoint.getX(), localPoint.getY()); // Added coords
            return;
        }

        log.debug("Converted local to world: ({}, {}, {})", worldPoint.getX(), worldPoint.getY(), worldPoint.getPlane());
        // Handle movement based on config mode
        log.debug("Checking movement mode. Current mode: {}", config.movementMode());
        if (config.movementMode() == MultiboxConfig.MovementMode.MENU_ENTRY) {
            log.debug("Using MENU_ENTRY movement for world point ({}, {})", worldPoint.getX(), worldPoint.getY()); // Added log
            handleMenuEntryMovement(worldPoint);
            return;
        }

        // Default to virtual mouse movement
        log.debug("Using VIRTUAL_MOUSE movement for local point ({}, {})", localPoint.getX(), localPoint.getY()); // Added log
        Point canvasPoint = getCanvasLocation(localPoint);
        if (canvasPoint == null) {
            log.error("Could not determine click location for movement from local point ({}, {})", localPoint.getX(), localPoint.getY()); // Added coords
            return;
        }

        // Execute the movement on the client thread
        final Point clickPoint = canvasPoint;
        Microbot.getClientThread().invoke(() -> {
            try {
                log.debug("Executing client thread invoke for virtual mouse click."); // Added log
                VirtualMouse virtualMouse = new VirtualMouse();
                virtualMouse.click(clickPoint.getX(), clickPoint.getY());
                if (config.debugMode()) {
                    log.debug("Executed movement to canvas point ({}, {})", clickPoint.getX(), clickPoint.getY());
                }
            } catch (Exception e) {
                log.error("Error executing movement click", e);
            }
        });
    }

    private void handleMenuEntryMovement(WorldPoint worldPoint) {
        Microbot.getClientThread().invoke(() -> {
            try {
                log.debug("Executing client thread invoke for menu action."); // Added log
                // Use menuAction with correct signature
                client.menuAction(
                    worldPoint.getX(),      // p0 (worldX)
                    worldPoint.getY(),      // p1 (worldY)
                    MenuAction.WALK,        // action (enum constant)
                    0,                      // id
                    -1,                     // itemId
                    "Walk here",            // option
                    ""                      // target
                );
                if (config.debugMode()) {
                    log.debug("Executed menu entry movement to world point ({}, {})", 
                        worldPoint.getX(), worldPoint.getY());
                }
            } catch (Exception e) {
                log.error("Error executing menu entry movement", e);
            }
        });
    }

    private Point getCanvasLocation(LocalPoint localPoint) {
        // This part seems complex and potentially fragile. Let's add logging here too.
        if (localPoint == null) {
            log.debug("getCanvasLocation: localPoint is null");
            return null;
        }

        WorldPoint worldPoint = WorldPoint.fromLocal(client, localPoint);
        if (worldPoint != null) {
            log.debug("getCanvasLocation: Attempting conversion for localPoint ({}, {})", localPoint.getX(), localPoint.getY());
            LocalPoint validLocal = LocalPoint.fromWorld(client, worldPoint);
            if (validLocal != null) {
                int cameraX = client.getCameraX();
                int cameraY = client.getCameraY();
                int scale = client.getScale();
                log.debug("getCanvasLocation: worldPoint=({}, {}), validLocal=({}, {}), camera=({}, {}), scale={}",
                    worldPoint.getX(), worldPoint.getY(), validLocal.getX(), validLocal.getY(), cameraX, cameraY, scale);

                // Potential issue: Division by zero if scale is 0? Unlikely but possible.
                if (scale == 0) {
                    log.error("getCanvasLocation: Scale is zero, cannot calculate canvas location.");
                    return null;
                }

                int x = (validLocal.getX() - cameraX) / scale;
                int y = (validLocal.getY() - cameraY) / scale;

                log.debug("getCanvasLocation: Calculated canvas point ({}, {})", x, y);
                return new Point(x, y);
            } else {
                 log.debug("getCanvasLocation: Could not get validLocal from worldPoint ({}, {})", worldPoint.getX(), worldPoint.getY());
            }
        } else {
             log.debug("getCanvasLocation: Could not get worldPoint from localPoint ({}, {})", localPoint.getX(), localPoint.getY());
        }
        return null;
    }

    // New method to handle interaction packets
    public void handleInteractionPacket(InteractionPacket packet) {
        if (client == null) {
            log.error("Client is null, cannot handle interaction packet");
            return;
        }

        final InteractionPacket finalPacket;
        String objectType = packet.getObjectType();
        if (objectType != null && !objectType.isEmpty() && objectType.equalsIgnoreCase("tree")) {
            // Find a nearby tree
            WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
            if (playerLocation == null) {
                log.error("Could not get player location");
                return;
            }

            int searchRadius = 5; // Adjust as needed
            Tile[][][] tiles = client.getScene().getTiles();
            java.util.List<GameObject> nearbyTrees = new java.util.ArrayList<>();
            for (int z = 0; z < tiles.length; z++) {
                for (int x = 0; x < tiles[z].length; x++) {
                    for (int y = 0; y < tiles[z][x].length; y++) {
                        Tile tile = tiles[z][x][y];
                        if (tile != null) {
                            GameObject[] gameObjects = tile.getGameObjects();
                            if (gameObjects != null) {
                                for (GameObject gameObject : gameObjects) {
                                    if (gameObject != null) {
                                        ObjectComposition objectComposition = client.getObjectDefinition(gameObject.getId());
                                        if (objectComposition != null) {
                                            String name = objectComposition.getName();
                                            if (name != null && name.equalsIgnoreCase("tree")) {
                                                LocalPoint localPoint = gameObject.getLocalLocation();
                                                if (localPoint != null) {
                                                    int sceneX = localPoint.getSceneX();
                                                    int sceneY = localPoint.getSceneY();
                                                    WorldPoint objectLocation = WorldPoint.fromScene(client, sceneX, sceneY, client.getPlane());
                                                    if (objectLocation != null && playerLocation.distanceTo(objectLocation) <= searchRadius) {
                                                        log.debug("Found nearby tree: {} at {}", name, objectLocation);
                                                        nearbyTrees.add(gameObject);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!nearbyTrees.isEmpty()) {
                // Choose a random tree
                int randomIndex = (int) (Math.random() * nearbyTrees.size());
                GameObject selectedTree = nearbyTrees.get(randomIndex);
                int newTreeId = selectedTree.getId();

                log.debug("Selected random tree with ID: {}", newTreeId);

                // Update the packet with the new tree ID
                finalPacket = new InteractionPacket(packet.getParam0(), packet.getParam1(), newTreeId, packet.getMenuAction(), packet.getOption(), packet.getTarget(), packet.getObjectType());

            } else {
                finalPacket = packet;
            }
            log.debug("Found a tree interaction, but dynamic tree selection is not yet fully implemented.");
        } else {
            finalPacket = packet;
        }

        log.debug("Handling InteractionPacket: {}", finalPacket);
        try {
            Microbot.getClientThread().invoke(() -> {
                try {
                    log.debug("Invoking menuAction with params: param0={}, param1={}, menuAction={}, id={}, option={}, target={}",
                        finalPacket.getParam0(), finalPacket.getParam1(), finalPacket.getMenuAction(), finalPacket.getId(), finalPacket.getOption(), finalPacket.getTarget());
                    client.menuAction(
                        finalPacket.getParam0(),
                        finalPacket.getParam1(),
                        finalPacket.getMenuAction(), // Use the reconstructed MenuAction enum
                        finalPacket.getId(),
                        -1, // itemId is often -1 for non-item interactions, adjust if needed later
                        finalPacket.getOption(),
                        finalPacket.getTarget()
                    );
                    if (config.debugMode()) {
                        log.debug("Executed menu action: {}", finalPacket);
                    }
                } catch (Exception e) {
                    log.error("Error executing menu action from packet", e);
                }
            });
        } catch (Exception e) {
            log.error("Error handling interaction packet: {}", e.getMessage());
        }
    }
}
