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
import net.runelite.api.Point; // Keep Point import
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry; // Keep MenuEntry import
import net.runelite.api.events.FocusChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.input.KeyListener; // Keep KeyListener import
import net.runelite.client.input.MouseListener; // Keep MouseListener import
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard; // Keep Keyboard utils
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget; // Keep Widget utils
import net.runelite.client.util.Text;
import java.awt.Rectangle; // Import Rectangle
import java.awt.event.KeyEvent; // Keep KeyEvent import
import java.awt.event.MouseEvent; // Keep MouseEvent import
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry; // Correct import path

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
            // Pass only host and port, message handler is removed
            multiboxClient = new MultiboxClient(config.masterAddress(), config.serverPort());
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

    @Subscribe
    public void onGameTick(GameTick event) {
        // Process one message per tick on slave clients
        if (config.clientRole() == MultiboxConfig.ClientRole.SLAVE && multiboxClient != null && multiboxClient.isRunning()) {
            String message = multiboxClient.pollMessage();
            if (message != null) {
                handleIncomingMessage(message); // Process the dequeued message
            }
        }
    }

    // Method to handle messages received by slave clients
    private void handleIncomingMessage(String message) {
        if (config.clientRole() != MultiboxConfig.ClientRole.SLAVE) return; // Only slaves process messages

        log.debug("Received message from master: {}", message);

        // Ensure actions interacting with Client or Microbot API run on the client thread
        // Use Callable<Void> for runOnClientThread
        Microbot.getClientThread().runOnClientThread(() -> {
            try {
                String[] parts = message.split(",", 7); // Max 7 parts needed now (INTERACT + 6 params)
                String messageType = parts[0];

                if (messageType.equals("INTERACT")) {
                    if (parts.length < 7) { // Check for 7 parts now
                        log.warn("Received malformed INTERACT message: {}", message);
                        return null; // Return null for Callable<Void>
                    }

                    String menuActionName = parts[1]; // Keep for logging/potential future use
                    String targetName = parts[2];
                    int identifier = Integer.parseInt(parts[3]);
                    String option = parts[4];
                    int param0 = Integer.parseInt(parts[5]); // Parse param0
                    int param1 = Integer.parseInt(parts[6]); // Parse param1

                    log.debug("Processing INTERACT: ActionName={}, Target={}, ID={}, Option={}, Param0={}, Param1={}",
                            menuActionName, targetName, identifier, option, param0, param1);

                    // Directly invoke the menu action using the received parameters
                    // We need the MenuAction enum type, not just its name.
                    MenuAction menuActionType;
                    try {
                        menuActionType = MenuAction.valueOf(menuActionName);
                    } catch (IllegalArgumentException e) {
                        log.error("Invalid MenuAction name received: {}", menuActionName);
                        return null; // Return null for Callable<Void>
                    }

                    // Construct a NewMenuEntry object
                    NewMenuEntry targetEntry = new NewMenuEntry(
                            option,
                            targetName,
                            identifier,
                            menuActionType, // Pass the MenuAction enum directly
                            param0,
                            param1,
                            true // isForceLeftClick = true (matches typical left-click behavior)
                    );

                    // Use Microbot.doInvoke with the NewMenuEntry and a dummy Rectangle
                    Microbot.doInvoke(targetEntry, new Rectangle(0, 0, 0, 0));

                    // Logging success
                    log.info("Invoked action via doInvoke: {}", message);

            } else if (messageType.equals("KEY_PRESS") || messageType.equals("KEY_RELEASE")) {
                 if (parts.length < 3) {
                    log.warn("Received malformed KEY message: {}", message);
                    return null; // Return null for Callable<Void>
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
            return null; // Return null at the end of the Callable<Void> lambda
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
                    int param0 = topEntry.getParam0(); // Get param0
                    int param1 = topEntry.getParam1(); // Get param1 (often packed widget ID)

                    // Create a structured message including param0 and param1
                    // Example: INTERACT,CC_OP,High Alchemy,1,Cast,0,12845056
                    String message = String.format("INTERACT,%s,%s,%d,%s,%d,%d",
                            type.name(),    // e.g., CC_OP
                            targetName,     // e.g., High Alchemy
                            identifier,     // e.g., 1 (menu index/action ID)
                            option,         // e.g., Cast
                            param0,         // e.g., 0
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