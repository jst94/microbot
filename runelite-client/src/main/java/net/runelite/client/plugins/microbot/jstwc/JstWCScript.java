package net.runelite.client.plugins.microbot.jstwc;

import net.runelite.api.*;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.kit.KitType;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.breakhandler.BreakHandlerScript;
import net.runelite.client.plugins.microbot.jstwc.enums.JstWCState;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.antiban.enums.Activity;
import net.runelite.client.plugins.microbot.util.antiban.enums.PlayStyle;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldType;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.runelite.api.ItemID.*;

public class JstWCScript extends Script {
    public static String version = "1.0.0";
    
    private JstWCConfig config;
    private JstWCPlugin plugin;
    private JstWCState state = JstWCState.INITIALIZING;
    private String nextAction = "";
    private JstWCEquipmentManager equipmentManager;
    private JstWCErrorRecovery errorRecovery;
    
    // Isle of Souls location and area
    private final WorldArea ISLE_OF_SOULS_AREA = new WorldArea(2184, 2993, 6, 6, 0); // 2184, 2993, 2190, 2987
    private final WorldPoint OPTIMAL_POSITION = new WorldPoint(2186, 2995, 0); // Between trees
    
    // Game object IDs
    private final int[] TEAK_TREE_IDS = {40758}; // Isle of Souls teak tree
    private final int[] BIRD_IDS = {5241, 10541, 5240}; // Isle of Souls birds (Level 5 and 11)
    
    // Equipment and inventory management (using ItemID constants instead of hardcoded arrays)
    
    // Timing and state tracking
    private long lastTreeClick = 0;
    private long lastDropAction = 0;
    private boolean aggressionSetup = false;
    private int ticksSinceLastHit = 0;
    private int attackingBirdsCount = 0;
    private long lastTickTime = 0;
    private boolean isOnHitTick = false;
    
    // World hopping tracking
    private long lastHopTime = 0;
    private static final long HOP_COOLDOWN = 15000; // 15 seconds between hops (reduced for faster player avoidance)
    
    public boolean run(JstWCConfig config, JstWCPlugin plugin) {
        this.config = config;
        this.plugin = plugin;
        this.equipmentManager = new JstWCEquipmentManager(config);
        this.errorRecovery = new JstWCErrorRecovery();
        
        Rs2Antiban.resetAntibanSettings();
        Rs2Antiban.antibanSetupTemplates.applyWoodcuttingSetup();
        Rs2Antiban.setActivity(Activity.GENERAL_WOODCUTTING);
        Rs2Antiban.setPlayStyle(PlayStyle.AGGRESSIVE);
        
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) {
                    if (errorRecovery.getPreviousState() != null && !errorRecovery.isRecoveringFromDisconnect()) {
                        errorRecovery.recordDisconnect();
                    }
                    return;
                }
                
                // Handle reconnection after disconnect
                if (errorRecovery.isRecoveringFromDisconnect()) {
                    Microbot.log("Recovering from disconnection...");
                    errorRecovery.clearDisconnect();
                    state = errorRecovery.getRecoveryState(errorRecovery.getPreviousState());
                    nextAction = "Recovering from disconnection";
                    sleep(2000, 3000);
                }
                
                if (!super.run()) return;
                if (Rs2AntibanSettings.actionCooldownActive) return;
                
                long startTime = System.currentTimeMillis();
                
                // Update previous state for error recovery
                errorRecovery.updatePreviousState(state);
                
                // Handle breaks
                if (config.enableBreaks() && Rs2Antiban.takeMicroBreakByChance()) {
                    return;
                }
                
                // Don't enforce location check at the start - let the flow handle it
                // This was causing issues when players are not at the location initially
                
                // Update aggression tracking
                updateAggressionStatus();
                
                switch (state) {
                    case INITIALIZING:
                        handleInitialization();
                        break;
                        
                    case CHECKING_EQUIPMENT:
                        handleCheckingEquipment();
                        break;
                        
                    case BANKING_FOR_EQUIPMENT:
                        handleBankingForEquipment();
                        break;
                        
                    case BUYING_EQUIPMENT:
                        handleBuyingEquipment();
                        break;
                        
                    case COLLECTING_PURCHASES:
                        handleCollectingPurchases();
                        break;
                        
                    case PREPARING_FOR_TRAVEL:
                        handlePreparingForTravel();
                        break;
                        
                    case TRAVELING_TO_LOCATION:
                        handleTravelingToLocation();
                        break;
                        
                    case SETTING_UP_AGGRESSION:
                        setupAggression();
                        break;
                        
                    case WORLD_HOPPING:
                        handleWorldHopping();
                        break;
                        
                    case POSITIONING:
                        handlePositioning();
                        break;
                        
                    case TWO_TICK_CUTTING:
                        handle2TickCutting();
                        break;
                        
                    case DROPPING_LOGS:
                        handleDropping();
                        break;
                        
                    case EATING_FOOD:
                        handleEating();
                        break;
                        
                    case REGAINING_AGGRESSION:
                        regainAggression();
                        break;
                        
                    case WAITING:
                        handleWaiting();
                        break;
                        
                    case ERROR:
                        handleError();
                        break;
                        
                    case RETRYING:
                        handleRetrying();
                        break;
                }
                
