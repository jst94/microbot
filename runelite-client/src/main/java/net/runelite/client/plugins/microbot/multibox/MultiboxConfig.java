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
}