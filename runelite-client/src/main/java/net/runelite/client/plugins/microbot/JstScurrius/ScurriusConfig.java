package net.runelite.client.plugins.microbot.JstScurrius;

import net.runelite.client.plugins.microbot.JstScurrius.Enums.BoostPotions;
import net.runelite.client.plugins.microbot.JstScurrius.Enums.FightType;
import net.runelite.client.plugins.microbot.JstScurrius.Enums.FoodType;
import net.runelite.client.plugins.microbot.JstScurrius.Enums.ScurriusOffensivePrayer;
import net.runelite.client.plugins.microbot.JstScurrius.Enums.ScurriusPrayerStyle;
import net.runelite.client.plugins.microbot.JstScurrius.Enums.ScurriusRestorePotions;
import net.runelite.client.plugins.microbot.JstScurrius.Enums.ScurriusRunePouch;
import net.runelite.client.plugins.microbot.JstScurrius.Enums.UiLayoutOption;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;

@ConfigGroup(value="autoScurriusPluginConfig")
public interface ScurriusConfig
extends Config {
    @ConfigSection(name="Start Plugin", description="", position=0)
    public static final String instructionsSection = "instructionsSection";
    @ConfigSection(name="Bank Settings", description="Inventory Setup", position=2)
    public static final String bankSettings = "bankSettings";
    @ConfigSection(name="Health Settings", description="", position=1)
    public static final String healthSettings = "healthSettings";
    @ConfigSection(name="Loot & Alch Configuration", description="", position=55, closedByDefault=false)
    public static final String lootItems = "lootItems";
    @ConfigSection(name="Game Tick Configuration", description="Configure how the bot handles game tick delays, 1 game tick equates to roughly 600ms", position=99, closedByDefault=true)
    public static final String delayTickConfig = "delayTickConfig";
    @ConfigSection(name="UI Settings", description="Settings related to the user interface", position=100)
    public static final String uiSettings = "uiSettings";

    @ConfigItem(keyName="start/stop hotkey", name="Start Key", description="Toggle for turning plugin on and off.", position=0, section="instructionsSection")
    default public Keybind toggle() {
        return Keybind.NOT_SET;
    }

    @ConfigItem(keyName="prayerStyle", name="Prayer Style", description="", section="instructionsSection", position=2)
    default public ScurriusPrayerStyle prayerStyle() {
        return ScurriusPrayerStyle.NORMAL;
    }

    @ConfigItem(keyName="offensivePrayer", name="Prayer", description="Select the offensive prayer to use", section="instructionsSection", position=3)
    default public ScurriusOffensivePrayer offensivePrayer() {
        return ScurriusOffensivePrayer.PIETY;
    }

    @ConfigItem(keyName="instanceType", name="Instance Type", description="Select the instance type", section="instructionsSection", position=4)
    default public FightType instanceType() {
        return FightType.INSTANCED;
    }

    @ConfigItem(keyName="runePouch", name="Rune Pouch", description="Select the rune pouch to use", section="bankSettings", position=0)
    default public ScurriusRunePouch runePouch() {
        return ScurriusRunePouch.NORMAL;
    }

    @ConfigItem(keyName="boostPotions", name="Boost Potions", description="Select the boost potions to use", section="bankSettings", position=1)
    default public BoostPotions boostPotions() {
        return BoostPotions.SUPER_COMBAT;
    }

    @ConfigItem(keyName="foodType", name="Food Type", description="Select the food type to use", section="bankSettings", position=2)
    default public FoodType foodType() {
        return FoodType.SHARK;
    }

    @ConfigItem(keyName="restorePotion", name="Restore Potion", description="Select the type of prayer restore potion", section="bankSettings", position=3)
    default public ScurriusRestorePotions restorePotion() {
        return ScurriusRestorePotions.PRAYER_POTION;
    }

    @ConfigItem(keyName="useStamina", name="Use Stamina", description="Use Stamina Potions", section="bankSettings", position=4)
    default public boolean useStamina() {
        return false;
    }

    @ConfigItem(keyName="runesInPouch", name="Runes in Pouch", description="Select the runes to store in the pouch, ensure these are the TELEPORT runes ONLY", section="bankSettings", position=5)
    default public boolean runesInPouch() {
        return true;
    }

    @ConfigItem(keyName="foodAmount", name="Food Amount", description="Amount of food to withdraw", section="bankSettings", position=6)
    default public int foodAmount() {
        return 10;
    }

    @ConfigItem(keyName="restoreAmounts", name="Restore Amounts", description="Amount of restore potions to withdraw", section="bankSettings", position=7)
    default public int restoreAmounts() {
        return 10;
    }

    @ConfigItem(keyName="boostPotionAmount", name="Boost Potion", description="Amount of boost potions to withdraw", section="bankSettings", position=8)
    default public int boostPotionAmount() {
        return 10;
    }

    @ConfigItem(keyName="staminaAmount", name="Stamina Amount", description="Amount of stamina potions to withdraw", section="bankSettings", position=10)
    default public int staminaAmount() {
        return 10;
    }

    @ConfigItem(keyName="airRuneAmounts", name="Air Runes", description="Amount of air runes to withdraw if you dont have Rune pouch", section="bankSettings", position=11)
    default public int airRuneAmounts() {
        return 10;
    }

    @ConfigItem(keyName="lawRuneAmounts", name="Law Runes", description="Amount of law runes to withdraw if you dont have Rune pouch", section="bankSettings", position=12)
    default public int lawRuneAmounts() {
        return 10;
    }

    @ConfigItem(keyName="fireRuneAmount", name="Fire Runes", description="Amount of fire runes to withdraw if you dont have Rune pouch", section="bankSettings", position=13)
    default public int fireRuneAmount() {
        return 10;
    }

    @ConfigItem(keyName="natureRuneAmount", name="Nature Runes", description="Amount of nature runes to withdraw", section="bankSettings", position=14)
    default public int natureRuneAmount() {
        return 10;
    }

    @ConfigItem(keyName="healthThreshold", name="Health Threshold", description="Health threshold to eat food", section="healthSettings", position=0)
    default public int healthThreshold() {
        return 50;
    }

    @ConfigItem(keyName="prayerThreshold", name="Prayer Threshold", description="Prayer threshold to drink restore potion", section="healthSettings", position=1)
    default public int prayerThreshold() {
        return 50;
    }

    @ConfigItem(keyName="minBoost", name="Min Boost", description="Minimum boost to drink boost potion", section="healthSettings", position=2)
    default public int minBoost() {
        return 50;
    }

    @ConfigItem(position=2, keyName="lootItemsEnabled", name="Enable Looting", description="Loots Items", section="lootItems")
    default public boolean lootItems() {
        return false;
    }

    @ConfigItem(position=3, keyName="enableAlching", name="Enable Alching", description="Enable Alching", section="lootItems")
    default public boolean enableAlching() {
        return false;
    }

    @ConfigItem(keyName="dropFoodForLoot", name="Drop Food for Loot", description="Drop food to make room for loot", section="lootItems", position=35)
    default public boolean dropFoodForLoot() {
        return false;
    }

    @ConfigItem(keyName="itemstoLoot", name="Items to Loot", description="List of items to loot, separated by commas", section="lootItems", position=54)
    default public String getLootItems() {
        return "Rune sq shield, Rune full helm, Rune med helm, Rune battleaxe, Rune chainbody, Long bone, Scurrius' spine, Curved bone, Rune arrow, Death rune, Chaos rune, Law rune, Coins, Prayer potion(4),";
    }

    @ConfigItem(keyName="itemsToAlch", name="Items to Alch", description="List of items to alch, separated by commas", section="lootItems", position=55)
    default public String itemsToAlch() {
        return "Rune sq shield, Rune full helm, Rune med helm, Rune battleaxe, Rune chainbody";
    }

    @Range(min=0, max=10)
    @ConfigItem(keyName="tickDelayMin", name="Game Tick Min", description="", position=58, section="delayTickConfig")
    default public int tickDelayMin() {
        return 1;
    }

    @Range(min=0, max=10)
    @ConfigItem(keyName="tickDelayMax", name="Game Tick Max", description="", position=59, section="delayTickConfig")
    default public int tickDelayMax() {
        return 3;
    }

    @Range(min=0, max=10)
    @ConfigItem(keyName="tickDelayTarget", name="Game Tick Target", description="", position=60, section="delayTickConfig")
    default public int tickDelayTarget() {
        return 2;
    }

    @Range(min=0, max=10)
    @ConfigItem(keyName="tickDelayDeviation", name="Game Tick Deviation", description="", position=61, section="delayTickConfig")
    default public int tickDelayDeviation() {
        return 1;
    }

    @ConfigItem(keyName="tickDelayWeightedDistribution", name="Game Tick Weighted Distribution", description="Shifts the random distribution towards the lower end at the target, otherwise it will be an even distribution", position=25, section="delayTickConfig")
    default public boolean tickDelayWeightedDistribution() {
        return false;
    }

    @ConfigItem(keyName="uiLayout", name="UI Layout", description="Select the UI layout for the overlay", section="uiSettings", position=50)
    default public UiLayoutOption uiLayout() {
        return UiLayoutOption.FULL;
    }

    @ConfigItem(position=3, keyName="enableRockFallTiles", name="Enable Debug Visuals", description="Displays rockfall tiles", section="uiSettings")
    default public boolean enableRockFallDebug() {
        return false;
    }
}
