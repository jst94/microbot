package net.runelite.client.plugins.microbot.AutoBankSkillerPlugin;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("autobankskiller")
public interface AutoBankSkillerConfig extends Config {
    enum SkillType {
        FLETCHING,
        CRAFTING,
        HERBLORE
    }

    enum FletchingType {
        CUT_LOGS,
        STRING_BOW
    }

    @ConfigItem(
        keyName = "skillType",
        name = "Skill Type",
        description = "The type of skilling to perform",
        position = 0
    )
    default SkillType skillType() {
        return SkillType.FLETCHING;
    }

    @ConfigItem(
        keyName = "fletchingType",
        name = "Fletching Type",
        description = "The type of fletching to perform",
        position = 1
    )
    default FletchingType fletchingType() {
        return FletchingType.CUT_LOGS;
    }

    @ConfigItem(
        keyName = "itemId",
        name = "Primary Item ID",
        description = "ID of the primary item to use (logs for fletching)",
        position = 2
    )
    default int itemId() {
        return 0;
    }

    @ConfigItem(
        keyName = "secondaryItemId",
        name = "Secondary Item ID",
        description = "ID of the secondary item to use (knife/bowstring for fletching)",
        position = 3
    )
    default int secondaryItemId() {
        return 0;
    }

    @ConfigItem(
        keyName = "option",
        name = "Menu Option",
        description = "Which option to choose in the menu (1-5)",
        position = 4
    )
    default int option() {
        return 1;
    }
}
