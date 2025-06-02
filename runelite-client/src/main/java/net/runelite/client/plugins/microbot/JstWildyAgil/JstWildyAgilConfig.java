package net.runelite.client.plugins.microbot.JstWildyAgil;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("jstwildyagil")
public interface JstWildyAgilConfig extends Config {

    @ConfigSection(
            name = "General",
            description = "General settings",
            position = 0,
            closedByDefault = false
    )
    String generalSection = "general";

    @ConfigItem(
            keyName = "guide",
            name = "Script Guide",
            description = "Guide for the Wilderness Agility script",
            position = 0,
            section = generalSection
    )
    default String guide() {
        return "JST WildyAgil - Wilderness Agility Course & Token Handler";
    }

    @ConfigItem(
            keyName = "handleTokens",
            name = "Handle Agility Tickets",
            description = "Enable to hand in Agility Arena Tickets",
            position = 1,
            section = generalSection
    )
    default boolean handleTokens() {
        return true;
    }

    @ConfigSection(
            name = "Food & Banking",
            description = "Settings for food and banking",
            position = 1,
            closedByDefault = false
    )
    String foodBankingSection = "foodBanking";

    @ConfigItem(
            keyName = "useFood",
            name = "Use Food",
            description = "Enable to use food if health is low.",
            position = 2,
            section = foodBankingSection
    )
    default boolean useFood() {
        return false;
    }

    @ConfigItem(
            keyName = "foodName",
            name = "Food Name",
            description = "Name of the food to eat/withdraw (e.g., Monkfish).",
            position = 3,
            section = foodBankingSection
    )
    default String foodName() {
        return "Monkfish";
    }

    @ConfigItem(
            keyName = "foodAmount",
            name = "Food Amount to Withdraw",
            description = "How much food to withdraw from the bank.",
            position = 4,
            section = foodBankingSection
    )
    default int foodAmount() {
        return 5;
    }


    @ConfigItem(
            keyName = "minHealthEat",
            name = "Min Health to Eat (%)",
            description = "Minimum health percentage before eating food (if available).",
            position = 5,
            section = foodBankingSection
    )
    default int minHealthEat() {
        return 60;
    }

    // Voeg hier meer configuratie opties toe als nodig
    // Bijvoorbeeld: stamina potion usage, anti-pk measures etc.
}