package net.runelite.client.plugins.microbot.jstwc;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.util.*;

public class JstWCEquipmentManager {
    
    public enum PurchaseResult {
        SUCCESS,
        INSUFFICIENT_GOLD,
        PRICE_TOO_HIGH,
        ITEM_UNAVAILABLE,
        GE_LIMIT_REACHED,
        FAILED
    }
    
    private static final List<String> AXE_PRIORITY = Arrays.asList(
            "Dragon axe", "Rune axe", "Adamant axe", "Mithril axe", 
            "Black axe", "Steel axe", "Iron axe", "Bronze axe"
    );
    
    private static final List<String> BOW_PRIORITY = Arrays.asList(
            "Yew shortbow", "Magic shortbow", "Maple shortbow",
            "Willow shortbow", "Oak shortbow", "Shortbow"
    );
    
    private static final List<String> CROSSBOW_PRIORITY = Arrays.asList(
            "Hunter's crossbow", "Iron crossbow", "Bronze crossbow"
    );
    
    private static final List<String> SHIELD_PRIORITY = Arrays.asList(
            "Wooden shield", "Bronze sq shield", "Iron sq shield"
    );
    
    private static final List<String> FOOD_PRIORITY = Arrays.asList(
            "Monkfish", "Lobster", "Swordfish", "Tuna", "Salmon", "Trout"
    );
    
    private final JstWCConfig config;
    private final Map<String, Boolean> equipmentStatus = new HashMap<>();
    private final List<String> itemsToBuy = new ArrayList<>();
    private final Map<String, PurchaseResult> purchaseResults = new HashMap<>();
    private final Map<String, Integer> failedItems = new HashMap<>();
    
    public JstWCEquipmentManager(JstWCConfig config) {
        this.config = config;
    }
    
    public boolean validateEquipment() {
        equipmentStatus.clear();
        
        // Check for axe
        boolean hasAxe = hasAnyAxe();
        equipmentStatus.put("axe", hasAxe);
        Microbot.log("JstWC Equipment: Has axe: " + hasAxe);
        
        // Check for weapon (bow or crossbow)
        boolean hasWeapon = false;
        if (config.useHuntersCrossbow()) {
            boolean hasCrossbow = hasAnyCrossbow();
            boolean hasShield = hasAnyShield();
            hasWeapon = hasCrossbow && hasShield;
            equipmentStatus.put("crossbow", hasCrossbow);
            equipmentStatus.put("shield", hasShield);
            Microbot.log("JstWC Equipment: Has crossbow: " + hasCrossbow + ", Has shield: " + hasShield);
        } else {
            hasWeapon = hasAnyBow();
            equipmentStatus.put("bow", hasWeapon);
            Microbot.log("JstWC Equipment: Has bow: " + hasWeapon);
        }
        
        // Check for regen bracelet if enabled
        if (config.useRegenBracelet()) {
            boolean hasRegen = Rs2Equipment.isWearing("Regen bracelet") || 
                              Rs2Inventory.contains("Regen bracelet");
            equipmentStatus.put("regen_bracelet", hasRegen);
            Microbot.log("JstWC Equipment: Has regen bracelet: " + hasRegen);
        }
        
        // Check for food
        boolean hasFood = hasAnyFood();
        equipmentStatus.put("food", hasFood);
        Microbot.log("JstWC Equipment: Has food: " + hasFood);
        
        // Check for runes
        boolean hasRunes = hasRequiredRunes();
        equipmentStatus.put("runes", hasRunes);
        
        boolean allValid = hasAxe && hasWeapon && hasFood && hasRunes;
        Microbot.log("JstWC Equipment: Overall validation result: " + allValid);
        
        return allValid;
    }
    
