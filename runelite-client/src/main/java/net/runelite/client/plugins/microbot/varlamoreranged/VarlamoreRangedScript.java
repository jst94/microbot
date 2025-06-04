package net.runelite.client.plugins.microbot.varlamoreranged;

import com.google.inject.Inject;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

public class VarlamoreRangedScript extends Script {
    private State currentState = State.TUTORIAL_COMPLETE;
    private VarlamoreRangedConfig config;

    @Inject
    public VarlamoreRangedScript() {
        // Default constructor for dependency injection
    }

    @Override
    public boolean run() {
        mainLoop();
        return true;
    }
    
    public boolean run(VarlamoreRangedConfig config) {
        // Store config for use throughout the script
        this.config = config;
        mainLoop();
        return true;
    }

    private void mainLoop() {
        while (true) {
            try {
                if (!super.run()) break;
                switch (currentState) {
                    case TUTORIAL_COMPLETE:
                        handleTutorialComplete();
                        break;
                    case WALK_TO_GE:
                        walkToGrandExchange();
                        break;
                    case WAIT_FOR_TRADE:
                        waitForTrade();
                        break;
                    case BUY_BOND:
                        buyBond();
                        break;
                    case REDEEM_BOND:
                        redeemBond();
                        break;
                    case BUY_EQUIPMENT:
                        buyEquipment();
                        break;
                    case START_QUEST:
                        startQuest();
                        break;
                    case COMPLETE_QUEST:
                        completeQuest();
                        break;
                    case TRAIN_RANGED:
                        trainRanged();
                        break;
                }
                sleep(1000);
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }
    }    private void handleTutorialComplete() {
        if (Rs2Player.isInTutorialIsland()) {
            Microbot.showMessage("Completing Tutorial Island...");
            
            // Use the professional tutorial completion logic based on varbit progress
            int tutorialProgress = Microbot.getVarbitPlayerValue(281);
            
            // Handle dialogues first - this is critical for tutorial progression
            if (Rs2Dialogue.hasContinue()) {
                Rs2Dialogue.clickContinue();
                return;
            }
            
            if (Rs2Dialogue.hasSelectAnOption()) {
                // Handle quest-specific dialogue options first
                if (Rs2Dialogue.handleQuestOptionDialogueSelection()) {
                    return;
                } else {
                    // Select first available option if no quest helper highlighting
                    Rs2Dialogue.keyPressForDialogueOption(1);
                    return;
                }
            }
            
            // Safety check - don't continue if player is moving or animating
            if (Rs2Player.isMoving() || Rs2Player.isAnimating() || Microbot.pauseAllScripts) {
                return;
            }
            
            // Handle different tutorial phases based on varbit progress
            if (tutorialProgress < 10) {
                // Getting Started - Gielinor Guide
                handleGettingStarted();
            } else if (tutorialProgress >= 10 && tutorialProgress < 120) {
                // Survival Guide
                handleSurvivalGuide();
            } else if (tutorialProgress >= 120 && tutorialProgress < 200) {
                // Cooking Guide  
                handleCookingGuide();
            } else if (tutorialProgress >= 200 && tutorialProgress <= 250) {
                // Quest Guide
                handleQuestGuide();
            } else if (tutorialProgress >= 260 && tutorialProgress <= 360) {
                // Mining Guide
                handleMiningGuide();
            } else if (tutorialProgress > 360 && tutorialProgress < 510) {
                // Combat Guide
                handleCombatGuide();
            } else if (tutorialProgress >= 510 && tutorialProgress < 540) {
                // Banker Guide
                handleBankerGuide();
            } else if (tutorialProgress >= 540 && tutorialProgress < 610) {
                // Prayer Guide
                handlePrayerGuide();
            } else if (tutorialProgress >= 610 && tutorialProgress < 1000) {
                // Magic Guide
                handleMagicGuide();
            } else if (tutorialProgress >= 1000) {
                // Tutorial complete!
                Microbot.showMessage("Tutorial Island completed successfully!");
                currentState = State.WALK_TO_GE;
                return;
            }
            
            sleep(600); // Brief pause between actions
        } else {
            // Player is no longer on Tutorial Island
            Microbot.showMessage("Tutorial Island complete!");
            currentState = State.WALK_TO_GE;
        }
    }
    
    private void handleGettingStarted() {
        var npc = Rs2Npc.getNpc("Gielinor Guide");
        if (npc != null && !Rs2Dialogue.isInDialogue()) {
            if (Rs2Npc.interact(npc, "Talk-to")) {
                sleepUntil(Rs2Dialogue::isInDialogue, 3000);
            }
        }
    }
    
    private void handleSurvivalGuide() {
        var npc = Rs2Npc.getNpc("Survival Expert");
        if (npc != null && !Rs2Dialogue.isInDialogue()) {
            if (Rs2Npc.interact(npc, "Talk-to")) {
                sleepUntil(Rs2Dialogue::isInDialogue, 3000);
            }
        }
    }
    
    private void handleCookingGuide() {
        var npc = Rs2Npc.getNpc("Master Chef");
        if (npc != null && !Rs2Dialogue.isInDialogue()) {
            if (Rs2Npc.interact(npc, "Talk-to")) {
                sleepUntil(Rs2Dialogue::isInDialogue, 3000);
            }
        }
    }
    
    private void handleQuestGuide() {
        var npc = Rs2Npc.getNpc("Quest Guide");
        if (npc != null && !Rs2Dialogue.isInDialogue()) {
            if (Rs2Npc.interact(npc, "Talk-to")) {
                sleepUntil(Rs2Dialogue::isInDialogue, 3000);
            }
        }
    }
    
    private void handleMiningGuide() {
        var npc = Rs2Npc.getNpc("Mining Instructor");
        if (npc != null && !Rs2Dialogue.isInDialogue()) {
            if (Rs2Npc.interact(npc, "Talk-to")) {
                sleepUntil(Rs2Dialogue::isInDialogue, 3000);
            }
        }
    }
    
    private void handleCombatGuide() {
        var npc = Rs2Npc.getNpc("Combat Instructor");
        if (npc != null && !Rs2Dialogue.isInDialogue()) {
            if (Rs2Npc.interact(npc, "Talk-to")) {
                sleepUntil(Rs2Dialogue::isInDialogue, 3000);
            }
        }
    }
    
    private void handleBankerGuide() {
        var npc = Rs2Npc.getNpc("Account Guide");
        if (npc != null && !Rs2Dialogue.isInDialogue()) {
            if (Rs2Npc.interact(npc, "Talk-to")) {
                sleepUntil(Rs2Dialogue::isInDialogue, 3000);
            }
        }
    }
    
    private void handlePrayerGuide() {
        var npc = Rs2Npc.getNpc("Brother Brace");
        if (npc != null && !Rs2Dialogue.isInDialogue()) {
            if (Rs2Npc.interact(npc, "Talk-to")) {
                sleepUntil(Rs2Dialogue::isInDialogue, 3000);
            }
        }
    }
    
    private void handleMagicGuide() {
        var npc = Rs2Npc.getNpc("Magic Instructor");
        if (npc != null && !Rs2Dialogue.isInDialogue()) {
            if (Rs2Npc.interact(npc, "Talk-to")) {
                sleepUntil(Rs2Dialogue::isInDialogue, 3000);
            }
        }
    }

    private void walkToGrandExchange() {
        Microbot.showMessage("Walking to Grand Exchange...");
        Rs2Walker.walkTo(new WorldPoint(3164, 3488, 0)); // GE coordinates
        
        // Wait until we're close to the GE
        sleepUntil(() -> {
            WorldPoint playerLocation = Rs2Player.getWorldLocation();
            WorldPoint geLocation = new WorldPoint(3164, 3488, 0);
            return playerLocation.distanceTo(geLocation) < 10;
        }, 30000);
        
        Microbot.showMessage("Arrived at Grand Exchange!");
        currentState = State.WAIT_FOR_TRADE;
    }

    private void waitForTrade() {
        Microbot.showMessage("Waiting for trade or manual coin acquisition...");
        
        // Check if player already has sufficient coins (15M for bond + equipment)
        int currentCoins = Rs2Inventory.count("Coins");
        if (currentCoins >= 15000000) {
            Microbot.showMessage("Sufficient coins detected: " + currentCoins);
            currentState = State.BUY_BOND;
            return;
        }
        
        // Wait for trade or manual interaction (10 minute timeout for larger amount)
        long startTime = System.currentTimeMillis();
        long timeout = 10 * 60 * 1000; // 10 minutes
        
        while (System.currentTimeMillis() - startTime < timeout) {
            currentCoins = Rs2Inventory.count("Coins");
            if (currentCoins >= 15000000) {
                Microbot.showMessage("Trade received! Coins: " + currentCoins);
                currentState = State.BUY_BOND;
                return;
            }
            sleep(5000); // Check every 5 seconds
        }
        
        // If timeout reached, still proceed (maybe player has coins in bank)
        Microbot.showMessage("Trade timeout reached, proceeding to bond purchase...");
        currentState = State.BUY_BOND;
    }

    private void buyBond() {
        Microbot.showMessage("Starting bond purchase...");
        
        // Open Grand Exchange
        if (!Rs2GrandExchange.openExchange()) {
            Microbot.showMessage("Failed to open Grand Exchange");
            return;
        }
        
        // Check if we already have a bond
        if (Rs2Inventory.hasItem("Old school bond")) {
            Microbot.showMessage("Bond already in inventory, proceeding to redeem...");
            currentState = State.REDEEM_BOND;
            return;
        }
        
        // Buy bond at current market price + 5%
        Microbot.showMessage("Buying Old school bond...");
        if (Rs2GrandExchange.buyItemAbove5Percent("Old school bond", 1)) {
            Microbot.showMessage("Bond purchase initiated");
            
            // Wait for purchase to complete (up to 2 minutes)
            Microbot.showMessage("Waiting for bond purchase to complete...");
            long startTime = System.currentTimeMillis();
            long timeout = 2 * 60 * 1000; // 2 minutes
            
            while (System.currentTimeMillis() - startTime < timeout) {
                if (Rs2Inventory.hasItem("Old school bond")) {
                    Microbot.showMessage("Bond purchase complete!");
                    Rs2GrandExchange.closeExchange();
                    currentState = State.REDEEM_BOND;
                    return;
                }
                sleep(5000);
            }
            
            // If timeout, check for completion one more time
            if (Rs2Inventory.hasItem("Old school bond")) {
                currentState = State.REDEEM_BOND;
            } else {
                Microbot.showMessage("Bond purchase timeout - may need manual intervention");
                // Still proceed in case it completes later
                currentState = State.REDEEM_BOND;
            }
        } else {
            Microbot.showMessage("Failed to initiate bond purchase - proceeding anyway");
            currentState = State.REDEEM_BOND;
        }
        
        Rs2GrandExchange.closeExchange();
    }

    private void redeemBond() {
        Microbot.showMessage("Redeeming bond for membership...");
        
        // Check if we have a bond to redeem
        if (!Rs2Inventory.hasItem("Old school bond")) {
            Microbot.showMessage("No bond found in inventory, skipping to equipment purchase");
            currentState = State.BUY_EQUIPMENT;
            return;
        }
        
        // Right-click bond and select "Redeem"
        if (Rs2Inventory.interact("Old school bond", "Redeem")) {
            Microbot.showMessage("Bond redemption initiated...");
            sleep(2000);
            
            // Handle redemption dialog if it appears
            if (Rs2Dialogue.hasContinue()) {
                Rs2Dialogue.clickContinue();
                sleep(1000);
            }
            
            // Confirm redemption if asked
            if (Rs2Dialogue.hasSelectAnOption()) {
                Rs2Dialogue.keyPressForDialogueOption(1); // Usually "Yes" option
                sleep(2000);
            }
            
            // Wait for membership to activate
            Microbot.showMessage("Waiting for membership activation...");
            sleep(5000);
            
            // Check if we're now a member (this is a basic check)
            // In a real implementation, you'd check the member status properly
            if (!Rs2Inventory.hasItem("Old school bond")) {
                Microbot.showMessage("Bond redeemed successfully! Now a member.");
            } else {
                Microbot.showMessage("Bond redemption may have failed, but continuing...");
            }
        } else {
            Microbot.showMessage("Failed to interact with bond, continuing anyway...");
        }
        
        currentState = State.BUY_EQUIPMENT;
    }

    private void buyEquipment() {
        Microbot.showMessage("Starting Grand Exchange equipment purchases...");
        
        // Open Grand Exchange
        if (!Rs2GrandExchange.openExchange()) {
            Microbot.showMessage("Failed to open Grand Exchange");
            return;
        }
        
        // Buy Oak shortbow (using buyItemAbove5Percent for quick purchase)
        Microbot.showMessage("Buying Oak shortbow...");
        if (Rs2GrandExchange.buyItemAbove5Percent("Oak shortbow", 1)) {
            Microbot.showMessage("Oak shortbow purchase initiated");
        } else {
            Microbot.showMessage("Failed to buy Oak shortbow");
        }
        
        // Buy Bronze arrows
        Microbot.showMessage("Buying Bronze arrows...");
        if (Rs2GrandExchange.buyItemAbove5Percent("Bronze arrow", 500)) {
            Microbot.showMessage("Bronze arrows purchase initiated");
        } else {
            Microbot.showMessage("Failed to buy Bronze arrows");
        }
        
        // Wait for all buy offers to complete
        Microbot.showMessage("Waiting for purchases to complete...");
        sleepUntil(Rs2GrandExchange::hasFinishedBuyingOffers, 60000);
        
        // Collect items from GE to inventory
        Microbot.showMessage("Collecting items from Grand Exchange...");
        Rs2GrandExchange.collectToInventory();
        sleep(2000);
        
        Rs2GrandExchange.closeExchange();
        Microbot.showMessage("Equipment purchases complete!");
        currentState = State.START_QUEST;
    }
    
    private void startQuest() {
        // Start the "Children of the Sun" quest - proper implementation
        Microbot.showMessage("Starting Children of the Sun quest...");
        
        // Talk to Alina east of Varrock Square to start the quest
        Microbot.showMessage("Walking to Alina east of Varrock Square...");
        Rs2Walker.walkTo(new WorldPoint(3225, 3426, 0)); // Alina's location
        sleep(2000);
        
        var alina = Rs2Npc.getNpc("Alina");
        if (alina != null) {
            Microbot.showMessage("Talking to Alina to start the quest...");
            Rs2Npc.interact(alina, "Talk-to");
            sleepUntil(Rs2Dialogue::isInDialogue, 5000);
              // Handle dialogue options for quest start
            if (Rs2Dialogue.isInDialogue()) {
                if (Rs2Dialogue.hasContinue()) {
                    Rs2Dialogue.clickContinue();
                } else if (Rs2Dialogue.hasSelectAnOption()) {
                    Rs2Dialogue.keyPressForDialogueOption("When will this delegation arrive?");
                    sleep(1000);
                    if (Rs2Dialogue.hasContinue()) {
                        Rs2Dialogue.clickContinue();
                    } else if (Rs2Dialogue.hasSelectAnOption()) {
                        Rs2Dialogue.keyPressForDialogueOption("Yes.");
                    }
                }
                sleep(2000);
                Microbot.showMessage("Children of the Sun quest started!");
            }
        }
        
        // Follow the guard phase - key coordinates from quest helper
        Microbot.showMessage("Following guard quietly...");
        WorldPoint[] guardPath = {
            new WorldPoint(3225, 3429, 0),
            new WorldPoint(3233, 3429, 0), 
            new WorldPoint(3233, 3427, 0),
            new WorldPoint(3240, 3417, 0),
            new WorldPoint(3241, 3403, 0),
            new WorldPoint(3236, 3392, 0),
            new WorldPoint(3247, 3397, 0)
        };
        
        for (WorldPoint point : guardPath) {
            Rs2Walker.walkTo(point);
            sleep(2000); // Wait between movements to simulate stealth
        }
        
        // Attempt to enter the house (part of quest progression)
        Microbot.showMessage("Attempting to enter suspicious house...");
        Rs2Walker.walkTo(new WorldPoint(3259, 3400, 0)); // House location
        sleep(2000);
          // Talk to Sergeant Tobyn in Varrock Square
        Microbot.showMessage("Reporting to Sergeant Tobyn...");
        Rs2Walker.walkTo(new WorldPoint(3211, 3437, 0)); // Tobyn's location
        sleep(2000);
          var tobyn = Rs2Npc.getNpc("Sergeant Tobyn");
        if (tobyn == null) {
            tobyn = Rs2Npc.getNpc("Guard Sergeant");
        }
        if (tobyn != null) {
            Rs2Npc.interact(tobyn, "Talk-to");
            sleepUntil(Rs2Dialogue::isInDialogue, 3000);
            if (Rs2Dialogue.isInDialogue()) {
                Rs2Dialogue.clickContinue();
            }
        }
        
        Microbot.showMessage("Guard marking phase initiated...");
        currentState = State.COMPLETE_QUEST;
    }
    
    private void completeQuest() {
        // Complete the Children of the Sun quest - Guard marking puzzle
        Microbot.showMessage("Starting guard marking puzzle...");
        
        // Mark the 4 correct guards based on quest helper data
        WorldPoint[] correctGuards = {
            new WorldPoint(3208, 3422, 0), // Guard 1 - outside Aris's tent
            new WorldPoint(3221, 3430, 0), // Guard 2 - south east of Benny's news stand
            new WorldPoint(3246, 3429, 0), // Guard 3 - with mace north-west of Varrock East Bank
            new WorldPoint(3237, 3427, 0)  // Guard 4 - leaning on north wall of Lowe's Archery Emporium
        };
        
        for (int i = 0; i < correctGuards.length; i++) {
            Microbot.showMessage("Marking guard " + (i + 1) + "/4...");
            Rs2Walker.walkTo(correctGuards[i]);
            sleep(2000);
            
            // Look for guards to mark
            var guard = Rs2Npc.getNpc("Guard");
            if (guard != null) {
                Rs2Npc.interact(guard, "Mark");
                sleep(1500);
            }
        }
          // Report back to Sergeant Tobyn after marking guards
        Microbot.showMessage("Reporting back to Sergeant Tobyn...");
        Rs2Walker.walkTo(new WorldPoint(3211, 3437, 0)); // Tobyn in Varrock Square
        sleep(2000);
        
        var tobyn2 = Rs2Npc.getNpc("Sergeant Tobyn");
        if (tobyn2 == null) {
            tobyn2 = Rs2Npc.getNpc("Guard Sergeant");
        }
        if (tobyn2 != null) {
            Rs2Npc.interact(tobyn2, "Talk-to");
            sleepUntil(Rs2Dialogue::isInDialogue, 3000);
            if (Rs2Dialogue.isInDialogue()) {
                Rs2Dialogue.clickContinue();
            }
        }
        
        // Go to Varrock Castle roof to finish the quest
        Microbot.showMessage("Going to Varrock Castle roof to finish quest...");
        Rs2Walker.walkTo(new WorldPoint(3212, 3474, 0)); // Varrock Castle stairs
        sleep(2000);
        
        // Go up stairs to first floor
        Rs2Walker.walkTo(new WorldPoint(3224, 3472, 1)); // Ladder to second floor
        sleep(2000);
        
        // Go up ladder to roof
        Rs2Walker.walkTo(new WorldPoint(3202, 3473, 2)); // Tobyn on roof
        sleep(2000);
        
        // Complete quest with Tobyn on roof
        var roofTobyn = Rs2Npc.getNpc("Sergeant Tobyn");
        if (roofTobyn == null) {
            roofTobyn = Rs2Npc.getNpc("Guard Sergeant");
        }
        if (roofTobyn != null) {
            Rs2Npc.interact(roofTobyn, "Talk-to");
            sleepUntil(Rs2Dialogue::isInDialogue, 3000);
            if (Rs2Dialogue.isInDialogue()) {
                Rs2Dialogue.clickContinue();
                Microbot.showMessage("Children of the Sun quest completed!");
            }
        }
        
        currentState = State.TRAIN_RANGED;
    }

    private void trainRanged() {
        Microbot.showMessage("Starting ranged training at Varlamore chickens...");
        
        // Walk to Varlamore chicken training area
        WorldArea trainingArea = new WorldArea(1555, 3109, 11, 13, 0); // x, y, width, height, plane
        WorldPoint centerPoint = new WorldPoint(1560, 3115, 0);
        
        Microbot.showMessage("Walking to Varlamore chicken training area...");
        Rs2Walker.walkTo(centerPoint);
        sleepUntil(() -> {
            WorldPoint playerLocation = Rs2Player.getWorldLocation();
            return trainingArea.contains(playerLocation);
        }, 30000);
        
        // Equip ranged equipment before training
        equipRangedGear();
        
        Microbot.showMessage("Beginning ranged training combat loop...");
        
        // Main training loop
        while (shouldContinueTraining()) {
            try {
                // Check if we need to move back to training area
                WorldPoint playerLocation = Rs2Player.getWorldLocation();
                if (!trainingArea.contains(playerLocation)) {
                    Microbot.showMessage("Returning to training area...");
                    Rs2Walker.walkTo(centerPoint);
                    sleep(2000);
                    continue;
                }
                
                // Check if already in combat
                if (Rs2Player.isInCombat()) {
                    sleep(1000);
                    continue;
                }
                
                // Find and attack a chicken
                var chicken = Rs2Npc.getNpc("Chicken");
                if (chicken != null && chicken.getWorldLocation().distanceTo(playerLocation) <= 10) {
                    if (Rs2Npc.attack(chicken)) {
                        Microbot.showMessage("Attacking chicken...");
                        // Wait for combat to start
                        sleepUntil(() -> Rs2Player.isInCombat(), 3000);
                        
                        // Wait for combat to finish or timeout after 30 seconds
                        sleepUntil(() -> !Rs2Player.isInCombat(), 30000);
                    }
                } else {
                    // No chicken found nearby, wait a bit and try again
                    sleep(2000);
                }
                
                // Check our ranged level progress
                int currentRangedLevel = Microbot.getClient().getRealSkillLevel(net.runelite.api.Skill.RANGED);
                if (currentRangedLevel % 5 == 0) { // Every 5 levels, show progress
                    Microbot.showMessage("Ranged level: " + currentRangedLevel + "/20");
                }
                
                // Brief pause between attacks
                sleep(1000);
                
            } catch (Exception ex) {
                System.out.println("Error in training loop: " + ex.getMessage());
                sleep(2000);
            }
        }
        
        // Training complete
        int finalLevel = Microbot.getClient().getRealSkillLevel(net.runelite.api.Skill.RANGED);
        Microbot.showMessage("Ranged training complete! Final level: " + finalLevel);
    }
    
    private void equipRangedGear() {
        Microbot.showMessage("Equipping ranged gear...");
        
        // Equip Oak shortbow
        if (Rs2Inventory.hasItem("Oak shortbow")) {
            Rs2Inventory.equip("Oak shortbow");
            sleep(1000);
        }
        
        // Equip Bronze arrows
        if (Rs2Inventory.hasItem("Bronze arrow")) {
            Rs2Inventory.equip("Bronze arrow");
            sleep(1000);
        }
        
        Microbot.showMessage("Ranged gear equipped!");
    }
    
    private boolean shouldContinueTraining() {
        // Use configurable stop level, default to 20 if config not available
        int targetLevel = config != null ? config.stopAtLevel() : 20;
        
        // Check if we've reached target level
        int currentLevel = Microbot.getClient().getRealSkillLevel(net.runelite.api.Skill.RANGED);
        if (currentLevel >= targetLevel) {
            return false;
        }
        
        // Check if we still have arrows
        if (!Rs2Inventory.hasItem("Bronze arrow")) {
            Microbot.showMessage("Out of Bronze arrows!");
            return false;
        }
        
        return true;
    }

    public void shutdown() {
        super.shutdown();
        // Cleanup logic
    }

    public enum State {
        TUTORIAL_COMPLETE,
        WALK_TO_GE,
        WAIT_FOR_TRADE,
        BUY_BOND,
        REDEEM_BOND,
        BUY_EQUIPMENT,
        START_QUEST,
        COMPLETE_QUEST,
        TRAIN_RANGED
    }
}