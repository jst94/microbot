package net.runelite.client.plugins.microbot.varlamoreranged;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("varlamoreranged")
public interface VarlamoreRangedConfig extends Config {
    @ConfigItem(
            keyName = "guide",
            name = "How to use",
            description = "How to use this plugin",
            position = 0
    )
    default String GUIDE() {
        return "1. Start with a fresh account that has completed Tutorial Island\n" +
                "2. The bot will walk to Grand Exchange and wait for trade/coins\n" +
                "3. Give the account at least 15,000,000 coins for bond + equipment\n" +
                "4. The bot will buy and redeem a bond for membership\n" +
                "5. The bot will buy Oak shortbow and Bronze arrows\n" +
                "6. It will complete the 'Children of the Sun' quest (members only)\n" +
                "7. Finally train ranged to configured level at Varlamore chickens\n\n" +
                "Make sure you have a stable internet connection!";
    }
    
    @ConfigItem(
            keyName = "stopAtLevel",
            name = "Stop at Level",
            description = "Stop training when reaching this ranged level",
            position = 1
    )
    default int stopAtLevel() {
        return 20;
    }
}