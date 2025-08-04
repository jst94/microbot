package net.runelite.client.plugins.microbot.jstvale;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.plugins.microbot.jstvale.enums.TotemLocation;

@ConfigGroup("JstVale")
public interface JstValeConfig extends Config {

    @ConfigSection(
            name = "General Settings",
            description = "General settings for Vale Totems",
            position = 0
    )
    String generalSection = "general";

    @ConfigSection(
            name = "Logs & Materials",
            description = "Log types and material settings",
            position = 1
    )
    String materialsSection = "materials";

    @ConfigSection(
            name = "Routes & Strategy",
            description = "Totem route and strategy settings",
            position = 2
    )
    String routeSection = "route";

    // General Settings
    @ConfigItem(
            keyName = "enableBreaks",
            name = "Enable Breaks",
            description = "Enable random breaks during the script",
            position = 0,
            section = generalSection
    )
    default boolean enableBreaks() {
        return true;
    }

    @ConfigItem(
            keyName = "useGraceful",
            name = "Use Graceful Outfit",
            description = "Wear graceful outfit for stamina efficiency",
            position = 1,
            section = generalSection
    )
    default boolean useGraceful() {
        return true;
    }

    @ConfigItem(
            keyName = "useLogBasket",
            name = "Use Log Basket",
            description = "Use log basket to carry more logs",
            position = 2,
            section = generalSection
    )
    default boolean useLogBasket() {
        return true;
    }

    @ConfigItem(
            keyName = "collectOfferings",
            name = "Collect Offerings",
            description = "Collect vale offerings during runs",
            position = 3,
            section = generalSection
    )
    default boolean collectOfferings() {
        return true;
    }

    // Materials Settings
    @ConfigItem(
            keyName = "logType",
            name = "Log Type",
            description = "Type of logs to use for totems",
            position = 0,
            section = materialsSection
    )
    default LogType logType() {
        return LogType.MAGIC_LOGS;
    }

    @ConfigItem(
            keyName = "fletchLogs",
            name = "Fletch Logs",
            description = "Fletch logs into longbows before decorating",
            position = 1,
            section = materialsSection
    )
    default boolean fletchLogs() {
        return true;
    }

    @ConfigItem(
            keyName = "logsPerRun",
            name = "Logs Per Run",
            description = "Number of logs to take per run (minimum 40 for full run)",
            position = 2,
            section = materialsSection
    )
    @Range(min = 40, max = 100)
    default int logsPerRun() {
        return 50;
    }

    // Route Settings
    @ConfigItem(
            keyName = "startingTotem",
            name = "Starting Totem",
            description = "Which totem to start the route from",
            position = 0,
            section = routeSection
    )
    default TotemLocation startingTotem() {
        return TotemLocation.TOTEM_1;
    }

    @ConfigItem(
            keyName = "clockwiseRoute",
            name = "Clockwise Route",
            description = "Go clockwise to maximize ent trail activations",
            position = 1,
            section = routeSection
    )
    default boolean clockwiseRoute() {
        return true;
    }

    @ConfigItem(
            keyName = "useAgilityShortcuts",
            name = "Use Agility Shortcuts",
            description = "Use agility shortcuts between totems",
            position = 2,
            section = routeSection
    )
    default boolean useAgilityShortcuts() {
        return true;
    }

    @ConfigItem(
            keyName = "minAgilityLevel",
            name = "Minimum Agility Level",
            description = "Minimum agility level for continuous running",
            position = 3,
            section = routeSection
    )
    @Range(min = 1, max = 99)
    default int minAgilityLevel() {
        return 70;
    }

    enum LogType {
        REGULAR_LOGS("Regular logs"),
        OAK_LOGS("Oak logs"),
        WILLOW_LOGS("Willow logs"),
        MAPLE_LOGS("Maple logs"),
        YEW_LOGS("Yew logs"),
        MAGIC_LOGS("Magic logs");

        private final String name;

        LogType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}