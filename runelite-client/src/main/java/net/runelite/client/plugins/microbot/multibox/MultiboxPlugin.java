package net.runelite.client.plugins.microbot.multibox;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.api.Client;
import net.runelite.api.Point; // Import Point for coordinates
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.FocusChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.input.KeyListener; // Import KeyListener
import net.runelite.client.input.MouseListener;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard; // Import Keyboard utils
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget; // Import Widget utils
import net.runelite.client.util.Text;
import java.awt.event.KeyEvent; // Import KeyEvent
import java.awt.event.MouseEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@PluginDescriptor(
    name = "Microbot Multibox",
    description = "Microbot Multibox Plugin",
    tags = {"microbot", "multibox"},
    enabledByDefault = false,
    configName = "MultiboxConfig" // Link to the config interface
)
@Slf4j
public class MultiboxPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private PluginManager pluginManager;

    @Inject
    private MouseManager mouseManager;

    @Inject
    private KeyManager keyManager;

    @Inject
    private MultiboxConfig config;

    @Provides
    MultiboxConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(MultiboxConfig.class);
    }

    private ExecutorService executorService;
    private MultiboxServer server;
    private MultiboxClient multiboxClient;

    @Override
    protected void startUp() throws Exception {
        log.info("Multibox Plugin starting!");
        executorService = Executors.newSingleThreadExecutor();

        if (config.clientRole() == MultiboxConfig.ClientRole.MASTER) {
            log.info("Starting as MASTER on port {}", config.serverPort());
            server = new MultiboxServer(config.serverPort());
            executorService.submit(server);
            mouseManager.registerMouseListener(masterMouseListener);
            keyManager.registerKeyListener(masterKeyListener); // Register key listener
        } else {
            log.info("Starting as SLAVE, connecting to {}:{}", config.masterAddress(), config.serverPort());
            multiboxClient = new MultiboxClient(config.masterAddress(), config.serverPort(), this::handleIncomingMessage);
            executorService.submit(multiboxClient);
        }
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Multibox Plugin stopping!");
        if (server != null && server.isRunning()) {
            server.stop();
        }
        if (multiboxClient != null && multiboxClient.isRunning()) {
            multiboxClient.stop();
        }
        // Unregister listeners
        if (config.clientRole() == MultiboxConfig.ClientRole.MASTER) {
            mouseManager.unregisterMouseListener(masterMouseListener);
            keyManager.unregisterKeyListener(masterKeyListener); // Unregister key listener
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }
        server = null;
        multiboxClient = null;
        executorService = null;
        log.info("Multibox Plugin stopped!");
    }

    // Method to handle messages received by slave clients
    private void handleIncomingMessage(String message) {
        if (config.clientRole() != MultiboxConfig.ClientRole.SLAVE) return; // Only slaves process messages

        log.debug("Received message from master: {}", message);

        // Ensure actions interacting with Client or Microbot API run on the client thread
        Microbot.getClientThread().runOnClientThread(() -> {
            try {
                String[] parts = message.split(",", 6); // Max 6 parts needed for INTERACT (including param1)
                String messageType = parts[0];

                if (messageType.equals("INTERACT")) {
                    if (parts.length < 6) { // Check for 6 parts now
                        log.warn("Received malformed INTERACT message: {}", message);
                        return null;
                    }

                    String menuActionName = parts[1];
                    String targetName = parts[2];
                    int identifier = Integer.parseInt(parts[3]);
                    String option = parts[4];
                    int param1 = Integer.parseInt(parts[5]); // Parse param1 (packed widget ID)

                log.debug("Processing INTERACT: Action={}, Target={}, ID={}, Option={}, Param1={}", menuActionName, targetName, identifier, option, param1);

                boolean interacted = false;

                // Determine interaction type based on MenuAction name prefix
                if (menuActionName.startsWith("GAME_OBJECT")) {
                    interacted = Rs2GameObject.interact(identifier, option);
                    if (!interacted) { // Fallback to name if ID interaction fails
                        interacted = Rs2GameObject.interact(targetName, option);
                    }
                    log.info("Attempted GameObject interaction: ID={}, Name={}, Option={}, Success={}", identifier, targetName, option, interacted);
                } else if (menuActionName.startsWith("NPC")) {
                    interacted = Rs2Npc.interact(identifier, option);
                    if (!interacted) { // Fallback to name
                       interacted = Rs2Npc.interact(targetName, option);
                    }
                     log.info("Attempted NPC interaction: ID={}, Name={}, Option={}, Success={}", identifier, targetName, option, interacted);
                } else if (menuActionName.startsWith("ITEM_") || menuActionName.startsWith("CC_OP")) { // Inventory items
                     // CC_OP is often used for item actions in inventory
                    interacted = Rs2Inventory.interact(identifier, option);
                     if (!interacted) { // Fallback to name
                        interacted = Rs2Inventory.interact(targetName, option);
                     }
                     log.info("Attempted Inventory Item interaction: ID={}, Name={}, Option={}, Success={}", identifier, targetName, option, interacted);
                } else if (menuActionName.startsWith("GROUND_ITEM")) {
                    // Ground items often use location hash in identifier, ID might be less reliable
                    // Let's prioritize name/option for ground items if possible, or use interact(id, option)
                    interacted = Rs2GroundItem.interact(targetName, option); // Prioritize name for ground items
                    if (!interacted) {
                         interacted = Rs2GroundItem.interact(identifier, option, 15); // Fallback to ID with default range
                    }
                    log.info("Attempted Ground Item interaction: ID={}, Name={}, Option={}, Success={}", identifier, targetName, option, interacted);
                } else if (menuActionName.startsWith("WIDGET") || menuActionName.equals("CC_OP")) {
                    // Use clickWidgetFast with param1 (packed ID) and identifier (menu index/action ID)
                    Rs2Widget.clickWidgetFast(param1, identifier);
                    interacted = true; // Assume success for now, Rs2Widget doesn't return boolean here
                    log.info("Attempted Widget interaction: PackedID={}, Identifier={}, Option={}", param1, identifier, option);
                } else {
                    log.warn("Unhandled MenuAction type prefix: {}", menuActionName);
                }

                if (!interacted) {
                    log.warn("Failed to execute interaction: {}", message);
                }
            } else if (messageType.equals("KEY_PRESS") || messageType.equals("KEY_RELEASE")) {
                 if (parts.length < 3) {
                    log.warn("Received malformed KEY message: {}", message);
                    return null;
                 }
                 int keyCode = Integer.parseInt(parts[1]);
                 // char keyChar = parts[2].isEmpty() ? KeyEvent.CHAR_UNDEFINED : parts[2].charAt(0); // KeyChar might not be reliable across systems/locales

                 if (messageType.equals("KEY_PRESS")) {
                     Rs2Keyboard.keyHold(keyCode); // Use keyHold for press
                     log.debug("Simulated KEY_PRESS (hold): Code={}", keyCode);
                 } else { // KEY_RELEASE
                     Rs2Keyboard.keyRelease(keyCode); // Use keyRelease for release
                     log.debug("Simulated KEY_RELEASE: Code={}", keyCode);
                 }

            } else {
                 log.warn("Received unknown message type: {}", messageType);
            }

            } catch (NumberFormatException e) {
                log.error("Error parsing number from message: {}", message, e);
            } catch (Exception e) {
                log.error("Error executing interaction for message '{}': {}", message, e.getMessage(), e);
            }
            return null;
        });
    }

    // Master specific logic for broadcasting events
    private void broadcastAction(String actionMessage) {
        if (server != null && server.isRunning() && config.clientRole() == MultiboxConfig.ClientRole.MASTER) {
            log.debug("Broadcasting: {}", actionMessage);
            server.broadcast(actionMessage);
        }
    }

    // Listener for Master client mouse events
    private final MouseListener masterMouseListener = new MouseListener() {
        @Override
        public MouseEvent mousePressed(MouseEvent event) {
            // We capture on press because menu entries are generated before release/click
            if (event.getSource() == client.getCanvas() && event.getButton() == MouseEvent.BUTTON1) { // Handle left-clicks for now
                MenuEntry[] menuEntries = client.getMenuEntries();
                if (menuEntries.length > 0) {
                    // The top entry is usually the one that will be executed on click
                    MenuEntry topEntry = menuEntries[menuEntries.length - 1];
                    String targetName = Text.removeTags(topEntry.getTarget()); // Clean target name
                    int identifier = topEntry.getIdentifier(); // Usually the ID
                    String option = topEntry.getOption();
                    MenuAction type = topEntry.getType();
                    int param1 = topEntry.getParam1(); // Get param1 (often packed widget ID)

                    // Create a structured message including param1
                    // Example: INTERACT,CC_OP,High Alchemy,1,Cast,12845056
                    String message = String.format("INTERACT,%s,%s,%d,%s,%d",
                            type.name(),    // e.g., CC_OP
                            targetName,     // e.g., High Alchemy
                            identifier,     // e.g., 1 (menu index/action ID)
                            option,         // e.g., Cast
                            param1);        // e.g., 12845056 (packed widget ID)
                    broadcastAction(message);
                    // Optional: Consume event if needed, but might interfere with normal client operation
                    // event.consume();
                }
            }
            // Return original event even if we broadcast, let the client handle the actual click
            return event;
        }

        // mouseClicked is no longer needed for broadcasting the primary action
        @Override
        public MouseEvent mouseClicked(MouseEvent event) { return event; }

        @Override
        public MouseEvent mouseReleased(MouseEvent event) {
            // Could potentially broadcast right-click menu selections here if needed later
            return event;
        }

        @Override
        public MouseEvent mouseEntered(MouseEvent event) { return event; } // Don't forward enter/exit/drag/move for now

        @Override
        public MouseEvent mouseExited(MouseEvent event) { return event; }

        @Override
        public MouseEvent mouseDragged(MouseEvent event) { return event; }

        @Override
        public MouseEvent mouseMoved(MouseEvent event) { return event; }
        };

    // Listener for Master client key events
    private final KeyListener masterKeyListener = new KeyListener() {
        @Override
        public void keyTyped(KeyEvent e) {
            // Generally not used for game actions, but could broadcast if needed
            // String message = String.format("KEY_TYPED,%d,%c", e.getKeyCode(), e.getKeyChar());
            // broadcastAction(message);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            // Only forward key presses when the game canvas has focus (or maybe always?)
            // Consider if broadcasting should happen even if focus is elsewhere (e.g., chat)
             if (client.getCanvas().hasFocus()) { // Example: only broadcast if canvas focused
                String message = String.format("KEY_PRESS,%d,%c", e.getKeyCode(), e.getKeyChar());
                broadcastAction(message);
                // Optional: Consume event
                // e.consume();
             }
        }

        @Override
        public void keyReleased(KeyEvent e) {
             if (client.getCanvas().hasFocus()) { // Example: only broadcast if canvas focused
                String message = String.format("KEY_RELEASE,%d,%c", e.getKeyCode(), e.getKeyChar());
                broadcastAction(message);
                // Optional: Consume event
                // e.consume();
             }
        }
    };
// Remove extra brace from line 210
} // Add missing closing brace for the MultiboxPlugin class