package net.runelite.client.plugins.microbot.jstwc;

import net.runelite.client.config.*;

@ConfigGroup("jstwc")
public interface JstWCConfig extends Config {
    
    @ConfigSection(
            name = "General Settings",
            description = "Basic configuration for 2-tick woodcutting",
            position = 0
    )
    String generalSection = "general";
    
    @ConfigSection(
            name = "Combat Settings", 
            description = "Settings for maintaining aggression from birds",
            position = 1
    )
    String combatSection = "combat";
    
    @ConfigSection(
            name = "Drop Settings",
            description = "Configure what to drop and when",
            position = 2
    )
    String dropSection = "drop";
    
    @ConfigSection(
            name = "Auto-Buy Settings",
            description = "Configure automatic equipment purchasing",
            position = 3
    )
    String autoBuySection = "autobuy";
    
    @ConfigSection(
            name = "Travel Settings",
            description = "Configure automatic travel to Isle of Souls",
            position = 5
    )
    String travelSection = "travel";
    
    @ConfigItem(
            keyName = "startHotkey",
            name = "Start/Stop Hotkey",
            description = "Hotkey to start or stop the script",
            position = 0,
            section = generalSection
    )
    default Keybind startHotkey() {
        return Keybind.NOT_SET;
    }
    
    @ConfigItem(
            keyName = "useAntiban",
            name = "Use Antiban",
            description = "Enable antiban features for more human-like behavior",
            position = 1,
            section = generalSection
    )
    default boolean useAntiban() {
        return true;
    }
    
    @ConfigItem(
            keyName = "enableBreaks",
            name = "Enable Breaks",
            description = "Take random breaks to appear more human-like",
            position = 2,
            section = generalSection
    )
    default boolean enableBreaks() {
        return true;
    }
    
    @ConfigItem(
            keyName = "useRegenBracelet",
            name = "Use Regen Bracelet",
            description = "Equip regen bracelet to minimize damage from birds",
            position = 0,
            section = combatSection
    )
    default boolean useRegenBracelet() {
        return true;
    }
    
    @ConfigItem(
            keyName = "useHuntersCrossbow",
            name = "Use Hunter's Crossbow",
            description = "Use hunter's crossbow with shield instead of shortbow",
            position = 1,
            section = combatSection
    )
    default boolean useHuntersCrossbow() {
        return false;
    }
    
    @ConfigItem(
            keyName = "healthThreshold",
            name = "Health Threshold",
            description = "Minimum health percentage before eating food",
            position = 2,
            section = combatSection
    )
    @Range(min = 10, max = 90)
    default int healthThreshold() {
        return 40;
    }
    
    @ConfigItem(
            keyName = "foodName",
            name = "Food Name",
            description = "Name of food to eat when health is low",
            position = 3,
            section = combatSection
    )
    default String foodName() {
        return "Lobster";
    }
    
    @ConfigItem(
            keyName = "dropLogs",
            name = "Drop Logs",
            description = "Drop teak logs when inventory is full",
            position = 0,
            section = dropSection
    )
    default boolean dropLogs() {
        return true;
    }
    
    @ConfigItem(
            keyName = "dropFood",
            name = "Drop Food",
            description = "Drop food when running low on inventory space",
            position = 1,
            section = dropSection
    )
    default boolean dropFood() {
        return false;
    }
    
    @ConfigItem(
            keyName = "keepLogsAmount",
            name = "Keep Logs Amount",
            description = "Number of teak logs to keep in inventory (0 = drop all)",
            position = 2,
            section = dropSection
    )
    @Range(min = 0, max = 28)
    default int keepLogsAmount() {
        return 0;
    }
    
    @ConfigItem(
            keyName = "dropPattern",
            name = "Drop Pattern",
            description = "Pattern for dropping logs",
            position = 3,
            section = dropSection
    )
    default DropPattern dropPattern() {
        return DropPattern.ALTERNATING;
    }
    
    enum DropPattern {
        ALTERNATING("Alternating (recommended for 2-tick)"),
        SEQUENTIAL("Sequential"),
        RANDOM("Random");
        
        private final String description;
        
        DropPattern(String description) {
            this.description = description;
        }
        
        @Override
        public String toString() {
            return description;
        }
    }
    