    public List<String> getMissingEquipment() {
        itemsToBuy.clear();
        
        if (!equipmentStatus.getOrDefault("axe", false)) {
            String preferredAxe = config.preferredAxe();
            itemsToBuy.add(findAffordableItem(AXE_PRIORITY, preferredAxe));
        }
        
        if (config.useHuntersCrossbow()) {
            if (!equipmentStatus.getOrDefault("crossbow", false)) {
                itemsToBuy.add(findAffordableItem(CROSSBOW_PRIORITY, "Hunter's crossbow"));
            }
            if (!equipmentStatus.getOrDefault("shield", false)) {
                itemsToBuy.add(findAffordableItem(SHIELD_PRIORITY, "Wooden shield"));
            }
        } else {
            if (!equipmentStatus.getOrDefault("bow", false)) {
                String preferredBow = config.preferredBow();
                itemsToBuy.add(findAffordableItem(BOW_PRIORITY, preferredBow));
            }
        }
        
        if (config.useRegenBracelet() && config.buyRegenBracelet() &&
            !equipmentStatus.getOrDefault("regen_bracelet", false)) {
            itemsToBuy.add("Regen bracelet");
        }
        
        if (!equipmentStatus.getOrDefault("food", false)) {
            String foodType = config.foodName();
            itemsToBuy.add(findAffordableItem(FOOD_PRIORITY, foodType));
        }
        
        if (!equipmentStatus.getOrDefault("runes", false)) {
            itemsToBuy.add("Mind rune");
            itemsToBuy.add("Water rune");
            itemsToBuy.add("Earth rune");
        }
        
        return itemsToBuy;
    }
    
    public PurchaseResult purchaseEquipment() {
        if (!Rs2GrandExchange.isOpen()) {
            if (!Rs2GrandExchange.openExchange()) {
                return PurchaseResult.FAILED;
            }
            try { Thread.sleep(1200 + (int)(Math.random() * 600)); } catch (InterruptedException e) {}
        }
        
        int budget = config.equipmentBudget();
        int currentGold = Rs2Inventory.count("Coins");
        
        if (currentGold < 1000) {
            Microbot.log("Insufficient gold for equipment purchases (have: " + currentGold + ")");
            return PurchaseResult.INSUFFICIENT_GOLD;
        }
        
        boolean allSuccess = true;
        PurchaseResult worstResult = PurchaseResult.SUCCESS;
        
        for (String item : itemsToBuy) {
            if (item == null || purchaseResults.getOrDefault(item, PurchaseResult.FAILED) == PurchaseResult.SUCCESS) {
                continue;
            }
            
            // Skip items that have failed too many times
            if (failedItems.getOrDefault(item, 0) >= 2) {
                Microbot.log("Skipping " + item + " due to repeated failures");
                continue;
            }
            
            // Special handling for stackable items
            int quantity = 1;
            if (item.equals(config.foodName()) || FOOD_PRIORITY.contains(item)) {
                quantity = config.minFoodAmount();
            } else if (item.contains("rune")) {
                quantity = 100; // Buy 100 runes
            }
            
            // Calculate max price based on budget and item type
            int maxPrice = calculateMaxPrice(item, budget, currentGold);
            
            PurchaseResult result = attemptPurchase(item, quantity, maxPrice);
            purchaseResults.put(item, result);
            
            if (result != PurchaseResult.SUCCESS) {
                allSuccess = false;
                failedItems.put(item, failedItems.getOrDefault(item, 0) + 1);
                
                // Try alternative items for critical equipment
                if (result == PurchaseResult.PRICE_TOO_HIGH || result == PurchaseResult.ITEM_UNAVAILABLE) {
                    String alternative = findAlternativeItem(item);
                    if (alternative != null && !itemsToBuy.contains(alternative)) {
                        Microbot.log("Trying alternative: " + alternative + " instead of " + item);
                        itemsToBuy.add(alternative);
                    }
                }
                
                // Track worst result
                if (result.ordinal() > worstResult.ordinal()) {
                    worstResult = result;
                }
            } else {
                // Update remaining gold
                currentGold = Rs2Inventory.count("Coins");
            }
            
            try { Thread.sleep(1200 + (int)(Math.random() * 800)); } catch (InterruptedException e) {}
        }
        
        return allSuccess ? PurchaseResult.SUCCESS : worstResult;
    }
    