                long endTime = System.currentTimeMillis();
                long executionTime = endTime - startTime;
                if (executionTime > 50) {
                    Microbot.log("JstWC: Long execution time: " + executionTime + "ms");
                }
                
            } catch (Exception ex) {
                Microbot.log("JstWC error: " + ex.getMessage());
                ex.printStackTrace();
                errorRecovery.recordError(state, ex.getMessage());
                
                // Check if we should retry
                if (errorRecovery.shouldRetry(state)) {
                    state = JstWCState.RETRYING;
                    nextAction = "Retrying after error: " + ex.getMessage();
                } else {
                    state = JstWCState.ERROR;
                    nextAction = "Error occurred (max retries exceeded): " + ex.getMessage();
                }
            }
        }, 0, 50, TimeUnit.MILLISECONDS); // 50ms = ~1 game tick for precise timing
        
        return true;
    }
    
    private void handleInitialization() {
        nextAction = "Checking equipment and setup...";
        Microbot.log("JstWC: Starting initialization");
        
        try {
            // Skip auto retaliate for now - causing NPE issues in equipment setup phase
            // Will be set later when actually starting 2-tick cutting
            Microbot.log("JstWC: Skipping auto retaliate during initialization");
            
            // Get current location
            WorldPoint currentLocation = Rs2Player.getWorldLocation();
            if (currentLocation == null) {
                nextAction = "Unable to get player location";
                Microbot.log("JstWC: Player location is null");
                return;
            }
            
            Microbot.log("JstWC: Player location: " + currentLocation);
            Microbot.log("JstWC: Isle of Souls area contains player: " + ISLE_OF_SOULS_AREA.contains(currentLocation));
            
            // Check if we're already at the location with equipment
            if (ISLE_OF_SOULS_AREA.contains(currentLocation)) {
                Microbot.log("JstWC: Player is at Isle of Souls, checking equipment");
                // Quick validation for players already at the location
                boolean hasAxeResult = hasAxe();
                boolean hasWeaponResult = hasProperWeapon();
                
                Microbot.log("JstWC: Has axe: " + hasAxeResult + ", Has weapon: " + hasWeaponResult);
                
                if (!hasAxeResult || !hasWeaponResult) {
                    if (config.enableAutoBuy() || config.enableAutoTravel()) {
                        Microbot.log("JstWC: Missing equipment, checking equipment state");
                        state = JstWCState.CHECKING_EQUIPMENT;
                    } else {
                        nextAction = "Missing required equipment! Enable auto-buy or manually obtain equipment";
                        state = JstWCState.ERROR;
                    }
                    return;
                }
                
                // Check health and food
                if (needsFood()) {
                    Microbot.log("JstWC: Player needs food");
                    state = JstWCState.EATING_FOOD;
                    return;
                }
                
                Microbot.log("JstWC: Equipment and health OK, setting up aggression");
                state = JstWCState.SETTING_UP_AGGRESSION;
            } else {
                Microbot.log("JstWC: Player not at Isle of Souls, checking auto options");
                // Not at location, need to check equipment and travel
                if (config.enableAutoBuy() || config.enableAutoTravel()) {
                    Microbot.log("JstWC: Auto-buy or auto-travel enabled, checking equipment");
                    state = JstWCState.CHECKING_EQUIPMENT;
                } else {
                    nextAction = "Not at Isle of Souls! Enable auto-travel or move there manually";
                    state = JstWCState.ERROR;
                }
            }
        } catch (Exception e) {
            Microbot.log("JstWC: Error in initialization: " + e.getMessage());
            e.printStackTrace();
            nextAction = "Error in initialization: " + e.getMessage();
            state = JstWCState.ERROR;
        }
    }
    
    private void setupAggression() {
        nextAction = "Setting up bird aggression...";
        
        // Check for other players at the location before setting up aggression
        if (config.enableWorldHopping() && shouldWorldHop()) {
            Microbot.log("JstWC: Other players detected, initiating world hop");
            state = JstWCState.WORLD_HOPPING;
            return;
        }
        
        if (aggressionSetup && attackingBirdsCount >= 2) {
            state = JstWCState.POSITIONING;
            return;
        }
        
        // Find birds and draw aggression
        List<Rs2NpcModel> nearbyBirds = Rs2Npc.getNpcs(bird -> 
            Arrays.stream(BIRD_IDS).anyMatch(id -> id == bird.getId()))
            .collect(Collectors.toList());
        
        if (nearbyBirds.isEmpty()) {
            nextAction = "No birds found - walking to bird area";
            Rs2Walker.walkTo(new WorldPoint(2186, 2994, 0));
            return;
        }
        
        // CRITICAL: Use curse spells (Weaken/Confuse) for proper 2-tick aggression setup
        // These spells are REQUIRED for 2-tick woodcutting to work properly
        boolean spellCastSuccessful = false;
        
        if (Rs2Magic.canCast(MagicAction.WEAKEN)) {
            Microbot.log("JstWC: Using Weaken spell for bird aggression (optimal for 2-tick)");
            spellCastSuccessful = castCurseSpellOnBirds(nearbyBirds, MagicAction.WEAKEN);
        } else if (Rs2Magic.canCast(MagicAction.CONFUSE)) {
            Microbot.log("JstWC: Using Confuse spell for bird aggression (fallback)");
            spellCastSuccessful = castCurseSpellOnBirds(nearbyBirds, MagicAction.CONFUSE);
        } else {
            // Check if we have required runes but wrong spell book
            if (hasRequiredCurseRunes()) {
                nextAction = "Have curse runes but can't cast spells - check spellbook (need Standard spellbook)";
                Microbot.log("JstWC: ERROR - Have runes for curse spells but can't cast. Ensure you're on Standard spellbook!");
                state = JstWCState.ERROR;
                return;
            } else {
                nextAction = "Missing curse spell runes! Need Mind, Water, and Earth runes for Weaken/Confuse";
                Microbot.log("JstWC: ERROR - Cannot cast curse spells. Need runes for Weaken (1 Mind, 3 Water, 2 Earth) or Confuse (3 Water, 3 Earth, 2 Mind)");
                state = JstWCState.ERROR;
                return;
            }
        }
        
        if (!spellCastSuccessful) {
            // Last resort: direct attack method (less reliable for 2-tick)
            Microbot.log("JstWC: WARNING - Curse spells failed, using direct attack method (may not work for 2-tick)");
            Rs2NpcModel bird = nearbyBirds.get(0);
            if (Rs2Npc.interact(bird, "Attack")) {
                sleep(1200); // Wait for aggression
            } else {
                nextAction = "Failed to establish bird aggression";
                state = JstWCState.ERROR;
                return;
            }
        }
        
        aggressionSetup = true;
        sleep(2000); // Allow aggression to establish
        state = JstWCState.POSITIONING;
    }
    
    private void handlePositioning() {
        nextAction = "Positioning between teak trees...";
        
        // Check for other players before positioning
        if (config.enableWorldHopping() && shouldWorldHop()) {
            Microbot.log("JstWC: Other players detected during positioning, initiating world hop");
            state = JstWCState.WORLD_HOPPING;
            return;
        }
        
        WorldPoint currentPos = Rs2Player.getWorldLocation();
        
        // Move to optimal position between trees if not already there
        if (currentPos.distanceTo(OPTIMAL_POSITION) > 0) {
            Rs2Walker.walkFastCanvas(OPTIMAL_POSITION);
            sleepUntil(() -> Rs2Player.getWorldLocation().equals(OPTIMAL_POSITION), 3000);
        }
        
        // Check if we have attacking birds
        if (attackingBirdsCount < 2) {
            state = JstWCState.REGAINING_AGGRESSION;
            return;
        }
        
        state = JstWCState.TWO_TICK_CUTTING;
    }
    
    private void handle2TickCutting() {
        nextAction = "2-tick cutting teaks...";
        
        // Check for other players during cutting (they might have arrived after we started)
        if (config.enableWorldHopping() && shouldWorldHop()) {
            Microbot.log("JstWC: Other players detected during cutting, initiating world hop");
            state = JstWCState.WORLD_HOPPING;
            return;
        }
        
        // Check if we need to drop logs
        if (shouldDropLogs()) {
            state = JstWCState.DROPPING_LOGS;
            return;
        }
        
        // Check if we need food
        if (needsFood()) {
            state = JstWCState.EATING_FOOD;
            return;
        }
        
        // Check if we lost aggression
        if (attackingBirdsCount < 2) {
            state = JstWCState.REGAINING_AGGRESSION;
            return;
        }
        
        // 2-tick method: Use timing based on game ticks (600ms per tick)
        long currentTime = System.currentTimeMillis();
        
        // Check if we're on a hit tick (every 1200ms with 2 attacking birds)
        if (currentTime - lastTickTime >= 1200) {
            isOnHitTick = true;
            lastTickTime = currentTime;
        } else if (currentTime - lastTickTime >= 600) {
            isOnHitTick = false;
        }
        
        // Click tree on hit ticks
        if (isOnHitTick && currentTime - lastTreeClick > 100) {
            GameObject teakTree = Rs2GameObject.getGameObject(TEAK_TREE_IDS[0]);
            if (teakTree != null) {
                Rs2GameObject.interact(teakTree, "Chop down");
                lastTreeClick = currentTime;
            }
        }
        
        // Click ground or drop logs on off-ticks
        if (!isOnHitTick && currentTime - lastDropAction > 600) {
            // Either click ground at base of tree or drop a log
            if (shouldAlternatingDrop() && Rs2Inventory.hasItem(TEAK_LOGS)) {
                Rs2Inventory.dropAll(TEAK_LOGS);
                lastDropAction = System.currentTimeMillis();
                plugin.incrementLogsGained();
            } else {
                // Click ground at base of tree
                GameObject teakTree = Rs2GameObject.getGameObject(TEAK_TREE_IDS[0]);
                if (teakTree != null) {
                    Rs2Walker.walkFastCanvas(teakTree.getWorldLocation());
                }
            }
        }
    }
    
    private void handleDropping() {
        nextAction = "Dropping logs...";
        
        if (!shouldDropLogs()) {
            state = JstWCState.TWO_TICK_CUTTING;
            return;
        }
        
        switch (config.dropPattern()) {
            case ALTERNATING:
                // Drop every other log during 2-tick method
                if (Rs2Inventory.hasItem(TEAK_LOGS)) {
                    Rs2Inventory.dropAll(TEAK_LOGS);
                    plugin.incrementLogsGained();
                }
                break;
                
            case SEQUENTIAL:
                // Drop all logs keeping specified amount
                int keepAmount = config.keepLogsAmount();
                while (Rs2Inventory.count(TEAK_LOGS) > keepAmount) {
                    Rs2Inventory.dropAll(TEAK_LOGS);
                    plugin.incrementLogsGained();
                    sleep(50, 100);
                }
                break;
                
            case RANDOM:
                // Random drop pattern
                if (Rs2Inventory.hasItem(TEAK_LOGS) && Math.random() < 0.3) {
                    Rs2Inventory.dropAll(TEAK_LOGS);
                    plugin.incrementLogsGained();
                }
                break;
        }
        
        state = JstWCState.TWO_TICK_CUTTING;
    }
    
    private void handleEating() {
        nextAction = "Eating food...";
        
        if (!needsFood()) {
            state = JstWCState.TWO_TICK_CUTTING;
            return;
        }
        
        String foodName = config.foodName();
        if (Rs2Inventory.hasItem(foodName)) {
            Rs2Inventory.interact(foodName, "Eat");
            sleepUntil(() -> !Rs2Player.isAnimating(), 3000);
        } else {
            nextAction = "No food found! Please bring " + foodName;
            state = JstWCState.ERROR;
        }
    }
    
    private void regainAggression() {
        nextAction = "Regaining bird aggression using curse spells...";
        
        // Check for other players before regaining aggression
        if (config.enableWorldHopping() && shouldWorldHop()) {
            Microbot.log("JstWC: Other players detected while regaining aggression, initiating world hop");
            state = JstWCState.WORLD_HOPPING;
            return;
        }
        
        Microbot.log("JstWC: Lost bird aggression, recasting curse spells to regain it");
        aggressionSetup = false;
        attackingBirdsCount = 0; // Reset count to force fresh setup
        state = JstWCState.SETTING_UP_AGGRESSION;
    }
    
    private void handleWaiting() {
        nextAction = "Waiting...";
        sleep(1000);
        state = JstWCState.TWO_TICK_CUTTING;
    }
    
    private void handleError() {
        // Error state - wait and try to recover
        sleep(5000);
        state = JstWCState.INITIALIZING;
    }
    
    private void updateAggressionStatus() {
        // Count NPCs that are attacking the player
        attackingBirdsCount = 0;
        List<Rs2NpcModel> allNpcs = Rs2Npc.getNpcs().collect(Collectors.toList());
        for (Rs2NpcModel npc : allNpcs) {
            if (Arrays.stream(BIRD_IDS).anyMatch(id -> id == npc.getId()) && 
                npc.getInteracting() == Microbot.getClient().getLocalPlayer()) {
                attackingBirdsCount++;
            }
        }
        
        // Track ticks since last hit for aggression timing (using game tick timing)
        ticksSinceLastHit++;
    }
    
    private boolean hasAxe() {
        try {
            // Check equipped axe
            if (Rs2Equipment.isWearing(BRONZE_AXE, IRON_AXE, STEEL_AXE, BLACK_AXE, 
                                        MITHRIL_AXE, ADAMANT_AXE, RUNE_AXE, DRAGON_AXE)) {
                return true;
            }
            // Check inventory for axe
            return Rs2Inventory.hasItem(BRONZE_AXE, IRON_AXE, STEEL_AXE, BLACK_AXE, 
                                       MITHRIL_AXE, ADAMANT_AXE, RUNE_AXE, DRAGON_AXE);
        } catch (Exception e) {
            Microbot.log("JstWC: Error checking axe: " + e.getMessage());
            return false;
        }
    }
    
    private boolean hasProperWeapon() {
        try {
            if (config.useHuntersCrossbow()) {
                boolean hasCrossbow = Rs2Equipment.isWearing(HUNTERS_CROSSBOW, BRONZE_CROSSBOW, IRON_CROSSBOW);
                boolean hasShield = Rs2Equipment.isWearing(WOODEN_SHIELD, BRONZE_SQ_SHIELD, IRON_SQ_SHIELD);
                return hasCrossbow && hasShield;
            } else {
                boolean hasBow = Rs2Equipment.isWearing(SHORTBOW, OAK_SHORTBOW, WILLOW_SHORTBOW, 
                                                         MAPLE_SHORTBOW, YEW_SHORTBOW, MAGIC_SHORTBOW);
                boolean hasNoArrows = !Rs2Inventory.hasItem("arrow") && !Rs2Equipment.isWearing("arrow");
                return hasBow && hasNoArrows;
            }
        } catch (Exception e) {
            Microbot.log("JstWC: Error checking weapon: " + e.getMessage());
            return false;
        }
    }
    
    private boolean needsFood() {
        int currentHp = Rs2Player.getBoostedSkillLevel(net.runelite.api.Skill.HITPOINTS);
        int maxHp = Rs2Player.getRealSkillLevel(net.runelite.api.Skill.HITPOINTS);
        return currentHp < (maxHp * config.healthThreshold() / 100.0);
    }
    
    private boolean shouldDropLogs() {
        if (!config.dropLogs()) return false;
        
        int logCount = Rs2Inventory.count(TEAK_LOGS);
        int keepAmount = config.keepLogsAmount();
        
        return logCount > keepAmount;
    }
    
    private boolean shouldAlternatingDrop() {
        return config.dropPattern() == JstWCConfig.DropPattern.ALTERNATING && 
               Rs2Inventory.count(TEAK_LOGS) > config.keepLogsAmount();
    }
    
    private void handleCheckingEquipment() {
        nextAction = "Validating equipment requirements...";
        Microbot.log("JstWC: Checking equipment");
        
        try {
            // Check if user wants to skip validation
            if (config.skipEquipmentValidation()) {
                Microbot.log("JstWC: Equipment validation skipped by user - assuming all equipment is ready");
                if (ISLE_OF_SOULS_AREA.contains(Rs2Player.getWorldLocation())) {
                    Microbot.log("JstWC: At location, setting up aggression");
                    state = JstWCState.SETTING_UP_AGGRESSION;
                } else {
                    Microbot.log("JstWC: Preparing for travel");
                    state = JstWCState.PREPARING_FOR_TRAVEL;
                }
                return;
            }
            
            boolean hasValidEquipment = equipmentManager.validateEquipment();
            Microbot.log("JstWC: Has valid equipment: " + hasValidEquipment);
            
            if (hasValidEquipment) {
                // All equipment present, check location
                if (ISLE_OF_SOULS_AREA.contains(Rs2Player.getWorldLocation())) {
                    Microbot.log("JstWC: Equipment OK, at location, setting up aggression");
                    state = JstWCState.SETTING_UP_AGGRESSION;
                } else {
                    Microbot.log("JstWC: Equipment OK, preparing for travel");
                    state = JstWCState.PREPARING_FOR_TRAVEL;
                }
            } else {
                // Missing equipment
                List<String> missingItems = equipmentManager.getMissingEquipment();
                Microbot.log("JstWC: Missing equipment: " + String.join(", ", missingItems));
                
                boolean enableAutoBuy = config.enableAutoBuy();
                boolean needsBanking = equipmentManager.needsBanking();
                
                Microbot.log("JstWC: Auto-buy enabled: " + enableAutoBuy + ", Needs banking: " + needsBanking);
                
                // Temporarily disable auto-buy due to GE opening issues
                // Focus on manual setup for now
                if (false && enableAutoBuy && needsBanking) {
                    // Try banking first
                    Microbot.log("JstWC: Banking for equipment");
                    state = JstWCState.BANKING_FOR_EQUIPMENT;
                } else if (false && enableAutoBuy) {
                    // Go straight to buying
                    Microbot.log("JstWC: Buying equipment");
                    state = JstWCState.BUYING_EQUIPMENT;
                } else {
                    nextAction = "Manual setup required. Missing equipment: " + String.join(", ", missingItems) + 
                               ". Please obtain these items and restart the script, enable auto-buy, or enable 'Skip Equipment Validation' if you have everything.";
                    Microbot.log("JstWC: Auto-buy disabled, stopping. Missing: " + String.join(", ", missingItems));
                    Microbot.log("JstWC: Required items for 2-tick woodcutting:");
                    Microbot.log("JstWC: - Any axe (equipped or inventory)");
                    Microbot.log("JstWC: - Shortbow (equipped, no arrows) OR Hunter's crossbow + shield");
                    Microbot.log("JstWC: - Food (configured type: " + config.foodName() + ")");
                    Microbot.log("JstWC: - Runes for Confuse/Weaken spells (Mind, Water, Earth)");
                    if (config.useRegenBracelet()) {
                        Microbot.log("JstWC: - Regen bracelet (optional but recommended)");
                    }
                    Microbot.log("JstWC: If you have all items, enable 'Skip Equipment Validation' in Auto-Buy Settings");
                    state = JstWCState.ERROR;
                }
            }
        } catch (Exception e) {
            Microbot.log("JstWC: Error checking equipment: " + e.getMessage());
            e.printStackTrace();
            nextAction = "Error checking equipment: " + e.getMessage();
            state = JstWCState.ERROR;
        }
    }
    
    private void handleBankingForEquipment() {
        nextAction = "Banking for equipment...";
        Microbot.log("JstWC: Banking for equipment");
        
        // Get configured banking location
        BankLocation bankLocation = getBankLocation();
        
        if (!Rs2Bank.isOpen()) {
            // Check if we're close to a bank first
            double distanceToBank = Rs2Player.getWorldLocation().distanceTo(bankLocation.getWorldPoint());
            Microbot.log("JstWC: Distance to bank: " + distanceToBank);
            
            if (distanceToBank > 15) {
                // Too far from bank, walk to it first
                nextAction = "Walking to bank...";
                Microbot.log("JstWC: Walking to bank at: " + bankLocation.getWorldPoint());
                if (!Rs2Walker.walkTo(bankLocation.getWorldPoint())) {
                    Microbot.log("JstWC: Failed to walk to bank, trying to buy instead");
                    state = JstWCState.BUYING_EQUIPMENT;
                }
                return;
            }
            
            // Close to bank, try to open it
            boolean success = false;
            int attempts = 0;
            
            while (!success && attempts < 3) {
                if (Rs2Bank.useBank()) {
                    success = true;
                    Microbot.log("JstWC: Bank opened successfully");
                } else {
                    attempts++;
                    Microbot.log("JstWC: Failed to open bank, attempt " + attempts + "/3");
                    sleep(1200, 2000);
                }
            }
            
            if (!success) {
                errorRecovery.recordError(state, "Failed to open bank after 3 attempts");
                errorRecovery.recordRetryAttempt(state);
                state = JstWCState.RETRYING;
                return;
            }
        }
        
        // Bank is open, withdraw equipment
        if (equipmentManager.withdrawEquipmentFromBank()) {
            Rs2Bank.closeBank();
            sleep(600, 800);
            
            // Re-validate equipment
            if (equipmentManager.validateEquipment()) {
                errorRecovery.recordSuccess(state);
                state = JstWCState.PREPARING_FOR_TRAVEL;
            } else {
                // Still missing items, need to buy
                state = JstWCState.BUYING_EQUIPMENT;
            }
        } else {
            // Failed to withdraw, but don't error out - proceed to buying
            Microbot.log("Some items not found in bank, proceeding to buy missing items");
            Rs2Bank.closeBank();
            sleep(600, 800);
            state = JstWCState.BUYING_EQUIPMENT;
        }
    }
    
    private void handleBuyingEquipment() {
        nextAction = "Buying equipment from Grand Exchange...";
        Microbot.log("JstWC: Buying equipment from GE");
        
        // Check if we're close to GE
        WorldPoint geLocation = BankLocation.VARROCK_WEST.getWorldPoint();
        double distanceToGE = Rs2Player.getWorldLocation().distanceTo(geLocation);
        Microbot.log("JstWC: Distance to GE: " + distanceToGE);
        
        // Make sure we're at the GE
        if (!Rs2GrandExchange.isOpen()) {
            if (distanceToGE > 20) {
                // Too far from GE, walk there first
                nextAction = "Walking to Grand Exchange...";
                Microbot.log("JstWC: Walking to GE at: " + geLocation);
                if (!Rs2Walker.walkTo(geLocation)) {
                    errorRecovery.recordError(state, "Failed to walk to GE");
                    state = JstWCState.RETRYING;
                    return;
                }
                sleep(1200, 2000);
                return;
            }
            
            // Close to GE, try to open it
            Microbot.log("JstWC: Attempting to open Grand Exchange");
            
            // First check if any interfaces are blocking
            if (Rs2Bank.isOpen()) {
                Microbot.log("JstWC: Bank is open, closing it first");
                Rs2Bank.closeBank();
                sleep(1000, 1500);
            }
            
            if (!Rs2GrandExchange.openExchange()) {
                // We're at GE but can't open it - skip GE buying for now
                Microbot.log("JstWC: Cannot open Grand Exchange, skipping equipment buying");
                nextAction = "Cannot access Grand Exchange - please manually obtain missing equipment or move closer to GE";
                state = JstWCState.ERROR;
                return;
            }
        }
        
        // Purchase equipment
        JstWCEquipmentManager.PurchaseResult result = equipmentManager.purchaseEquipment();
        
        switch (result) {
            case SUCCESS:
                errorRecovery.recordSuccess(state);
                sleep(2000, 3000);
                state = JstWCState.COLLECTING_PURCHASES;
                break;
                
            case INSUFFICIENT_GOLD:
                errorRecovery.recordError(state, "Insufficient gold to buy equipment");
                state = JstWCState.ERROR;
                nextAction = "Not enough gold to buy required equipment";
                break;
                
            case PRICE_TOO_HIGH:
                nextAction = "Some items too expensive, trying alternatives...";
                if (errorRecovery.shouldRetry(state)) {
                    errorRecovery.recordRetryAttempt(state);
                    sleep(2000, 3000);
                    // Equipment manager will try alternatives on next attempt
                } else {
                    state = JstWCState.ERROR;
                    nextAction = "Cannot afford required equipment";
                }
                break;
                
            case ITEM_UNAVAILABLE:
                nextAction = "Some items unavailable, trying alternatives...";
                if (errorRecovery.shouldRetry(state)) {
                    errorRecovery.recordRetryAttempt(state);
                    sleep(2000, 3000);
                } else {
                    state = JstWCState.ERROR;
                    nextAction = "Required items not available on GE";
                }
                break;
                
            case GE_LIMIT_REACHED:
                errorRecovery.recordError(state, "GE limit reached");
                state = JstWCState.ERROR;
                nextAction = "No free GE slots - please collect existing offers";
                break;
                
            case FAILED:
            default:
                if (errorRecovery.shouldRetry(state)) {
                    errorRecovery.recordRetryAttempt(state);
                    nextAction = "Purchase failed, retrying...";
                    sleep(2000, 3000);
                } else {
                    state = JstWCState.ERROR;
                    nextAction = "Failed to purchase equipment";
                }
                break;
        }
    }
    
    private void handleCollectingPurchases() {
        nextAction = "Collecting purchases...";
        
        if (!Rs2GrandExchange.isOpen()) {
            Rs2GrandExchange.openExchange();
            return;
        }
        
        if (equipmentManager.collectPurchases()) {
            Rs2GrandExchange.closeExchange();
            sleep(600, 800);
            
            // Re-validate equipment
            if (equipmentManager.validateEquipment()) {
                state = JstWCState.PREPARING_FOR_TRAVEL;
            } else {
                nextAction = "Failed to collect all purchases";
                state = JstWCState.ERROR;
            }
        }
    }
    
    private void handlePreparingForTravel() {
        nextAction = "Preparing for travel to Isle of Souls...";
        
        // Open bank to prepare inventory
        BankLocation bankLocation = getBankLocation();
        
        if (!Rs2Bank.isOpen()) {
            boolean success = false;
            int attempts = 0;
            
            while (!success && attempts < 3) {
                if (Rs2Bank.useBank()) {
                    success = true;
                } else {
                    attempts++;
                    if (Rs2Player.getWorldLocation().distanceTo(bankLocation.getWorldPoint()) > 10) {
                        if (!Rs2Walker.walkTo(bankLocation.getWorldPoint())) {
                            errorRecovery.recordError(state, "Failed to walk to bank for travel prep");
                            state = JstWCState.RETRYING;
                            return;
                        }
                        sleep(1200, 2000);
                    } else {
                        sleep(1000, 1500);
                    }
                }
            }
            
            if (!success) {
                errorRecovery.recordError(state, "Failed to open bank for travel preparation");
                if (errorRecovery.shouldRetry(state)) {
                    errorRecovery.recordRetryAttempt(state);
                    state = JstWCState.RETRYING;
                } else {
                    state = JstWCState.ERROR;
                    nextAction = "Cannot access bank for travel preparation";
                }
                return;
            }
        }
        
        // Deposit unnecessary items
        Rs2Bank.depositAllExcept(item -> 
            item.getName().contains("axe") ||
            item.getName().contains("bow") ||
            item.getName().contains("crossbow") ||
            item.getName().contains("shield") ||
            item.getName().equals(config.foodName()) ||
            item.getName().contains("rune") ||
            item.getName().equals("Regen bracelet") ||
            item.getName().equals("Coins")
        );
        sleep(600, 800);
        
        // Ensure we have travel supplies if needed
        if (config.travelMethod() == JstWCConfig.TravelMethod.CHARTER_SHIP) {
            // Make sure we have coins for charter
            if (Rs2Inventory.count("Coins") < 5000 && Rs2Bank.hasItem("Coins")) {
                Rs2Bank.withdrawX("Coins", 10000);
                sleep(600, 800);
            }
        }
        
        Rs2Bank.closeBank();
        sleep(600, 800);
        
        // Equip items if needed
        equipItems();
        
        state = JstWCState.TRAVELING_TO_LOCATION;
    }
    
    private void handleTravelingToLocation() {
        nextAction = "Traveling to Isle of Souls...";
        
        // Check if already at location
        if (ISLE_OF_SOULS_AREA.contains(Rs2Player.getWorldLocation())) {
            state = JstWCState.SETTING_UP_AGGRESSION;
            return;
        }
        
        switch (config.travelMethod()) {
            case CHARTER_SHIP:
                travelViaCharterShip();
                break;
            case DIGSITE_PENDANT:
                travelViaDigsitePendant();
                break;
            case SKILLS_NECKLACE:
                travelViaSkillsNecklace();
                break;
        }
    }
    
    private void travelViaCharterShip() {
        // Implementation would involve:
        // 1. Walking to Port Sarim
        // 2. Finding charter ship NPC
        // 3. Selecting Isle of Souls destination
        // 4. Confirming travel
        
        // For now, use basic walking as fallback
        Rs2Walker.walkTo(OPTIMAL_POSITION);
        sleep(3000, 5000);
    }
    
    private void travelViaDigsitePendant() {
        // Implementation would involve:
        // 1. Using Digsite pendant teleport
        // 2. Walking to Fossil Island boat
        // 3. Taking boat to Isle of Souls
        
        // For now, use basic walking as fallback
        Rs2Walker.walkTo(OPTIMAL_POSITION);
        sleep(3000, 5000);
    }
    
    private void travelViaSkillsNecklace() {
        // Implementation would involve:
        // 1. Using Skills necklace to Farming Guild
        // 2. Walking south to Isle of Souls
        
        // For now, use basic walking as fallback
        Rs2Walker.walkTo(OPTIMAL_POSITION);
        sleep(3000, 5000);
    }
    
    private BankLocation getBankLocation() {
        switch (config.bankingLocation()) {
            case VARROCK_WEST:
                return BankLocation.VARROCK_WEST;
            case EDGEVILLE:
                return BankLocation.EDGEVILLE;
            case FALADOR_WEST:
                return BankLocation.FALADOR_WEST;
            case DRAYNOR:
                return BankLocation.DRAYNOR_VILLAGE;
            default:
                return BankLocation.VARROCK_WEST;
        }
    }
    
    private void equipItems() {
        // Equip axe if in inventory - try common axe names
        String[] axes = {"Dragon axe", "Rune axe", "Adamant axe", "Mithril axe", "Steel axe", "Iron axe", "Bronze axe"};
        Rs2Inventory.equip(axes);
        sleep(600, 800);
        
        // Equip weapon
        if (config.useHuntersCrossbow()) {
            Rs2Inventory.equip("Hunter's crossbow", "Iron crossbow", "Bronze crossbow");
            sleep(600, 800);
            Rs2Inventory.equip("Wooden shield", "Bronze sq shield", "Iron sq shield");
            sleep(600, 800);
        } else {
            String[] bows = {"Yew shortbow", "Magic shortbow", "Maple shortbow", "Willow shortbow", "Oak shortbow", "Shortbow"};
            Rs2Inventory.equip(bows);
            sleep(600, 800);
        }
        
        // Equip regen bracelet if available
        if (config.useRegenBracelet()) {
            Rs2Inventory.equip("Regen bracelet");
            sleep(600, 800);
        }
    }
    
    // Getter methods for overlay
    public JstWCState getCurrentState() {
        return state;
    }
    
    public String getNextAction() {
        return nextAction;
    }
    
    public boolean hasAggression() {
        return attackingBirdsCount >= 2;
    }
    
    public int getAttackingBirdsCount() {
        return attackingBirdsCount;
    }
    
    private void handleRetrying() {
        JstWCState stateToRetry = errorRecovery.getStateBeforeError();
        
        if (stateToRetry == null) {
            nextAction = "No previous state to retry, returning to initialization";
            state = JstWCState.INITIALIZING;
            return;
        }
        
        nextAction = "Retrying " + stateToRetry.getDescription() + 
                    " (attempt " + (errorRecovery.getRemainingRetries(stateToRetry)) + " remaining)";
        
        // Wait for retry delay
        sleep(2000, 3000);
        
        // Clear any UI elements that might be blocking
        if (Rs2Bank.isOpen()) {
            Rs2Bank.closeBank();
            sleep(600, 800);
        }
        if (Rs2GrandExchange.isOpen()) {
            Rs2GrandExchange.closeExchange();
            sleep(600, 800);
        }
        
        // Return to the state that failed
        state = stateToRetry;
        
        Microbot.log("Retrying state: " + stateToRetry + " (Error: " + errorRecovery.getLastError() + ")");
    }
    
    private boolean shouldWorldHop() {
        // Don't hop if we just hopped recently
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastHopTime < HOP_COOLDOWN) {
            return false;
        }
        
        // Count other players in the Isle of Souls area
        int playerCount = countPlayersInArea();
        Microbot.log("JstWC: Players in area: " + playerCount + " (max allowed: " + config.maxPlayersBeforeHop() + ")");
        
        return playerCount > config.maxPlayersBeforeHop();
    }
    
    private int countPlayersInArea() {
        // Expanded area to detect players approaching the woodcutting spot
        WorldArea expandedArea = new WorldArea(2182, 2991, 10, 10, 0); // Slightly larger detection area
        
        return (int) Rs2Player.getPlayers(player -> {
            if (player.getPlayer() == Microbot.getClient().getLocalPlayer()) {
                return false; // Exclude ourselves
            }
            WorldPoint playerLocation = player.getWorldLocation();
            
            // Check if player is in the main area or approaching
            if (ISLE_OF_SOULS_AREA.contains(playerLocation) || expandedArea.contains(playerLocation)) {
                // Additional check: if player has woodcutting equipment, they're likely here to woodcut
                if (hasWoodcuttingEquipment(player.getPlayer())) {
                    Microbot.log("JstWC: Detected player with woodcutting equipment: " + player.getPlayer().getName() + 
                               " at location: " + playerLocation);
                    return true;
                }
                // Count any player in the immediate area regardless of equipment
                if (ISLE_OF_SOULS_AREA.contains(playerLocation)) {
                    Microbot.log("JstWC: Detected player in woodcutting area: " + player.getPlayer().getName() + 
                               " at location: " + playerLocation);
                    return true;
                }
            }
            return false;
        }).count();
    }
    
    private void handleWorldHopping() {
        nextAction = "Looking for a less crowded world...";
        Microbot.log("JstWC: Initiating world hop");
        
        // Find a suitable world
        World targetWorld = findSuitableWorld();
        if (targetWorld == null) {
            Microbot.log("JstWC: No suitable world found, continuing in current world");
            state = JstWCState.POSITIONING; // Continue with current world
            return;
        }
        
        // Attempt to hop to the world
        Microbot.log("JstWC: Attempting to hop to world " + targetWorld.getId());
        if (Microbot.hopToWorld(targetWorld.getId())) {
            Microbot.log("JstWC: Successfully initiated hop to world " + targetWorld.getId());
            lastHopTime = System.currentTimeMillis();
            
            // Wait for the hop to complete
            sleepUntil(() -> Microbot.getClient().getGameState() == GameState.LOGGED_IN, 15000);
            sleep(2000, 3000);
            
            // Reset aggression setup flag since we're in a new world
            aggressionSetup = false;
            attackingBirdsCount = 0;
            
            // Go back to setting up aggression in the new world
            state = JstWCState.SETTING_UP_AGGRESSION;
        } else {
            Microbot.log("JstWC: Failed to hop worlds, continuing in current world");
            state = JstWCState.POSITIONING; // Continue with setup
        }
    }
    
    private World findSuitableWorld() {
        net.runelite.api.World[] worldArray = Microbot.getClient().getWorldList();
        if (worldArray == null) return null;
        
        int currentWorldId = Microbot.getClient().getWorld();
        List<World> suitableWorlds = new ArrayList<>();
        
        for (net.runelite.api.World world : worldArray) {
            // Skip current world
            if (world.getId() == currentWorldId) continue;
            
            // Skip restricted worlds
            if (world.getTypes().contains(net.runelite.api.WorldType.PVP) ||
                world.getTypes().contains(net.runelite.api.WorldType.HIGH_RISK) ||
                world.getTypes().contains(net.runelite.api.WorldType.DEADMAN) ||
                world.getTypes().contains(net.runelite.api.WorldType.SKILL_TOTAL) ||
                world.getTypes().contains(net.runelite.api.WorldType.QUEST_SPEEDRUNNING) ||
                world.getTypes().contains(net.runelite.api.WorldType.TOURNAMENT_WORLD)) {
                continue;
            }
            
            // Only hop to member worlds if we're in a member world
            boolean isCurrentWorldMembers = Microbot.getClient().getWorldType().contains(net.runelite.api.WorldType.MEMBERS);
            boolean isTargetWorldMembers = world.getTypes().contains(net.runelite.api.WorldType.MEMBERS);
            
            if (isCurrentWorldMembers && !isTargetWorldMembers) {
                continue; // Don't hop from members to f2p
            }
            if (!isCurrentWorldMembers && isTargetWorldMembers) {
                continue; // Don't hop from f2p to members
            }
            
            // Prefer worlds with lower player counts
            World httpWorld = World.builder()
                .id(world.getId())
                .players(world.getPlayerCount())
                .location(world.getLocation())
                .activity(world.getActivity())
                .types(world.getTypes().contains(net.runelite.api.WorldType.MEMBERS) ? 
                       EnumSet.of(net.runelite.http.api.worlds.WorldType.MEMBERS) : 
                       EnumSet.noneOf(net.runelite.http.api.worlds.WorldType.class))
                .address("oldschool" + (world.getId() - 300) + ".runescape.com")
                .build();
            
            suitableWorlds.add(httpWorld);
        }
        
        // Sort by player count (ascending) and return the least populated
        suitableWorlds.sort((w1, w2) -> Integer.compare(w1.getPlayers(), w2.getPlayers()));
        
        // Return a world with reasonable population (not completely empty, not too crowded)
        for (World world : suitableWorlds) {
            if (world.getPlayers() >= 50 && world.getPlayers() <= 800) {
                return world;
            }
        }
        
        // If no world in the ideal range, return the least populated
        return suitableWorlds.isEmpty() ? null : suitableWorlds.get(0);
    }
    
    private boolean hasWoodcuttingEquipment(Player player) {
        if (player == null) return false;
        
        try {
            // Check if player has visible axe equipped or bow (signs of 2-tick woodcutting setup)
            PlayerComposition composition = player.getPlayerComposition();
            if (composition == null) return false;
            
            // Check for equipped axe (weapon slot)
            int weaponId = composition.getEquipmentId(KitType.WEAPON);
            if (isAxe(weaponId)) {
                return true;
            }
            
            // Check for bow (common in 2-tick setups)
            if (isBow(weaponId)) {
                return true;
            }
            
            // If we can't determine equipment, assume they might be here for woodcutting
            // (Better to be safe and hop than risk competition)
            return false;
        } catch (Exception e) {
            // If we can't check equipment, assume they might be competing
            return true;
        }
    }
    
    private boolean isAxe(int itemId) {
        return itemId == BRONZE_AXE || itemId == IRON_AXE || itemId == STEEL_AXE || 
               itemId == BLACK_AXE || itemId == MITHRIL_AXE || itemId == ADAMANT_AXE || 
               itemId == RUNE_AXE || itemId == DRAGON_AXE;
    }
    
    private boolean isBow(int itemId) {
        return itemId == SHORTBOW || itemId == OAK_SHORTBOW || itemId == WILLOW_SHORTBOW || 
               itemId == MAPLE_SHORTBOW || itemId == YEW_SHORTBOW || itemId == MAGIC_SHORTBOW ||
               itemId == HUNTERS_CROSSBOW || itemId == BRONZE_CROSSBOW || itemId == IRON_CROSSBOW;
    }
    
    private boolean castCurseSpellOnBirds(List<Rs2NpcModel> birds, MagicAction spell) {
        int successfulCasts = 0;
        int targetBirds = Math.min(2, birds.size()); // We need at least 2 birds for proper 2-tick aggression
        
        for (Rs2NpcModel bird : birds.subList(0, targetBirds)) {
            if (bird.getInteracting() != Microbot.getClient().getLocalPlayer()) {
                try {
                    Microbot.log("JstWC: Casting " + spell.name() + " on bird ID " + bird.getId() + " at " + bird.getWorldLocation());
                    
                    if (Rs2Magic.castOn(spell, bird.getRuneliteNpc())) {
                        successfulCasts++;
                        Microbot.log("JstWC: Successfully cast " + spell.name() + " on bird (" + successfulCasts + "/" + targetBirds + ")");
                        sleep(600, 800); // Wait for spell animation and effect
                        
                        // Verify the bird is now targeting us
                        if (bird.getInteracting() == Microbot.getClient().getLocalPlayer()) {
                            Microbot.log("JstWC: Bird is now aggressive - spell worked!");
                        }
                    } else {
                        Microbot.log("JstWC: Failed to cast " + spell.name() + " on bird - retrying...");
                        // Retry once
                        sleep(300, 500);
                        if (Rs2Magic.castOn(spell, bird.getRuneliteNpc())) {
                            successfulCasts++;
                            sleep(600, 800);
                        }
                    }
                } catch (Exception e) {
                    Microbot.log("JstWC: Exception while casting spell: " + e.getMessage());
                }
            } else {
                // Bird is already aggressive
                successfulCasts++;
                Microbot.log("JstWC: Bird already targeting player (" + successfulCasts + "/" + targetBirds + ")");
            }
        }
        
        boolean success = successfulCasts >= 2;
        Microbot.log("JstWC: Curse spell casting result - " + successfulCasts + "/" + targetBirds + " birds aggressive. Success: " + success);
        return success;
    }
    
    private boolean hasRequiredCurseRunes() {
        // Check if we have runes for either Weaken or Confuse spells
        int mindCount = Rs2Inventory.count("Mind rune");
        int waterCount = Rs2Inventory.count("Water rune");
        int earthCount = Rs2Inventory.count("Earth rune");
        
        // Check for staff alternatives that provide unlimited runes
        boolean hasWaterStaff = Rs2Equipment.isWearing("Staff of water", "Mud staff", "Steam staff", "Mist staff");
        boolean hasEarthStaff = Rs2Equipment.isWearing("Staff of earth", "Mud staff", "Lava staff", "Dust staff");
        
        // Weaken requires: 1 Mind, 3 Water, 2 Earth
        boolean canCastWeaken = mindCount >= 1 && 
                               (waterCount >= 3 || hasWaterStaff) && 
                               (earthCount >= 2 || hasEarthStaff);
        
        // Confuse requires: 2 Mind, 3 Water, 3 Earth  
        boolean canCastConfuse = mindCount >= 2 && 
                                (waterCount >= 3 || hasWaterStaff) && 
                                (earthCount >= 3 || hasEarthStaff);
        
        return canCastWeaken || canCastConfuse;
    }
    
    @Override
    public void shutdown() {
        if (errorRecovery != null) {
            errorRecovery.reset();
        }
        Rs2Antiban.resetAntibanSettings();
        super.shutdown();
    }
}