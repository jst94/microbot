package net.runelite.client.plugins.microbot.AutoBankSkillerPlugin;

import net.runelite.client.config.*;

@ConfigGroup("AutoBankSkillerPluginConfig")
public interface AutoBankSkillerPluginConfig extends Config {

    @ConfigItem(
            keyName = "startHotkey",
            name = "Start hotkey",
            description = "Hotkey to start the plugin",
            position = 1
    )
    default Keybind startHotkey() {
        return null;
    }

    @ConfigItem(
            keyName = "skillType",
            name = "Skill Type",
            description = "Choose the type of skilling activity",
            position = 2
    )
    default String skillType() {
        return "HERB_CLEANING";
    }

    @ConfigSection(
            name = "Herb cleaning settings",
            description = "Herb cleaning settings",
            position = 3,
            closedByDefault = true
    )
    String herbCleaningSettings = "Herb cleaning settings";

    @ConfigItem(
            keyName = "grimyHerbs",
            name = "Grimy herbs",
            description = "Grimy herbs' names to clean, separated by new line",
            position = 1,
            section = herbCleaningSettings
    )
    default String grimyHerbs() {
        return "Grimy guam leaf\nGrimy ranarr weed";
    }

    @ConfigItem(
            keyName = "cleanHerbSleepMin",
            name = "Min sleep time",
            description = "Min sleep time between cleaning herbs",
            position = 1,
            section = herbCleaningSettings
    )
    default int cleanHerbSleepMin() {
        return 50;
    }

    @ConfigItem(
            keyName = "cleanHerbSleepMax",
            name = "Max sleep time",
            description = "Max sleep time between cleaning herbs",
            position = 2,
            section = herbCleaningSettings
    )
    default int cleanHerbSleepMax() {
        return 100;
    }

    @ConfigSection(
            name = "Gem cutting settings",
            description = "Gem cutting settings",
            position = 4,
            closedByDefault = true
    )
    String gemCuttingSettings = "Gem cutting settings";

    @ConfigItem(
            keyName = "uncutGems",
            name = "Uncut gems",
            description = "Uncut gems' names to cut, separated by new line",
            position = 1,
            section = gemCuttingSettings
    )
    default String uncutGems() {
        return "Uncut sapphire\nUncut diamond";
    }

    @ConfigSection(
            name = "Potion making settings",
            description = "Potions settings",
            position = 5,
            closedByDefault = true
    )
    String potionSettings = "potionSettings";

    @ConfigItem(
            keyName = "potionType",
            name = "Potion Type",
            description = "Type of potion to make",
            position = 1,
            section = potionSettings
    )
    default String potionType() {
        return "GUAM_POTION";
    }

    @ConfigSection(
            name = "Lunar spell settings",
            description = "Lunar spell settings",
            position = 6,
            closedByDefault = true
    )
    String lunarSpellSettings = "lunarSpellSettings";

    @ConfigItem(
            keyName = "lunarSpellType",
            name = "Spell Type",
            description = "Type of lunar spell to cast",
            position = 1,
            section = lunarSpellSettings
    )
    default String lunarSpellType() {
        return "REGULAR_PLANK";
    }

    @ConfigSection(
            name = "Crafting settings",
            description = "Crafting settings",
            position = 7,
            closedByDefault = true
    )
    String craftingSettings = "craftingSettings";

    @ConfigItem(
            keyName = "craftingType",
            name = "Crafting Type",
            description = "Type of crafting to do",
            position = 1,
            section = craftingSettings
    )
    default String craftingType() {
        return "WATER_BATTLESTAFF";
    }

    @ConfigSection(
            name = "Fletching settings",
            description = "Fletching settings",
            position = 8,
            closedByDefault = true
    )
    String fletchingSettings = "fletchingSettings";

    @ConfigItem(
            keyName = "fletchingType",
            name = "Fletching Type",
            description = "Type of fletching to do",
            position = 1,
            section = fletchingSettings
    )
    default String fletchingType() {
        return "ARROW_SHAFTS";
    }

    @ConfigSection(
            name = "Spam combine settings",
            description = "Spam combine settings (e.g. darts, bolts)",
            position = 9,
            closedByDefault = true
    )
    String spamCombineSettings = "spamCombineSettings";

    @ConfigItem(
            keyName = "spamCombineFirstItem",
            name = "First item",
            description = "First item's name or ID",
            position = 1,
            section = spamCombineSettings
    )
    default String spamCombineFirstItem() {
        return "Adamant dart tip";
    }

    @ConfigItem(
            keyName = "spamCombineSecondItem",
            name = "Second item",
            description = "First item's name or ID",
            position = 2,
            section = spamCombineSettings
    )
    default String spamCombineSecondItem() {
        return "Feather";
    }

    @ConfigItem(
            keyName = "waitForAnimation",
            name = "Wait for animation",
            description = "Wait for animation to finish before combining items again (e.g. arrows)",
            position = 3,
            section = spamCombineSettings
    )
    default boolean waitForAnimation() {
        return true;
    }

    @ConfigItem(
            keyName = "spamCombineSleepMin",
            name = "Min sleep time",
            description = "Min sleep time between combine clicks",
            position = 4,
            section = spamCombineSettings
    )
    default int spamCombineSleepMin() {
        return 50;
    }

    @ConfigItem(
            keyName = "spamCombineSleepMax",
            name = "Max sleep time",
            description = "Max sleep time between combine clicks",
            position = 5,
            section = spamCombineSettings
    )
    default int spamCombineSleepMax() {
        return 100;
    }

    @ConfigSection(
            name = "Custom item on item settings",
            description = "Custom item on item settings",
            position = 10,
            closedByDefault = true
    )
    String customItemOnItemSettings = "customItemOnItemSettings";

    @ConfigItem(
            keyName = "customFirstItem",
            name = "First item",
            description = "First item's name",
            position = 1,
            section = customItemOnItemSettings
    )
    default String customFirstItem() {
        return "Pestle and mortar";
    }

    @ConfigItem(
            keyName = "customFirstItemCount",
            name = "First item count",
            description = "First item's withdraw count",
            position = 2,
            section = customItemOnItemSettings
    )

    default int customFirstItemCount() {
        return 1;
    }

    @ConfigItem(
            keyName = "customSecondItem",
            name = "Second item",
            description = "Second item's name",
            position = 3,
            section = customItemOnItemSettings
    )
    default String customSecondItem() {
        return "Unicorn horn";
    }

    @ConfigItem(
            keyName = "customSecondItemCount",
            name = "Second item count",
            description = "Second item's withdraw count",
            position = 4,
            section = customItemOnItemSettings
    )

    default int customSecondItemCount() {
        return 27;
    }

    @ConfigItem(
            keyName = "customDontDepositFirstItem",
            name = "Don't deposit first item",
            description = "Don't deposit first item. E.g. always keep pestle and mortar in inventory",
            position = 5,
            section = customItemOnItemSettings
    )
    default boolean customDontDepositFirstItem() {
        return false;
    }

}