    @ConfigItem(
            keyName = "skipEquipmentValidation",
            name = "Skip Equipment Validation",
            description = "Skip equipment checks and assume you have all required items. Use only if you're sure you have everything!",
            position = 0,
            section = autoBuySection
    )
    default boolean skipEquipmentValidation() {
        return false;
    }
    
    @ConfigItem(
            keyName = "enableAutoBuy",
            name = "Enable Auto-Buy",
            description = "Automatically purchase missing equipment from Grand Exchange",
            position = 1,
            section = autoBuySection
    )
    default boolean enableAutoBuy() {
        return true;
    }
    
    @ConfigItem(
            keyName = "equipmentBudget",
            name = "Equipment Budget",
            description = "Maximum gold to spend on equipment",
            position = 2,
            section = autoBuySection
    )
    @Range(min = 10000, max = 10000000)
    default int equipmentBudget() {
        return 100000;
    }
    
    @ConfigItem(
            keyName = "preferredBow",
            name = "Preferred Bow",
            description = "Preferred bow type for 2-tick woodcutting",
            position = 3,
            section = autoBuySection
    )
    default String preferredBow() {
        return "Willow shortbow";
    }
    
    @ConfigItem(
            keyName = "preferredAxe",
            name = "Preferred Axe",
            description = "Preferred axe type for woodcutting",
            position = 4,
            section = autoBuySection
    )
    default String preferredAxe() {
        return "Rune axe";
    }
    
    @ConfigItem(
            keyName = "buyRegenBracelet",
            name = "Buy Regen Bracelet",
            description = "Purchase regen bracelet if not owned",
            position = 5,
            section = autoBuySection
    )
    default boolean buyRegenBracelet() {
        return true;
    }
    
    @ConfigItem(
            keyName = "enableAutoTravel",
            name = "Enable Auto-Travel",
            description = "Automatically travel to Isle of Souls",
            position = 0,
            section = travelSection
    )
    default boolean enableAutoTravel() {
        return true;
    }
    
    @ConfigItem(
            keyName = "bankingLocation",
            name = "Banking Location",
            description = "Preferred banking location",
            position = 1,
            section = travelSection
    )
    default BankingLocation bankingLocation() {
        return BankingLocation.VARROCK_WEST;
    }
    
    @ConfigItem(
            keyName = "travelMethod",
            name = "Travel Method",
            description = "Method to reach Isle of Souls",
            position = 2,
            section = travelSection
    )
    default TravelMethod travelMethod() {
        return TravelMethod.CHARTER_SHIP;
    }
    
    @ConfigItem(
            keyName = "minFoodAmount",
            name = "Minimum Food Amount",
            description = "Minimum food to withdraw before traveling",
            position = 3,
            section = travelSection
    )
    @Range(min = 5, max = 20)
    default int minFoodAmount() {
        return 10;
    }
    
    @ConfigItem(
            keyName = "enableWorldHopping",
            name = "Enable World Hopping",
            description = "Hop worlds when other players are detected at the location",
            position = 4,
            section = travelSection
    )
    default boolean enableWorldHopping() {
        return true;
    }
    
    @ConfigItem(
            keyName = "maxPlayersBeforeHop",
            name = "Max Players Before Hop",
            description = "Maximum number of other players allowed at the location before hopping",
            position = 5,
            section = travelSection
    )
    @Range(min = 0, max = 5)
    default int maxPlayersBeforeHop() {
        return 0; // Hop immediately when any other player is detected (most aggressive setting)
    }
    
    enum BankingLocation {
        VARROCK_WEST("Varrock West (GE)"),
        EDGEVILLE("Edgeville"),
        FALADOR_WEST("Falador West"),
        DRAYNOR("Draynor Village");
        
        private final String description;
        
        BankingLocation(String description) {
            this.description = description;
        }
        
        @Override
        public String toString() {
            return description;
        }
    }
    
    enum TravelMethod {
        CHARTER_SHIP("Charter ship from Port Sarim"),
        DIGSITE_PENDANT("Digsite pendant to Fossil Island"),
        SKILLS_NECKLACE("Skills necklace to Farming Guild");
        
        private final String description;
        
        TravelMethod(String description) {
            this.description = description;
        }
        
        @Override
        public String toString() {
            return description;
        }
    }
}