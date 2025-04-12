package net.runelite.client.plugins.microbot.multibox;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("microbotmultibox")
public interface MultiboxConfig extends Config {
    enum ClientRole {
        MASTER,
        SLAVE
    }

    enum MovementMode {
        VIRTUAL_MOUSE,
        MENU_ENTRY
    }

    @ConfigItem(
        keyName = "clientRole",
        name = "Client Role",
        description = "Whether this client is the master or a slave",
        position = 1
    )
    default ClientRole clientRole() {
        return ClientRole.SLAVE;
    }

    @ConfigItem(
        keyName = "masterAddress",
        name = "Master Address",
        description = "IP address of the master client (for slaves)",
        position = 2
    )
    default String masterAddress() {
        return "127.0.0.1";
    }

    @Range(
        min = 1024,
        max = 65535
    )
    @ConfigItem(
        keyName = "serverPort",
        name = "Server Port",
        description = "Port to use for multibox communication",
        position = 3
    )
    default int serverPort() {
        return 43594;
    }

    @ConfigItem(
        keyName = "movementMode",
        name = "Movement Mode",
        description = "How to handle movement commands",
        position = 4
    )
    default MovementMode movementMode() {
        return MovementMode.VIRTUAL_MOUSE;
    }

    @ConfigItem(
        keyName = "autoReconnect",
        name = "Auto Reconnect",
        description = "Automatically try to reconnect if connection is lost",
        position = 5
    )
    default boolean autoReconnect() {
        return true;
    }

    @Range(
        min = 1000,
        max = 30000
    )
    @ConfigItem(
        keyName = "reconnectDelay",
        name = "Reconnect Delay (ms)",
        description = "Delay between reconnection attempts in milliseconds",
        position = 6
    )
    default int reconnectDelay() {
        return 5000;
    }

    @ConfigItem(
        keyName = "debugMode",
        name = "Debug Mode",
        description = "Enable detailed debug logging",
        position = 7
    )
    default boolean debugMode() {
        return false;
    }
}
