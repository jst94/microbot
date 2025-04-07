package net.runelite.client.plugins.microbot.multibox;

import net.runelite.client.config.Config;
import net.runelite.client.config.*;

@ConfigGroup("microbotMultibox")
public interface MultiboxConfig extends Config {

    enum ClientRole {
        MASTER,
        SLAVE
    }

    @ConfigItem(
            keyName = "clientRole",
            name = "Client Role",
            description = "Set whether this client is the Master or a Slave",
            position = 1
    )
    default ClientRole clientRole() {
        return ClientRole.SLAVE; // Default to Slave
    }

    @ConfigItem(
            keyName = "serverPort",
            name = "Server Port",
            description = "Port number for the master server to listen on / slaves to connect to",
            position = 2
    )
    default int serverPort() {
        return 61616; // Default port
    }

    @ConfigItem(
            keyName = "masterAddress",
            name = "Master Address",
            description = "IP address of the master client (only needed for slaves)",
            position = 3
    )
    default String masterAddress() {
        return "127.0.0.1"; // Default to localhost
    }
    @ConfigItem(
            keyName = "maxActionsPerTick",
            name = "Max Actions Per Tick",
            description = "Maximum number of actions processed per game tick on slave clients",
            position = 4
    )
    default int maxActionsPerTick() {
        return 3;
    }

    @ConfigItem(
            keyName = "maxRandomDelayMs",
            name = "Max Random Delay (ms)",
            description = "Maximum random delay in milliseconds before executing an action",
            position = 5
    )
    default int maxRandomDelayMs() {
        return 100;
    }

    @ConfigItem(
            keyName = "maxMouseJitter",
            name = "Max Mouse Jitter (pixels)",
            description = "Maximum random jitter in pixels added to mouse coordinates",
            position = 6
    )
    default int maxMouseJitter() {
        return 3;
    }
}