    private PurchaseResult attemptPurchase(String item, int quantity, int maxPrice) {
        try {
            // Try to buy with price limit (Rs2GrandExchange.buyItem takes price, then quantity)
            if (maxPrice > 0) {
                if (!Rs2GrandExchange.buyItem(item, maxPrice, quantity)) {
                    // Check specific failure reason
                    if (Rs2Inventory.count("Coins") < maxPrice * quantity) {
                        return PurchaseResult.INSUFFICIENT_GOLD;
                    }
                    return PurchaseResult.ITEM_UNAVAILABLE;
                }
            } else {
                // Use reasonable default price for market purchase
                int defaultPrice = calculateMaxPrice(item, config.equipmentBudget(), Rs2Inventory.count("Coins"));
                if (defaultPrice == 0) defaultPrice = 1000; // Fallback price
                
                if (!Rs2GrandExchange.buyItem(item, defaultPrice, quantity)) {
                    return PurchaseResult.ITEM_UNAVAILABLE;
                }
            }
            
            Microbot.log("Successfully initiated purchase of " + quantity + "x " + item);
            return PurchaseResult.SUCCESS;
            
        } catch (Exception e) {
            Microbot.log("Exception during purchase: " + e.getMessage());
            return PurchaseResult.FAILED;
        }
    }
    
    private int calculateMaxPrice(String item, int budget, int currentGold) {
        // Set reasonable price limits based on item type
        if (item.contains("Dragon axe")) return Math.min(5000000, currentGold);
        if (item.contains("Rune axe")) return Math.min(50000, currentGold);
        if (item.contains("Adamant axe")) return Math.min(5000, currentGold);
        if (item.contains("Mithril axe")) return Math.min(2000, currentGold);
        if (item.contains("axe")) return Math.min(1000, currentGold);
        
        if (item.contains("shortbow")) return Math.min(2000, currentGold);
        if (item.contains("crossbow")) return Math.min(5000, currentGold);
        if (item.contains("shield")) return Math.min(1000, currentGold);
        
        if (item.contains("Regen bracelet")) return Math.min(200000, currentGold);
        
        if (FOOD_PRIORITY.contains(item)) return Math.min(2000, currentGold / config.minFoodAmount());
        if (item.contains("rune")) return Math.min(200, currentGold / 100);
        
        return 0; // Use market price for unknown items
    }
    
    private String findAlternativeItem(String item) {
        // Find cheaper alternatives from priority lists
        if (AXE_PRIORITY.contains(item)) {
            int index = AXE_PRIORITY.indexOf(item);
            if (index < AXE_PRIORITY.size() - 1) {
                return AXE_PRIORITY.get(index + 1);
            }
        }
        
        if (BOW_PRIORITY.contains(item)) {
            int index = BOW_PRIORITY.indexOf(item);
            if (index < BOW_PRIORITY.size() - 1) {
                return BOW_PRIORITY.get(index + 1);
            }
        }
        
        if (CROSSBOW_PRIORITY.contains(item)) {
            int index = CROSSBOW_PRIORITY.indexOf(item);
            if (index < CROSSBOW_PRIORITY.size() - 1) {
                return CROSSBOW_PRIORITY.get(index + 1);
            }
        }
        
        if (FOOD_PRIORITY.contains(item)) {
            int index = FOOD_PRIORITY.indexOf(item);
            if (index < FOOD_PRIORITY.size() - 1) {
                return FOOD_PRIORITY.get(index + 1);
            }
        }
        
        return null;
    }
    
    public boolean collectPurchases() {
        if (!Rs2GrandExchange.isOpen()) {
            Rs2GrandExchange.openExchange();
            return false;
        }
        
        return Rs2GrandExchange.collectAllToInventory();
    }
    
