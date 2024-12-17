package net.runelite.client.plugins.microbot.jsttithe;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("jsttithe")
public interface JstTitheConfig extends Config {
    @ConfigItem(
            keyName = "numberOfPlants",
            name = "Number of Plants",
            description = "Number of plants to maintain (max 20)",
            position = 0
    )
    @Range(min = 1, max = 20)
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

    @ConfigItem(
            keyName = "wateringInterval",
            name = "Watering Interval (ms)",
            description = "Time between watering plants in milliseconds",
            position = 2
    )
    @Range(min = 20000, max = 60000)
    default int wateringInterval() {
        return 30000;
    }

    @ConfigItem(
            keyName = "useClosestPatches",
            name = "Use Closest Patches",
            description = "Start planting from patches closest to water barrel",
            position = 3
    )
    default boolean useClosestPatches() {
        return true;
    }

    @ConfigItem(
            keyName = "waitForGrowth",
            name = "Wait For Growth",
            description = "Wait for plants to fully grow before harvesting",
            position = 4
    )
    default boolean waitForGrowth() {
        return true;
    }

    @ConfigItem(
            keyName = "refillThreshold",
            name = "Refill Threshold",
            description = "Number of empty watering cans before refilling",
            position = 5
    )
    @Range(min = 1, max = 8)
    default int refillThreshold() {
        return 4;
    }

    @ConfigItem(
            keyName = "randomizePatches",
            name = "Randomize Patches",
            description = "Randomize the order of patches to plant in",
            position = 6
    )
    default boolean randomizePatches() {
        return false;
    }

    @ConfigItem(
            keyName = "sleepMin",
            name = "Min Sleep Time (ms)",
            description = "Minimum time to sleep between actions",
            position = 7
    )
    @Range(min = 300, max = 2000)
    default int sleepMin() {
        return 600;
    }

    @ConfigItem(
            keyName = "sleepMax",
            name = "Max Sleep Time (ms)",
            description = "Maximum time to sleep between actions",
            position = 8
    )
    @Range(min = 600, max = 3000)
    default int sleepMax() {
        return 1200;
    }
}
