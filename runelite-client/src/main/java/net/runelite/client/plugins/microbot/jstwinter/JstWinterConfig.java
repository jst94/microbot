package net.runelite.client.plugins.microbot.jstwinter;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.plugins.microbot.util.misc.Rs2Food;
import net.runelite.client.plugins.microbot.jstwinter.enums.JstBrazier;

@ConfigGroup("jstwinter")
public interface JstWinterConfig extends Config {
    @ConfigSection(
            name = "General",
            description = "General settings",
            position = 0
    )
    String generalSection = "general";
    
    @ConfigSection(
            name = "World Hopping",
            description = "World hopping configuration",
            position = 4
    )
    String worldHopSection = "worldhop";
    
    @ConfigSection(
            name = "Food",
            description = "Food configuration",
            position = 1
    )
    String foodSection = "food";
    
    @ConfigSection(
            name = "Brazier",
            description = "Brazier configuration",
            position = 2
    )
    String brazierSection = "brazier";

    @ConfigSection(
            name = "Pyromancer",
            description = "Pyromancer healing configuration",
            position = 3
    )
    String pyromancerSection = "pyromancer";

    @ConfigItem(
            keyName = "guide",
            name = "How to use",
            description = "How to use this plugin",
            position = 0,
            section = generalSection
    )
    default String GUIDE() {
        return "Start at Wintertodt bank with equipment setup. Wear 4+ warm items for less damage!";
    }

    @ConfigItem(
            keyName = "RelightBrazier",
            name = "Relight Brazier",
            description = "Relight braziers when they go out (6x Firemaking XP)",
            position = 1,
            section = generalSection
    )
    default boolean relightBrazier() {
        return true;
    }

    @ConfigItem(
            keyName = "FletchRoots",
            name = "Fletch roots",
            description = "Fletch bruma roots into kindling (0.6x Fletching XP)",
            position = 2,
            section = generalSection
    )
    default boolean fletchRoots() {
        return true;
    }

    @ConfigItem(
            keyName = "FixBrazier",
            name = "Fix Brazier",
            description = "Fix broken braziers (4x Construction XP)",
            position = 3,
            section = generalSection
    )
    default boolean fixBrazier() {
        return true;
    }

    @ConfigItem(
            keyName = "HealPyromancer",
            name = "Heal Pyromancer",
            description = "Heal pyromancers when damaged (30 points)",
            position = 4,
            section = generalSection
    )
    default boolean healPyromancer() {
        return true;
    }

    @ConfigItem(
            keyName = "AxeInventory",
            name = "Axe In Inventory",
            description = "Keep axe in inventory instead of equipped",
            position = 5,
            section = generalSection
    )
    default boolean axeInInventory() {
        return false;
    }

    @ConfigItem(
            keyName = "MinimumPoints",
            name = "Minimum Points",
            description = "Minimum points to get per game (for rewards)",
            position = 6,
            section = generalSection
    )
    default int minimumPoints() {
        return 500;
    }

    @ConfigItem(
            keyName = "SoloMode",
            name = "Solo Mode",
            description = "Enable solo Wintertodt strategy (13500+ points)",
            position = 7,
            section = generalSection
    )
    default boolean soloMode() {
        return false;
    }

    @ConfigItem(
            keyName = "SoloTargetPoints",
            name = "Solo Target Points",
            description = "Target points for solo mode (higher = more loot rolls)",
            position = 8,
            section = generalSection
    )
    default int soloTargetPoints() {
        return 13500;
    }

    @ConfigItem(
            keyName = "UseRejuvenationPotions",
            name = "Use Rejuvenation Potions",
            description = "Use rejuvenation potions from crates instead of food",
            position = 0,
            section = foodSection
    )
    default boolean useRejuvenationPotions() {
        return false;
    }

    @ConfigItem(
            keyName = "Food",
            name = "Food Type",
            description = "Type of food to use (if not using rejuvenation potions)",
            position = 1,
            section = foodSection
    )
    default Rs2Food food() {
        return Rs2Food.MONKFISH;
    }

    @ConfigItem(
            keyName = "FoodAmount",
            name = "Food Amount",
            description = "Amount of food to take per game",
            position = 2,
            section = foodSection
    )
    default int foodAmount() {
        return 8;
    }

    @ConfigItem(
            keyName = "MinFood",
            name = "Minimum Food",
            description = "Minimum food required to start a new game",
            position = 3,
            section = foodSection
    )
    default int minFood() {
        return 3;
    }

    @ConfigItem(
            keyName = "EatAtWarmth",
            name = "Eat at Warmth %",
            description = "Eat when warmth level drops to this percentage",
            position = 4,
            section = foodSection
    )
    default int eatAtWarmthLevel() {
        return 25;
    }

    @ConfigItem(
            keyName = "WarmthThreshold",
            name = "Bank at Warmth %",
            description = "Bank when warmth drops to this % and no food left",
            position = 5,
            section = foodSection
    )
    default int warmthThreshold() {
        return 15;
    }

    @ConfigItem(
            keyName = "Brazier",
            name = "Brazier Location",
            description = "Which brazier to use",
            position = 0,
            section = brazierSection
    )
    default JstBrazier brazierLocation() {
        return JstBrazier.SOUTH_EAST;
    }

    @ConfigItem(
            keyName = "SmartBrazierSelection",
            name = "Smart Brazier Selection",
            description = "Automatically select the best brazier based on activity",
            position = 1,
            section = brazierSection,
            hidden = true
    )
    default boolean smartBrazierSelection() {
        return false;
    }

    @ConfigItem(
            keyName = "DodgeSnowfall",
            name = "Dodge Snowfall",
            description = "Dodge area-of-effect snowfall attacks",
            position = 2,
            section = brazierSection
    )
    default boolean dodgeSnowfall() {
        return true;
    }

    @ConfigItem(
            keyName = "PyromancerPriority",
            name = "Pyromancer Priority",
            description = "Prioritize healing pyromancers over other activities",
            position = 0,
            section = pyromancerSection
    )
    default boolean pyromancerPriority() {
        return false;
    }

    @ConfigItem(
            keyName = "HealThreshold",
            name = "Heal Below HP %",
            description = "Heal pyromancer when their HP is below this percentage",
            position = 1,
            section = pyromancerSection
    )
    default int healThreshold() {
        return 50;
    }
    
    @ConfigItem(
            keyName = "EnableWorldHopping",
            name = "Enable World Hopping",
            description = "Hop worlds when too many players or competition",
            position = 0,
            section = worldHopSection
    )
    default boolean enableWorldHopping() {
        return false;
    }
    
    @ConfigItem(
            keyName = "MaxPlayersBeforeHop",
            name = "Max Players Before Hop",
            description = "Hop worlds when more than this many players are at Wintertodt",
            position = 1,
            section = worldHopSection
    )
    default int maxPlayersBeforeHop() {
        return 15;
    }
    
    @ConfigItem(
            keyName = "HopOnCrash",
            name = "Hop When Crashed",
            description = "Hop worlds when someone crashes your brazier/roots",
            position = 2,
            section = worldHopSection
    )
    default boolean hopOnCrash() {
        return true;
    }
    
    @ConfigItem(
            keyName = "PreferredWorldType",
            name = "Preferred World Type",
            description = "Preferred world type to hop to",
            position = 3,
            section = worldHopSection
    )
    default WorldType preferredWorldType() {
        return WorldType.MEMBERS;
    }
    
    enum WorldType {
        MEMBERS("Members"),
        FREE("Free to Play"),
        ANY("Any");
        
        private final String name;
        
        WorldType(String name) {
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
}