    private boolean hasAnyAxe() {
        for (String axe : AXE_PRIORITY) {
            if (Rs2Equipment.isWearing(axe) || Rs2Inventory.contains(axe)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean hasAnyBow() {
        for (String bow : BOW_PRIORITY) {
            if (Rs2Equipment.isWearing(bow) || Rs2Inventory.contains(bow)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean hasAnyCrossbow() {
        for (String crossbow : CROSSBOW_PRIORITY) {
            if (Rs2Equipment.isWearing(crossbow) || Rs2Inventory.contains(crossbow)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean hasAnyShield() {
        for (String shield : SHIELD_PRIORITY) {
            if (Rs2Equipment.isWearing(shield) || Rs2Inventory.contains(shield)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean hasAnyFood() {
        if (Rs2Inventory.count(config.foodName()) >= 5) {
            return true;
        }
        // Check for any food in priority list
        for (String food : FOOD_PRIORITY) {
            if (Rs2Inventory.contains(food)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean hasRequiredRunes() {
        // CRITICAL: Check for curse spell runes - REQUIRED for 2-tick woodcutting
        // Weaken spell (preferred): 1 Mind, 3 Water, 2 Earth per cast
        // Confuse spell (fallback): 2 Mind, 3 Water, 3 Earth per cast
        int mindCount = Rs2Inventory.count("Mind rune");
        int waterCount = Rs2Inventory.count("Water rune");
        int earthCount = Rs2Inventory.count("Earth rune");
        
        Microbot.log("JstWC Equipment: Curse spell rune counts - Mind: " + mindCount + ", Water: " + waterCount + ", Earth: " + earthCount);
        
        // Check for elemental staves that provide unlimited runes
        boolean hasWaterStaff = Rs2Equipment.isWearing("Staff of water", "Mud staff", "Steam staff", "Mist staff");
        boolean hasEarthStaff = Rs2Equipment.isWearing("Staff of earth", "Mud staff", "Lava staff", "Dust staff");
        
        Microbot.log("JstWC Equipment: Staff status - Water staff: " + hasWaterStaff + ", Earth staff: " + hasEarthStaff);
        
        // We need enough runes for multiple casts (recommend at least 20-50 casts worth)
        // Weaken: 1 Mind, 3 Water, 2 Earth (50 casts = 50 mind, 150 water, 100 earth)
        // Confuse: 2 Mind, 3 Water, 3 Earth (50 casts = 100 mind, 150 water, 150 earth)
        
        boolean canCastWeaken = mindCount >= 50 && 
                               (waterCount >= 150 || hasWaterStaff) && 
                               (earthCount >= 100 || hasEarthStaff);
        
        boolean canCastConfuse = mindCount >= 100 && 
                                (waterCount >= 150 || hasWaterStaff) && 
                                (earthCount >= 150 || hasEarthStaff);
        
        boolean hasEnoughRunes = canCastWeaken || canCastConfuse;
        
        if (!hasEnoughRunes) {
            Microbot.log("JstWC Equipment: WARNING - Insufficient runes for curse spells!");
            Microbot.log("JstWC Equipment: For Weaken need: 50+ Mind, 150+ Water (or water staff), 100+ Earth (or earth staff)");
            Microbot.log("JstWC Equipment: For Confuse need: 100+ Mind, 150+ Water (or water staff), 150+ Earth (or earth staff)");
        } else {
            if (canCastWeaken) {
                Microbot.log("JstWC Equipment: Have sufficient runes for Weaken spell (preferred)");
            } else {
                Microbot.log("JstWC Equipment: Have sufficient runes for Confuse spell (fallback)");
            }
        }
        
        return hasEnoughRunes;
    }
    
    private String findAffordableItem(List<String> priorityList, String preferred) {
        // First try the preferred item
        if (priorityList.contains(preferred)) {
            return preferred;
        }
        
        // Otherwise return the first item in priority list
        return priorityList.isEmpty() ? null : priorityList.get(0);
    }
    
    public boolean needsBanking() {
        // Check if we need to bank for equipment we already own
        return !hasAnyAxe() || (!hasAnyBow() && !config.useHuntersCrossbow()) ||
               (config.useHuntersCrossbow() && (!hasAnyCrossbow() || !hasAnyShield())) ||
               !hasAnyFood() || !hasRequiredRunes();
    }
    
    public boolean withdrawEquipmentFromBank() {
        if (!Rs2Bank.isOpen()) {
            return false;
        }
        
        // Withdraw axe
        if (!hasAnyAxe()) {
            for (String axe : AXE_PRIORITY) {
                if (Rs2Bank.hasItem(axe)) {
                    Rs2Bank.withdrawOne(axe);
                    try { Thread.sleep(600 + (int)(Math.random() * 200)); } catch (InterruptedException e) {}
                    break;
                }
            }
        }
        
        // Withdraw weapon
        if (config.useHuntersCrossbow()) {
            if (!hasAnyCrossbow()) {
                for (String crossbow : CROSSBOW_PRIORITY) {
                    if (Rs2Bank.hasItem(crossbow)) {
                        Rs2Bank.withdrawOne(crossbow);
                        try { Thread.sleep(600 + (int)(Math.random() * 200)); } catch (InterruptedException e) {}
                        break;
                    }
                }
            }
            if (!hasAnyShield()) {
                for (String shield : SHIELD_PRIORITY) {
                    if (Rs2Bank.hasItem(shield)) {
                        Rs2Bank.withdrawOne(shield);
                        try { Thread.sleep(600 + (int)(Math.random() * 200)); } catch (InterruptedException e) {}
                        break;
                    }
                }
            }
        } else {
            if (!hasAnyBow()) {
                for (String bow : BOW_PRIORITY) {
                    if (Rs2Bank.hasItem(bow)) {
                        Rs2Bank.withdrawOne(bow);
                        try { Thread.sleep(600 + (int)(Math.random() * 200)); } catch (InterruptedException e) {}
                        break;
                    }
                }
            }
        }
        
        // Withdraw food
        if (!hasAnyFood()) {
            String foodName = config.foodName();
            if (Rs2Bank.hasItem(foodName)) {
                Rs2Bank.withdrawX(foodName, config.minFoodAmount());
            } else {
                // Try other food types
                for (String food : FOOD_PRIORITY) {
                    if (Rs2Bank.hasItem(food)) {
                        Rs2Bank.withdrawX(food, config.minFoodAmount());
                        break;
                    }
                }
            }
            try { Thread.sleep(600 + (int)(Math.random() * 200)); } catch (InterruptedException e) {}
        }
        
        // Withdraw runes
        if (!hasRequiredRunes()) {
            if (Rs2Inventory.count("Mind rune") < 50 && Rs2Bank.hasItem("Mind rune")) {
                Rs2Bank.withdrawX("Mind rune", 100);
                try { Thread.sleep(600 + (int)(Math.random() * 200)); } catch (InterruptedException e) {}
            }
            if (Rs2Inventory.count("Water rune") < 50 && Rs2Bank.hasItem("Water rune") &&
                !Rs2Equipment.isWearing("Staff of water") && !Rs2Equipment.isWearing("Mud staff")) {
                Rs2Bank.withdrawX("Water rune", 100);
                try { Thread.sleep(600 + (int)(Math.random() * 200)); } catch (InterruptedException e) {}
            }
            if (Rs2Inventory.count("Earth rune") < 50 && Rs2Bank.hasItem("Earth rune") &&
                !Rs2Equipment.isWearing("Staff of earth") && !Rs2Equipment.isWearing("Mud staff")) {
                Rs2Bank.withdrawX("Earth rune", 100);
                try { Thread.sleep(600 + (int)(Math.random() * 200)); } catch (InterruptedException e) {}
            }
        }
        
        // Withdraw regen bracelet if configured
        if (config.useRegenBracelet() && !Rs2Equipment.isWearing("Regen bracelet") &&
            Rs2Bank.hasItem("Regen bracelet")) {
            Rs2Bank.withdrawOne("Regen bracelet");
            try { Thread.sleep(600 + (int)(Math.random() * 200)); } catch (InterruptedException e) {}
        }
        
        return true;
    }
    
    public void clearPurchaseHistory() {
        purchaseResults.clear();
        failedItems.clear();
    }
    
    public Map<String, PurchaseResult> getPurchaseResults() {
        return new HashMap<>(purchaseResults);
    }
    
    public boolean hasFailedPurchases() {
        return purchaseResults.values().stream()
                .anyMatch(result -> result != PurchaseResult.SUCCESS);
    }
}