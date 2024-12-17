package net.runelite.client.plugins.microbot.tithefarm;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("microbottithefarm")
public interface TitheFarmConfig extends Config {
    @ConfigItem(
            keyName = "numberOfPlants",
            name = "Number of Plants",
            description = "Choose between 16 or 20 plants",
            position = 0
    )
    default int numberOfPlants() {
        return 20;
    }

    @ConfigItem(
            keyName = "enableDebug",
            name = "Enable Debug",
            description = "Enable debug logging",
            position = 1
    )
    default boolean enableDebug() {
        return false;
    }
}
