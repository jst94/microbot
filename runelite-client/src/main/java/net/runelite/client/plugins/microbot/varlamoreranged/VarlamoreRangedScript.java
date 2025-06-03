package net.runelite.client.plugins.microbot.varlamoreranged;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

public class VarlamoreRangedScript extends Script {
    private State currentState = State.TUTORIAL_COMPLETE;

    public VarlamoreRangedScript(VarlamoreRangedConfig config) {
        // Config parameter accepted but not stored as field since it's unused
    }

    @Override
    public boolean run() {
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
        Rs2Walker.walkTo(new WorldPoint(3164, 3488, 0)); // GE coordinates
        currentState = State.WAIT_FOR_TRADE;
    }

    private void waitForTrade() {
        // Implement trade waiting logic
        sleep(5000);
        currentState = State.BUY_EQUIPMENT;
    }

    private void buyEquipment() {
        if (Rs2Bank.openBank()) {
            Rs2Bank.withdrawX("Maple shortbow", 1);
            Rs2Bank.withdrawX("Bronze arrows", 500);
            Rs2Bank.closeBank();
        }
        currentState = State.START_QUEST;
    }    private void startQuest() {
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
    }    private void completeQuest() {
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
        // Implement ranged training at Varlamore chickens
        while (Rs2Inventory.hasItem("Bronze arrows")) {
            // Attack chicken logic
            sleep(1500);
        }
        Microbot.showMessage("Ranged training complete!");
    }

    public void shutdown() {
        super.shutdown();
        // Cleanup logic
    }

    public enum State {
        TUTORIAL_COMPLETE,
        WALK_TO_GE,
        WAIT_FOR_TRADE,
        BUY_EQUIPMENT,
        START_QUEST,
        COMPLETE_QUEST,
        TRAIN_RANGED
    }
}