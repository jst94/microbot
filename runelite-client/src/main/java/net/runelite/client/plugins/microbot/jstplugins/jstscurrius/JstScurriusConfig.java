package net.runelite.client.plugins.microbot.jstplugins.jstscurrius;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("JstScurrius")
public interface JstScurriusConfig extends Config {
    @ConfigItem(
        keyName = "foodSelection",
        name = "Food Selection",
        description = "Select the type of food to use",
        position = 1
    )
    default FoodType foodSelection() {
        return FoodType.SHARK;
    }

    @Range(
        min = 1,
        max = 28
    )
    @ConfigItem(
        keyName = "foodAmount",
        name = "Food Amount",
        description = "Amount of food to withdraw",
        position = 2
    )
    default int foodAmount() {
        return 10;
    }

    @ConfigItem(
        keyName = "potionSelection",
        name = "Prayer Potion Selection",
        description = "Select the type of prayer potion to use",
        position = 3
    )
    default PotionType potionSelection() {
        return PotionType.PRAYER_POTION;
    }

    @Range(
        min = 1,
        max = 28
    )
    @ConfigItem(
        keyName = "prayerPotionAmount",
        name = "Prayer Potion Amount",
        description = "Amount of prayer potions to withdraw",
        position = 4
    )
    default int prayerPotionAmount() {
        return 4;
    }

    @Range(
        min = 1,
        max = 99
    )
    @ConfigItem(
        keyName = "minEatPercent",
        name = "Min Eat %",
        description = "Minimum health percentage to eat at",
        position = 5
    )
    default int minEatPercent() {
        return 50;
    }

    @Range(
        min = 1,
        max = 99
    )
    @ConfigItem(
        keyName = "maxEatPercent",
        name = "Max Eat %",
        description = "Maximum health percentage to eat at",
        position = 6
    )
    default int maxEatPercent() {
        return 65;
    }

    @Range(
        min = 1,
        max = 99
    )
    @ConfigItem(
        keyName = "minPrayerPercent",
        name = "Min Prayer %",
        description = "Minimum prayer percentage to drink at",
        position = 7
    )
    default int minPrayerPercent() {
        return 30;
    }

    @Range(
        min = 1,
        max = 99
    )
    @ConfigItem(
        keyName = "maxPrayerPercent",
        name = "Max Prayer %",
        description = "Maximum prayer percentage to drink at",
        position = 8
    )
    default int maxPrayerPercent() {
        return 45;
    }

    @ConfigItem(
        keyName = "prioritizeRats",
        name = "Prioritize Rats",
        description = "Prioritize killing rats over Scurrius",
        position = 9
    )
    default boolean prioritizeRats() {
        return false;
    }

    @ConfigItem(
        keyName = "bossRoomEntryType",
        name = "Boss Room Entry Type",
        description = "How to enter the boss room",
        position = 10
    )
    default BossRoomEntryType bossRoomEntryType() {
        return BossRoomEntryType.SQUEEZE_THROUGH;
    }

    enum FoodType {
        SHARK(385),
        MONKFISH(7946),
        LOBSTER(379),
        SWORDFISH(373);

        private final int id;

        FoodType(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    enum PotionType {
        PRAYER_POTION(2434),
        SUPER_RESTORE(3024);

        private final int itemId;

        PotionType(int itemId) {
            this.itemId = itemId;
        }

        public int getItemId() {
            return itemId;
        }
    }

    enum BossRoomEntryType {
        SQUEEZE_THROUGH("Squeeze-through"),
        ENTER("Enter");

        private final String interactionText;

        BossRoomEntryType(String interactionText) {
            this.interactionText = interactionText;
        }

        public String getInteractionText() {
            return interactionText;
        }
    }
}
