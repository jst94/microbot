package net.runelite.client.plugins.microbot.jstwinter;

import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import static net.runelite.api.ObjectID.*;
import java.util.stream.Collectors;
import java.util.Arrays;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.breakhandler.BreakHandlerScript;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.antiban.enums.Activity;
import net.runelite.client.plugins.microbot.util.antiban.enums.PlayStyle;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.microbot.util.woodcutting.Rs2Woodcutting;
import net.runelite.client.plugins.microbot.jstwinter.enums.JstState;
import net.runelite.client.plugins.microbot.jstwinter.enums.JstBrazier;
import net.runelite.api.World;
import net.runelite.api.WorldType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.runelite.api.Constants.GAME_TICK_LENGTH;
import static net.runelite.api.ItemID.*;
import static net.runelite.api.ObjectID.*;
import static net.runelite.client.plugins.microbot.util.Global.sleepUntilTrue;
import static net.runelite.client.plugins.microbot.util.player.Rs2Player.eatAt;

public class JstWinterScript extends Script {
    public static String version = "1.0.0";
    public static JstWinterScript instance;

    public static JstState state = JstState.BANKING;
    public static boolean resetActions = false;
    static JstWinterConfig config;
    static JstWinterPlugin plugin;
    private static boolean lockState = false;
    private static long stateStartTime = System.currentTimeMillis();
    private static final long STATE_TIMEOUT_MS = 30000; // 30 second timeout for stuck states
    
    // Wintertodt locations
    final WorldPoint BOSS_ROOM = new WorldPoint(1630, 3982, 0);
    final WorldPoint BANK_LOCATION = new WorldPoint(1640, 3944, 0);
    final WorldPoint CRATE_LOCATION = new WorldPoint(1634, 3982, 0);
    final WorldPoint HERB_LOCATION = new WorldPoint(1635, 3978, 0);
    
    String axe = "";
    int wintertodtHp = -1;
    int currentPoints = 0;
    boolean init = false;
    private GameObject currentBrazier;
    private NPC damagedPyromancer;
    JstBrazier currentSelectedBrazier;
    private long lastHopTime = 0;
    private int consecutiveCrashes = 0;
    private long lastCrashTime = 0;
    private long lastBrazierSwitchTime = 0;
    private int switchCooldownMs = 30000; // 30 second cooldown between switches

    private static void changeState(JstState scriptState) {
        changeState(scriptState, false);
    }

    private static void changeState(JstState scriptState, boolean lock) {
        if (state == scriptState || lockState) return;
        System.out.println("JstWinter: Changing state from " + state + " to " + scriptState);
        state = scriptState;
        resetActions = true;
        lockState = lock;
        stateStartTime = System.currentTimeMillis();
    }

    private static void setLockState(JstState state, boolean lock) {
        if (lockState == lock) return;
        lockState = lock;
        System.out.println("JstWinter: State " + state.toString() + " set lockState to " + lockState);
    }

    public static void onHitsplatApplied(HitsplatApplied hitsplatApplied) {
        Actor actor = hitsplatApplied.getActor();
        if (actor != Microbot.getClient().getLocalPlayer()) {
            return;
        }
        resetActions = true;
    }

    public boolean run(JstWinterConfig config, JstWinterPlugin plugin) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) {
                    init = false;
                    return;
                }
                if (!super.run()) return;
                if (Rs2AntibanSettings.actionCooldownActive) return;
                
                // Check for state timeout
                if (System.currentTimeMillis() - stateStartTime > STATE_TIMEOUT_MS) {
                    Microbot.log("JstWinter: State " + state + " timed out, resetting");
                    lockState = false;
                    resetActions = true;
                    changeState(JstState.WAITING);
                }
                
                // Handle random events - Only pause if there's actually a random event targeting the player
                if (Rs2Widget.hasWidget("What would you like to do?") || Rs2Widget.hasWidget("Please wait")) {
                    Microbot.log("JstWinter: Random event interface detected, pausing script");
                    return;
                }

                long startTime = System.currentTimeMillis();

                if (!init) {
                    Microbot.log("JstWinter: Starting script...");
                    JstWinterScript.instance = this;
                    JstWinterScript.config = config;
                    JstWinterScript.plugin = plugin;
                    Rs2Antiban.resetAntibanSettings();
                    Rs2Antiban.antibanSetupTemplates.applyGeneralBasicSetup();
                    Rs2Antiban.setActivity(Activity.GENERAL_WOODCUTTING);
                    Rs2Antiban.setPlayStyle(PlayStyle.EXTREME_AGGRESSIVE);
                    state = JstState.BANKING;
                    sleep(2000);
                    
                    if (!validateInventory()) return;
                    init = true;
                }

                // Check game state
                boolean wintertodtRespawning = Rs2Widget.hasWidget("returns in");
                boolean isWintertodtAlive = Rs2Widget.hasWidget("Wintertodt's Energy");
                updateWintertodtHp(isWintertodtAlive);
                updatePoints();
                
                // Check if we should hop worlds (but not during critical states)
                if (config.enableWorldHopping() && shouldHopWorlds() && 
                    state != JstState.BANKING && state != JstState.HEAL_PYROMANCER && 
                    state != JstState.FIX_BRAZIER && !lockState) {
                    handleWorldHop();
                    return;
                }
                
                // Find game objects - dynamic brazier selection
                JstBrazier selectedBrazier = config.smartBrazierSelection() ? selectOptimalBrazier() : config.brazierLocation();
                currentBrazier = Rs2GameObject.getGameObject(BRAZIER_29312, selectedBrazier.getBrazierLocation());
                GameObject brokenBrazier = Rs2GameObject.getGameObject(BRAZIER_29313, selectedBrazier.getBrazierLocation());
                GameObject burningBrazier = Rs2GameObject.getGameObject(BURNING_BRAZIER_29314, selectedBrazier.getBrazierLocation());
                
                // Update current selected brazier for state management
                if (config.smartBrazierSelection()) {
                    currentSelectedBrazier = selectedBrazier;
                }
                
                // Check if we need to heal pyromancer
                if (config.healPyromancer()) {
                    checkPyromancer();
                }
                
                // Check player status
                boolean playerIsLowWarmth = getWarmthLevel() < config.warmthThreshold();
                boolean needBanking = shouldBank(playerIsLowWarmth, isWintertodtAlive);
                
                // Emergency checks
                if (Rs2Player.getBoostedSkillLevel(Skill.HITPOINTS) <= 0) {
                    Microbot.log("JstWinter: Player died, resetting to banking");
                    lockState = false;
                    changeState(JstState.BANKING);
                    return;
                }
                
                // Check if we're in the correct region
                if (!isInWintertodtRegion() && !isNearBank() 
                    && state != JstState.BANKING && state != JstState.ENTER_ROOM) {
                    Microbot.log("JstWinter: Not in Wintertodt area, returning to bank");
                    changeState(JstState.BANKING);
                    return;
                }

                // Handle dialog
                if (Rs2Widget.hasWidget("Leave and lose all progress")) {
                    Rs2Keyboard.typeString("1");
                    sleep(1600, 2000);
                    return;
                }

                // Drop unnecessary items
                dropUnnecessaryItems();
                
                // Check if we should eat (more conservative in solo mode)
                if (shouldEat()) {
                    return;
                }
                
                // Dodge snowfall
                if (config.dodgeSnowfall()) {
                    dodgeSnowfall();
                }

                // Check if we need to make potions first (before any activity)
                if (config.useRejuvenationPotions() && isWintertodtAlive) {
                    int potionCount = Rs2Inventory.count(ItemID.REJUVENATION_POTION_1) + 
                                    Rs2Inventory.count(ItemID.REJUVENATION_POTION_2) +
                                    Rs2Inventory.count(ItemID.REJUVENATION_POTION_3) +
                                    Rs2Inventory.count(ItemID.REJUVENATION_POTION_4);
                    // Always maintain at least 3 potions before starting activities
                    if (potionCount < 3 && state != JstState.BANKING) {
                        Microbot.log("JstWinter: Need to make potions first (have " + potionCount + ", need 3 minimum)");
                        changeState(JstState.BANKING);
                        return;
                    }
                }
                
                // Main game logic
                if (!needBanking) {
                    if (!isWintertodtAlive) {
                        if (state != JstState.ENTER_ROOM && state != JstState.WAITING && state != JstState.BANKING) {
                            setLockState(JstState.WAITING, false);
                            changeState(JstState.WAITING);
                        }
                    } else {
                        handleMainGameLoop(burningBrazier, brokenBrazier);
                    }
                } else {
                    setLockState(JstState.BANKING, false);
                    changeState(JstState.BANKING);
                }

                // Handle states
                switch (state) {
                    case BANKING:
                        if (!handleBanking()) return;
                        
                        // Check if we have enough supplies
                        boolean hasEnoughSupplies = false;
                        if (config.useRejuvenationPotions()) {
                            // For rejuvenation potions, we just need basic supplies to enter
                            hasEnoughSupplies = true; // We can make potions inside, so just need basic tools
                        } else {
                            hasEnoughSupplies = Rs2Inventory.hasItemAmount(config.food().getId(), config.foodAmount());
                        }
                        
                        if (Rs2Player.isFullHealth() && hasEnoughSupplies) {
                            plugin.setTimesBanked(plugin.getTimesBanked() + 1);
                            if (Rs2Antiban.takeMicroBreakByChance() || BreakHandlerScript.isBreakActive())
                                break;
                            changeState(JstState.ENTER_ROOM);
                        }
                        break;
                        
                    case ENTER_ROOM:
                        if (!BreakHandlerScript.isLockState() && !BreakHandlerScript.isBreakActive())
                            BreakHandlerScript.setLockState(true);
                        
                        // Final potion check before entering room
                        if (config.useRejuvenationPotions()) {
                            int potionCount = Rs2Inventory.count(ItemID.REJUVENATION_POTION_1) + 
                                            Rs2Inventory.count(ItemID.REJUVENATION_POTION_2) +
                                            Rs2Inventory.count(ItemID.REJUVENATION_POTION_3) +
                                            Rs2Inventory.count(ItemID.REJUVENATION_POTION_4);
                            if (potionCount < 3) {
                                Microbot.log("JstWinter: Not enough potions to enter room safely");
                                changeState(JstState.BANKING);
                                break;
                            }
                        }
                        
                        if (!wintertodtRespawning && !isWintertodtAlive) {
                            Rs2Walker.walkTo(BOSS_ROOM, 12);
                        } else {
                            state = JstState.WAITING;
                        }
                        break;

                    case WAITING:
                        walkToBrazier();
                        if (shouldLightBrazier(isWintertodtAlive, needBanking, burningBrazier, currentBrazier)) {
                            changeState(JstState.LIGHT_BRAZIER);
                        }
                        break;
                        
                    case LIGHT_BRAZIER:
                        if (currentBrazier != null && !Rs2Player.isAnimating()) {
                            if (Rs2GameObject.interact(currentBrazier, "Light")) {
                                sleepGaussian(600, 150);
                            }
                            return;
                        }
                        break;
                        
                    case CHOP_ROOTS:
                        // Check if inventory is full and we have items to burn
                        if (Rs2Inventory.isFull() && hasItemsToBurn()) {
                            Microbot.log("JstWinter: Inventory full, walking back to brazier");
                            walkToBrazier();
                            setLockState(JstState.CHOP_ROOTS, false);
                            return;
                        }
                        
                        // Check for crashers at roots
                        if (config.enableWorldHopping() && config.hopOnCrash() && isBeingCrashed(BRUMA_ROOTS)) {
                            handleCrash();
                            return;
                        }
                        
                        if (Rs2Woodcutting.isWearingAxeWithSpecialAttack()) {
                            Rs2Combat.setSpecState(true, 1000);
                        }
                        if (!Rs2Player.isAnimating()) {
                            if (Rs2GameObject.interact(BRUMA_ROOTS, "Chop")) {
                                sleepUntil(Rs2Player::isAnimating, 2000);
                                resetActions = false;
                                Rs2Antiban.actionCooldown();
                            }
                        }
                        break;
                        
                    case FLETCH_KINDLING:
                        if (Rs2Player.getAnimation() != AnimationID.FLETCHING_BOW_CUTTING || resetActions) {
                            walkToBrazier();
                            
                            // Check if we still have roots to fletch
                            if (!Rs2Inventory.hasItem(BRUMA_ROOT)) {
                                Microbot.log("JstWinter: No more roots to fletch, walking to brazier");
                                walkToBrazier();
                                setLockState(JstState.FLETCH_KINDLING, false);
                                return;
                            }
                            
                            Rs2ItemModel knife = Rs2Inventory.get("knife");
                            if (knife != null && knife.getSlot() != 27) {
                                sleep(GAME_TICK_LENGTH * 2);
                                if (Rs2Inventory.moveItemToSlot(knife, 27))
                                    sleepUntil(() -> Rs2Inventory.slotContains(27, "knife"), 5000);
                            }
                            Rs2Inventory.combineClosest(KNIFE, BRUMA_ROOT);
                            resetActions = false;
                            sleep(GAME_TICK_LENGTH);
                            sleepUntil(() -> Rs2Player.getAnimation() != AnimationID.FLETCHING_BOW_CUTTING, 2000);
                            Rs2Antiban.actionCooldown();
                            
                            // Continue fletching if we have more roots, otherwise walk to brazier
                            if (!Rs2Inventory.hasItem(BRUMA_ROOT)) {
                                Microbot.log("JstWinter: Finished fletching all roots, walking to brazier");
                                walkToBrazier();
                                setLockState(JstState.FLETCH_KINDLING, false);
                            }
                        }
                        break;
                        
                    case FEED_BRAZIER:
                        if (!Microbot.isGainingExp || resetActions) {
                            selectedBrazier = config.smartBrazierSelection() ? selectOptimalBrazier() : config.brazierLocation();
                            GameObject burningBrazierObj = Rs2GameObject.getGameObject(BURNING_BRAZIER_29314, selectedBrazier.getBrazierLocation());
                            if (brokenBrazier != null && config.fixBrazier()) {
                                changeState(JstState.FIX_BRAZIER);
                                return;
                            }
                            if (burningBrazierObj == null && currentBrazier != null && config.relightBrazier()) {
                                changeState(JstState.LIGHT_BRAZIER);
                                return;
                            }
                            if (burningBrazierObj != null && burningBrazierObj.getWorldLocation().distanceTo(Rs2Player.getWorldLocation()) < 10 && hasItemsToBurn()) {
                                if (Rs2GameObject.interact(burningBrazierObj, "Feed")) {
                                    Microbot.log("JstWinter: Feeding brazier");
                                    resetActions = false;
                                    sleep(GAME_TICK_LENGTH * 3);
                                    Rs2Antiban.actionCooldown();
                                } else {
                                    Microbot.log("JstWinter: Failed to interact with brazier");
                                    resetActions = true;
                                }
                            }
                        }
                        break;
                        
                    case FIX_BRAZIER:
                        if (!Rs2Player.isAnimating()) {
                            selectedBrazier = config.smartBrazierSelection() ? selectOptimalBrazier() : config.brazierLocation();
                            GameObject broken = Rs2GameObject.getGameObject(BRAZIER_29313, selectedBrazier.getBrazierLocation());
                            if (broken != null) {
                                if (Rs2GameObject.interact(broken, "Fix")) {
                                    Microbot.log("JstWinter: Fixing brazier");
                                    sleepGaussian(300, 50);
                                    resetActions = false;
                                } else {
                                    Microbot.log("JstWinter: Failed to fix brazier");
                                    walkToBrazier();
                                }
                            } else {
                                changeState(JstState.FEED_BRAZIER);
                            }
                        }
                        break;
                        
                    case HEAL_PYROMANCER:
                        if (damagedPyromancer != null && !damagedPyromancer.isDead()) {
                            if (Rs2Npc.interact(damagedPyromancer, "Help")) {
                                Microbot.log("JstWinter: Healing pyromancer");
                                sleepUntil(() -> Rs2Player.isAnimating(), 2000);
                                sleepUntil(() -> !Rs2Player.isAnimating() || damagedPyromancer == null, 5000);
                                resetActions = false;
                            }
                        } else {
                            Microbot.log("JstWinter: Pyromancer no longer needs healing");
                            changeState(JstState.FEED_BRAZIER);
                        }
                        break;
                        
                    case MAKE_POTIONS:
                        if (!handleMakePotions()) return;
                        
                        // Check if we have enough potions now
                        int potionCount = Rs2Inventory.count(ItemID.REJUVENATION_POTION_1) + 
                                        Rs2Inventory.count(ItemID.REJUVENATION_POTION_2) +
                                        Rs2Inventory.count(ItemID.REJUVENATION_POTION_3) +
                                        Rs2Inventory.count(ItemID.REJUVENATION_POTION_4);
                        if (potionCount >= 3) {
                            Microbot.log("JstWinter: Made enough potions (" + potionCount + "), resuming activity");
                            changeState(JstState.WAITING);
                        }
                        break;
                }

                long endTime = System.currentTimeMillis();
                long totalTime = endTime - startTime;
                System.out.println("JstWinter: Loop time " + totalTime + "ms");

            } catch (Exception ex) {
                Microbot.log("JstWinter error: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
        return true;
    }

    private void handleMainGameLoop(GameObject burningBrazier, GameObject brokenBrazier) {
        // Solo mode strategy
        if (config.soloMode()) {
            handleSoloStrategy(burningBrazier, brokenBrazier);
            return;
        }
        
        // Normal mode priority: Heal pyromancer > Fix brazier > Light brazier > Feed brazier > Fletch > Chop
        if (config.pyromancerPriority() && damagedPyromancer != null) {
            changeState(JstState.HEAL_PYROMANCER, true);
            return;
        }
        
        if (brokenBrazier != null && config.fixBrazier()) {
            changeState(JstState.FIX_BRAZIER, true);
            return;
        }
        
        if (burningBrazier == null && currentBrazier != null && config.relightBrazier()) {
            changeState(JstState.LIGHT_BRAZIER, true);
            return;
        }
        
        // Check if we're close to minimum points and Wintertodt is almost dead
        if (isWintertodtAlmostDead() && currentPoints < config.minimumPoints()) {
            // Prioritize point-gaining activities
            if (hasItemsToBurn()) {
                changeState(JstState.FEED_BRAZIER, true);
                return;
            }
        }
        
        if (shouldChopRoots()) {
            changeState(JstState.CHOP_ROOTS, true);
            return;
        }
        
        if (shouldFletchRoots()) {
            changeState(JstState.FLETCH_KINDLING, true);
            return;
        }
        
        if (shouldFeedBrazier()) {
            changeState(JstState.FEED_BRAZIER, true);
        }
    }
    
    private void handleSoloStrategy(GameObject burningBrazier, GameObject brokenBrazier) {
        // Solo strategy: maximize points by keeping WT alive as long as possible
        int targetPoints = config.soloTargetPoints();
        
        // Always prioritize pyromancer in solo
        if (damagedPyromancer != null) {
            changeState(JstState.HEAL_PYROMANCER, true);
            return;
        }
        
        // Fix brazier if needed
        if (brokenBrazier != null && config.fixBrazier()) {
            changeState(JstState.FIX_BRAZIER, true);
            return;
        }
        
        // Light brazier if needed
        if (burningBrazier == null && currentBrazier != null) {
            changeState(JstState.LIGHT_BRAZIER, true);
            return;
        }
        
        // Solo strategy: Stop feeding when we have enough points
        if (currentPoints >= targetPoints) {
            Microbot.log("JstWinter: Reached target points (" + currentPoints + "/" + targetPoints + "), waiting for game end");
            // Just maintain brazier and wait
            if (burningBrazier == null && currentBrazier != null) {
                changeState(JstState.LIGHT_BRAZIER, true);
            } else {
                changeState(JstState.WAITING);
            }
            return;
        }
        
        // If WT health is very low and we don't have enough points, feed aggressively
        if (wintertodtHp < 5 && currentPoints < targetPoints) {
            if (hasItemsToBurn()) {
                changeState(JstState.FEED_BRAZIER, true);
                return;
            }
        }
        
        // Solo fletching strategy: Always fletch for maximum points
        if (Rs2Inventory.hasItem(BRUMA_ROOT) && Rs2Inventory.hasItem(KNIFE)) {
            // Fletch at safe spot
            if (Rs2Player.getWorldLocation().distanceTo(config.brazierLocation().getSafeLocation()) > 3) {
                walkToBrazier();
                return;
            }
            changeState(JstState.FLETCH_KINDLING, true);
            return;
        }
        
        // Feed if we have kindling
        if (Rs2Inventory.hasItem(BRUMA_KINDLING) && burningBrazier != null) {
            changeState(JstState.FEED_BRAZIER, true);
            return;
        }
        
        // Chop more roots
        if (shouldChopRoots()) {
            changeState(JstState.CHOP_ROOTS, true);
        }
    }

    private boolean validateInventory() {
        Microbot.log("JstWinter: Validating inventory...");
        if (config.axeInInventory()) {
            if (!Rs2Inventory.hasItem("axe")) {
                Microbot.showMessage("JstWinter: Axe not found in inventory!");
                sleep(5000);
                return false;
            }
            axe = Rs2Inventory.get("axe").getName();
        } else if (!Rs2Equipment.isWearing("axe")) {
            if (Rs2Inventory.hasItem("axe")) {
                Rs2Inventory.wear("axe");
                sleepUntil(() -> Rs2Equipment.isWearing("axe"));
                return false;
            }
            Microbot.showMessage("JstWinter: Please equip an axe!");
            sleep(5000);
            return false;
        }
        return true;
    }

    private void updateWintertodtHp(boolean isWintertodtAlive) {
        Widget wintertodtHealthbar = Rs2Widget.getWidget(396, 26);
        if (wintertodtHealthbar != null && isWintertodtAlive) {
            String widgetText = wintertodtHealthbar.getText();
            wintertodtHp = Integer.parseInt(widgetText.split("\\D+")[1]);
        } else {
            wintertodtHp = -1;
        }
    }

    private void updatePoints() {
        Widget pointsWidget = Rs2Widget.getWidget(396, 8);
        if (pointsWidget != null) {
            String text = pointsWidget.getText();
            if (text != null && text.contains("Points:")) {
                try {
                    currentPoints = Integer.parseInt(text.replaceAll("[^0-9]", ""));
                } catch (NumberFormatException e) {
                    currentPoints = 0;
                }
            }
        }
    }

    private void checkPyromancer() {
        JstBrazier selectedBrazier = config.smartBrazierSelection() ? currentSelectedBrazier : config.brazierLocation();
        if (selectedBrazier == null) selectedBrazier = config.brazierLocation();
        
        WorldPoint pyroLocation = selectedBrazier.getPyromancerLocation();
        List<Rs2NpcModel> npcs = Rs2Npc.getNpcs(NpcID.INCAPACITATED_PYROMANCER).collect(Collectors.toList());
        
        damagedPyromancer = null;
        for (Rs2NpcModel npcModel : npcs) {
            if (npcModel != null && npcModel.getWorldLocation().distanceTo(pyroLocation) <= 2) {
                damagedPyromancer = npcModel.getRuneliteNpc();
                break;
            }
        }
    }

    private boolean shouldBank(boolean playerIsLowWarmth, boolean isWintertodtAlive) {
        // If using rejuvenation potions, bank when we need to make more potions
        if (config.useRejuvenationPotions()) {
            // Count all doses of rejuvenation potions
            int potionCount = Rs2Inventory.count(ItemID.REJUVENATION_POTION_1) + 
                            Rs2Inventory.count(ItemID.REJUVENATION_POTION_2) +
                            Rs2Inventory.count(ItemID.REJUVENATION_POTION_3) +
                            Rs2Inventory.count(ItemID.REJUVENATION_POTION_4);
            // Bank if we have less than 3 potions (critical) or below minFood threshold
            boolean needMorePotions = potionCount < 3 || (potionCount < config.minFood() && playerIsLowWarmth);
            return needMorePotions || !isWintertodtAlive;
        }
        
        // Regular food logic
        return !Rs2Inventory.hasItemAmount(config.food().getName(), config.minFood(), false, false) 
                && (playerIsLowWarmth || !isWintertodtAlive);
    }

    private boolean shouldEat() {
        // Solo mode: eat earlier to avoid deaths
        int eatThreshold = config.soloMode() ? config.eatAtWarmthLevel() + 10 : config.eatAtWarmthLevel();
        
        if (getWarmthLevel() <= eatThreshold) {
            changeState(JstState.EATING);
            if (config.useRejuvenationPotions()) {
                List<Rs2ItemModel> rejuvenationPotions = Rs2Inventory.getPotions();
                if (!rejuvenationPotions.isEmpty()) {
                    Rs2Inventory.interact(rejuvenationPotions.get(0), "Drink");
                    sleepGaussian(600, 150);
                    plugin.setFoodConsumed(plugin.getFoodConsumed() + 1);
                    resetActions = true;
                    return true;
                }
            } else {
                Rs2Player.useFood();
                sleepGaussian(600, 150);
                plugin.setFoodConsumed(plugin.getFoodConsumed() + 1);
                Rs2Inventory.dropAll("jug");
                resetActions = true;
                return true;
            }
        }
        return false;
    }

    private boolean shouldChopRoots() {
        // Check potion count before chopping
        if (config.useRejuvenationPotions()) {
            int potionCount = Rs2Inventory.count(ItemID.REJUVENATION_POTION_1) + 
                            Rs2Inventory.count(ItemID.REJUVENATION_POTION_2) +
                            Rs2Inventory.count(ItemID.REJUVENATION_POTION_3) +
                            Rs2Inventory.count(ItemID.REJUVENATION_POTION_4);
            if (potionCount < 3) {
                Microbot.log("JstWinter: Won't chop roots - need to make potions first");
                return false;
            }
        }
        
        if (Rs2Inventory.isFull()) {
            setLockState(JstState.CHOP_ROOTS, false);
            return false;
        }
        if (hasItemsToBurn()) return false;
        return true;
    }

    private boolean shouldFletchRoots() {
        // Solo mode: always fletch for maximum points
        if (config.soloMode()) {
            return Rs2Inventory.hasItem(BRUMA_ROOT) && Rs2Inventory.hasItem(KNIFE);
        }
        
        if (!config.fletchRoots()) return false;
        if (!Rs2Inventory.hasItem(BRUMA_ROOT)) {
            setLockState(JstState.FLETCH_KINDLING, false);
            return false;
        }
        // Only fletch if we're close to the brazier (safe location)
        if (Rs2Player.getWorldLocation().distanceTo(config.brazierLocation().getSafeLocation()) > 8) {
            return false;
        }
        return true;
    }

    private boolean shouldFeedBrazier() {
        if (!hasItemsToBurn()) {
            setLockState(JstState.FEED_BRAZIER, false);
            return false;
        }
        return true;
    }

    private boolean shouldLightBrazier(boolean isWintertodtAlive, boolean needBanking, GameObject fireBrazier, GameObject brazier) {
        if (!isWintertodtAlive) return false;
        if (needBanking) return false;
        if (state == JstState.CHOP_ROOTS) return false;
        
        if (brazier == null || fireBrazier != null) {
            setLockState(JstState.LIGHT_BRAZIER, false);
            return false;
        }
        
        changeState(JstState.LIGHT_BRAZIER, true);
        return true;
    }

    private boolean isWintertodtAlmostDead() {
        return wintertodtHp > 0 && wintertodtHp < 10;
    }

    private boolean hasItemsToBurn() {
        return Rs2Inventory.hasItem(BRUMA_KINDLING) || Rs2Inventory.hasItem(BRUMA_ROOT);
    }

    private void dropUnnecessaryItems() {
        if (!config.fletchRoots() && Rs2Inventory.hasItem(KNIFE)) {
            Rs2Inventory.drop(KNIFE);
        }
        if (!config.fixBrazier() && Rs2Inventory.hasItem(ItemID.HAMMER)) {
            Rs2Inventory.drop(ItemID.HAMMER);
        }
        if (Rs2Equipment.isWearing(BRUMA_TORCH) && Rs2Inventory.hasItem(ItemID.TINDERBOX)) {
            Rs2Inventory.drop(ItemID.TINDERBOX);
        }
    }

    private void walkToBrazier() {
        JstBrazier selectedBrazier = config.smartBrazierSelection() ? selectOptimalBrazier() : config.brazierLocation();
        if (Rs2Player.getWorldLocation().distanceTo(selectedBrazier.getSafeLocation()) > 6) {
            Rs2Walker.walkTo(selectedBrazier.getSafeLocation(), 2);
        } else if (!Rs2Player.getWorldLocation().equals(selectedBrazier.getSafeLocation())) {
            Rs2Walker.walkFastCanvas(selectedBrazier.getSafeLocation());
            sleep(GAME_TICK_LENGTH);
        } else if (Rs2Player.getWorldLocation().equals(selectedBrazier.getSafeLocation()) && state == JstState.WAITING) {
            Rs2GameObject.hoverOverObject(currentBrazier);
        }
    }

    private void dodgeSnowfall() {
        for (GraphicsObject graphicsObject : Microbot.getClient().getGraphicsObjects()) {
            if (!resetActions && graphicsObject.getId() == 502
                    && WorldPoint.fromLocalInstance(Microbot.getClient(), 
                    graphicsObject.getLocation()).distanceTo(Rs2Player.getWorldLocation()) == 1) {
                List<GameObject> gameObjects = Rs2GameObject.getGameObjects(5);
                if (gameObjects.size() > 2) {
                    changeState(JstState.DODGING);
                    Rs2Walker.walkFastCanvas(new WorldPoint(
                            Rs2Player.getWorldLocation().getX(), 
                            Rs2Player.getWorldLocation().getY() - 1, 
                            Rs2Player.getWorldLocation().getPlane()));
                    Rs2Player.waitForWalking(1000);
                    resetActions = true;
                }
            }
        }
    }

    private boolean handleBanking() {
        if (config.useRejuvenationPotions()) {
            // Just bank and prepare basic supplies for rejuvenation potion users
            if (Rs2Player.getWorldLocation().distanceTo(BANK_LOCATION) > 6) {
                Rs2Walker.walkTo(BANK_LOCATION);
                Rs2Player.waitForWalking();
                return true;
            }
            
            Rs2Bank.useBank();
            if (!Rs2Bank.isOpen()) return true;
            
            // Deposit everything except essentials
            Rs2Bank.depositAllExcept("hammer", "tinderbox", "knife", axe);
            
            // Withdraw essentials
            if (config.fixBrazier() && !Rs2Inventory.hasItem("hammer")) {
                Rs2Bank.withdrawDeficit("hammer", 1);
            }
            if (!Rs2Equipment.isWearing(BRUMA_TORCH)) {
                Rs2Bank.withdrawDeficit("tinderbox", 1, true);
            }
            if (config.fletchRoots()) {
                Rs2Bank.withdrawDeficit("knife", 1, true);
            }
            if (config.axeInInventory()) {
                Rs2Bank.withdrawDeficit(axe, 1);
            }
            
            Rs2Bank.closeBank();
            return false;
        }
        
        if (!Rs2Player.isFullHealth() && Rs2Inventory.hasItem(config.food().getName(), false)) {
            eatAt(99);
            return true;
        }
        
        if (Rs2Inventory.hasItemAmount(config.food().getName(), config.foodAmount())) {
            state = JstState.ENTER_ROOM;
            return true;
        }
        
        if (Rs2Player.getWorldLocation().distanceTo(BANK_LOCATION) > 6) {
            Rs2Walker.walkTo(BANK_LOCATION);
            Rs2Player.waitForWalking();
        }
        
        Rs2Bank.useBank();
        if (!Rs2Bank.isOpen()) return true;
        
        Rs2Bank.depositAllExcept("hammer", "tinderbox", "knife", config.food().getName(), axe);
        int foodCount = Rs2Inventory.getInventoryFood().size();
        
        if (config.fixBrazier() && !Rs2Inventory.hasItem("hammer")) {
            Rs2Bank.withdrawDeficit("hammer", 1);
        }
        if (!Rs2Equipment.isWearing(BRUMA_TORCH)) {
            Rs2Bank.withdrawDeficit("tinderbox", 1, true);
        }
        if (config.fletchRoots()) {
            Rs2Bank.withdrawDeficit("knife", 1, true);
        }
        if (config.axeInInventory()) {
            Rs2Bank.withdrawDeficit(axe, 1);
        }
        
        if (!Rs2Bank.hasBankItem(config.food().getName(), config.foodAmount(), true)) {
            Microbot.showMessage("JstWinter: Insufficient food!");
            Microbot.pauseAllScripts.compareAndSet(false, true);
            return true;
        }
        
        Rs2Bank.withdrawX(config.food().getId(), config.foodAmount() - foodCount);
        return sleepUntilTrue(() -> Rs2Inventory.hasItemAmount(config.food().getName(), config.foodAmount(), false, true), 100, 5000);
    }


    public int getWarmthLevel() {
        String warmthWidgetText = Rs2Widget.getChildWidgetText(396, 20);
        
        if (warmthWidgetText == null || warmthWidgetText.isEmpty()) {
            Microbot.log("JstWinter: No warmth level found");
            return 100;
        }
        
        Pattern pattern = Pattern.compile("(\\d+)%");
        Matcher matcher = pattern.matcher(warmthWidgetText);
        
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        
        Pattern fallbackPattern = Pattern.compile("\\d+");
        Matcher fallbackMatcher = fallbackPattern.matcher(warmthWidgetText);
        
        if (fallbackMatcher.find()) {
            return Integer.parseInt(fallbackMatcher.group());
        }
        
        Microbot.log("JstWinter: No warmth level found");
        return 100;
    }
    
    private boolean isInWintertodtRegion() {
        Player localPlayer = Microbot.getClient().getLocalPlayer();
        if (localPlayer == null) return false;
        WorldPoint location = localPlayer.getWorldLocation();
        if (location == null) return false;
        return location.getRegionID() == 6462;
    }
    
    private boolean isNearBank() {
        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (playerLocation == null) return false;
        return playerLocation.distanceTo(BANK_LOCATION) <= 10;
    }
    
    private JstBrazier selectOptimalBrazier() {
        // Solo mode: always use configured brazier
        if (config.soloMode()) {
            return config.brazierLocation();
        }
        
        // Check if we should consider switching braziers
        if (currentSelectedBrazier != null && !shouldReselectBrazier()) {
            return currentSelectedBrazier;
        }
        
        JstBrazier bestBrazier = config.brazierLocation();
        int bestScore = Integer.MAX_VALUE;
        
        // Evaluate all available braziers
        for (JstBrazier brazier : JstBrazier.values()) {
            int score = evaluateBrazierScore(brazier);
            
            if (score < bestScore) {
                bestScore = score;
                bestBrazier = brazier;
            }
        }
        
        // Log brazier switch if it's different from current
        if (currentSelectedBrazier != bestBrazier) {
            int currentBrazierScore = currentSelectedBrazier != null ? evaluateBrazierScore(currentSelectedBrazier) : Integer.MAX_VALUE;
            Microbot.log(String.format("JstWinter: Switching from %s (score: %d) to %s (score: %d)", 
                currentSelectedBrazier != null ? currentSelectedBrazier.name() : "none", 
                currentBrazierScore,
                bestBrazier.name(), 
                bestScore));
            currentSelectedBrazier = bestBrazier;
            lastBrazierSwitchTime = System.currentTimeMillis();
        }
        
        return bestBrazier;
    }
    
    private int evaluateBrazierScore(JstBrazier brazier) {
        int playerCount = countPlayersNearBrazier(brazier);
        GameObject brazierObj = Rs2GameObject.getGameObject(BRAZIER_29312, brazier.getBrazierLocation());
        GameObject brokenBrazier = Rs2GameObject.getGameObject(BRAZIER_29313, brazier.getBrazierLocation());
        GameObject burningBrazier = Rs2GameObject.getGameObject(BURNING_BRAZIER_29314, brazier.getBrazierLocation());
        
        int score = playerCount * 2; // Base score from player count
        
        // Penalties and bonuses
        if (brokenBrazier != null) {
            score += 8; // High penalty for broken brazier
        } else if (burningBrazier == null && brazierObj != null) {
            score += 3; // Medium penalty for unlit brazier
        } else if (burningBrazier != null) {
            score -= 2; // Small bonus for burning brazier
        }
        
        // Bonus for current brazier to avoid unnecessary switches
        if (brazier == currentSelectedBrazier) {
            score -= 1; // Small bonus to stick with current brazier
        }
        
        // Distance penalty (slight preference for closer braziers)
        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (playerLocation != null) {
            int distance = playerLocation.distanceTo(brazier.getBrazierLocation());
            score += distance / 10; // Small distance penalty
        }
        
        return Math.max(0, score); // Ensure score is never negative
    }
    
    private boolean shouldReselectBrazier() {
        long currentTime = System.currentTimeMillis();
        
        // Don't switch too frequently
        if (currentTime - lastBrazierSwitchTime < switchCooldownMs) {
            return false;
        }
        
        // Always reselect when in these states
        if (state == JstState.WAITING || state == JstState.BANKING) {
            return true;
        }
        
        // Force reselection if current brazier becomes very crowded
        if (currentSelectedBrazier != null) {
            int currentPlayerCount = countPlayersNearBrazier(currentSelectedBrazier);
            if (currentPlayerCount > config.maxPlayersBeforeHop() - 3) { // Switch before world hop threshold
                Microbot.log("JstWinter: Current brazier too crowded (" + currentPlayerCount + " players), looking for alternatives");
                return true;
            }
        }
        
        // Reselect if we've been in the same non-productive state too long
        return (currentTime - stateStartTime > 45000); // 45 seconds
    }
    
    private int countPlayersNearBrazier(JstBrazier brazier) {
        List<Player> players = Rs2Player.getPlayers();
        int count = 0;
        
        for (Player player : players) {
            if (player != null && player != Microbot.getClient().getLocalPlayer()) {
                WorldPoint playerLocation = player.getWorldLocation();
                if (playerLocation != null && playerLocation.distanceTo(brazier.getBrazierLocation()) <= 10) {
                    count++;
                }
            }
        }
        
        return count;
    }
    
    private boolean shouldHopWorlds() {
        // Don't hop if not in Wintertodt area
        if (!isInWintertodtRegion()) {
            return false;
        }
        
        // Don't hop too frequently
        long timeSinceLastHop = System.currentTimeMillis() - lastHopTime;
        if (timeSinceLastHop < 60000) { // 1 minute cooldown
            Microbot.log("JstWinter: World hop on cooldown (" + (60000 - timeSinceLastHop) / 1000 + "s remaining)");
            return false;
        }
        
        // Don't hop mid-game unless it's really bad
        if (isWintertodtAlive() && wintertodtHp > 20) {
            return false;
        }
        
        // Check player count
        int playerCount = countTotalPlayersInArea();
        Microbot.log("JstWinter: Player count check - " + playerCount + " players (max: " + config.maxPlayersBeforeHop() + ")");
        if (playerCount > config.maxPlayersBeforeHop()) {
            Microbot.log("JstWinter: Too many players (" + playerCount + "), preparing to hop");
            return true;
        }
        
        // Check if we're being crashed repeatedly
        if (consecutiveCrashes >= 3) {
            Microbot.log("JstWinter: Too many crashes (" + consecutiveCrashes + "), preparing to hop");
            return true;
        }
        
        return false;
    }
    
    private boolean isWintertodtAlive() {
        return Rs2Widget.hasWidget("Wintertodt's Energy") && wintertodtHp > 0;
    }
    
    private int countTotalPlayersInArea() {
        List<Player> players = Rs2Player.getPlayers();
        int count = 0;
        
        for (Player player : players) {
            if (player != null && player != Microbot.getClient().getLocalPlayer()) {
                WorldPoint playerLocation = player.getWorldLocation();
                if (playerLocation != null && playerLocation.getRegionID() == 6462) {
                    count++;
                }
            }
        }
        
        return count;
    }
    
    private boolean isBeingCrashed(int objectId) {
        GameObject targetObject = Rs2GameObject.getGameObject(objectId);
        if (targetObject == null) return false;
        
        List<Player> players = Rs2Player.getPlayers();
        for (Player player : players) {
            if (player != null && player != Microbot.getClient().getLocalPlayer()) {
                // Check if another player is at the same object and animating
                if (player.getWorldLocation().distanceTo(targetObject.getWorldLocation()) <= 1 && 
                    player.getAnimation() != -1) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private void handleCrash() {
        long currentTime = System.currentTimeMillis();
        
        // Reset crash counter if it's been a while
        if (currentTime - lastCrashTime > 300000) { // 5 minutes
            consecutiveCrashes = 0;
        }
        
        consecutiveCrashes++;
        lastCrashTime = currentTime;
        
        Microbot.log("JstWinter: Crashed! (" + consecutiveCrashes + " times)");
        
        if (config.hopOnCrash() && consecutiveCrashes >= 2) {
            handleWorldHop();
        }
    }
    
    private void handleWorldHop() {
        Microbot.log("JstWinter: Initiating world hop from state: " + state);
        
        // Find a suitable world
        World targetWorld = findSuitableWorld();
        if (targetWorld == null) {
            Microbot.log("JstWinter: No suitable world found, continuing current world");
            return;
        }
        
        // Stop the script temporarily
        JstState previousState = state;
        state = JstState.WAITING;
        lockState = true;
        
        // Hop to the world
        Microbot.log("JstWinter: Attempting to hop to world " + targetWorld.getId());
        if (Microbot.hopToWorld(targetWorld.getId())) {
            Microbot.log("JstWinter: Successfully hopped to world " + targetWorld.getId());
            lastHopTime = System.currentTimeMillis();
            consecutiveCrashes = 0;
            
            // Wait for the hop to complete
            sleepUntil(() -> Microbot.getClient().getGameState() == GameState.LOGGED_IN, 10000);
            sleep(2000);
            
            // Reset state
            lockState = false;
            state = JstState.BANKING;
        } else {
            Microbot.log("JstWinter: Failed to hop worlds");
            lockState = false;
        }
    }
    
    private World findSuitableWorld() {
        World[] worldArray = Microbot.getClient().getWorldList();
        if (worldArray == null) return null;
        List<World> worlds = Arrays.asList(worldArray);
        
        int currentWorldId = Microbot.getClient().getWorld();
        List<World> suitableWorlds = new ArrayList<>();
        
        for (World world : worlds) {
            // Skip current world
            if (world.getId() == currentWorldId) continue;
            
            // Skip restricted worlds
            if (world.getTypes().contains(WorldType.PVP) ||
                world.getTypes().contains(WorldType.HIGH_RISK) ||
                world.getTypes().contains(WorldType.DEADMAN) ||
                world.getTypes().contains(WorldType.SKILL_TOTAL) ||
                world.getTypes().contains(WorldType.QUEST_SPEEDRUNNING) ||
                world.getTypes().contains(WorldType.TOURNAMENT_WORLD)) {
                continue;
            }
            
            // Check world type preference
            if (config.preferredWorldType() == JstWinterConfig.WorldType.MEMBERS && 
                !world.getTypes().contains(WorldType.MEMBERS)) {
                continue;
            }
            
            if (config.preferredWorldType() == JstWinterConfig.WorldType.FREE && 
                world.getTypes().contains(WorldType.MEMBERS)) {
                continue;
            }
            
            // Check player count (prefer less crowded worlds)
            if (world.getPlayerCount() < 1500) {
                suitableWorlds.add(world);
            }
        }
        
        // Sort by player count (ascending) and pick one of the least crowded
        if (!suitableWorlds.isEmpty()) {
            suitableWorlds.sort((a, b) -> Integer.compare(a.getPlayerCount(), b.getPlayerCount()));
            // Pick from the 5 least crowded worlds randomly
            int index = Math.min(suitableWorlds.size() - 1, (int)(Math.random() * 5));
            return suitableWorlds.get(index);
        }
        
        return null;
    }

    private boolean handleMakePotions() {
        Microbot.log("JstWinter: Making rejuvenation potions in minigame");
        
        // Count current potions
        int currentPotions = Rs2Inventory.count(ItemID.REJUVENATION_POTION_1) + 
                           Rs2Inventory.count(ItemID.REJUVENATION_POTION_2) +
                           Rs2Inventory.count(ItemID.REJUVENATION_POTION_3) +
                           Rs2Inventory.count(ItemID.REJUVENATION_POTION_4);
        int potionsNeeded = Math.max(3, config.minFood()) - currentPotions;
        
        if (potionsNeeded <= 0) {
            return false; // Have enough potions
        }
        
        // Walk to crate location if not close
        if (Rs2Player.getWorldLocation().distanceTo(CRATE_LOCATION) > 5) {
            Rs2Walker.walkTo(CRATE_LOCATION, 3);
            Rs2Player.waitForWalking(2000);
            return true;
        }
        
        // Get unfinished potions from crate
        int currentUnfPotions = Rs2Inventory.count(ItemID.REJUVENATION_POTION_UNF);
        
        if (currentUnfPotions < potionsNeeded) {
            GameObject crate = Rs2GameObject.getGameObject(CRATE_LOCATION);
            if (crate == null) {
                crate = Rs2GameObject.getGameObject(29330); // Backup ID for crate
            }
            
            final GameObject finalCrate = crate;
            if (finalCrate != null) {
                Microbot.log("JstWinter: Taking concoction from crate");
                if (Rs2GameObject.interact(finalCrate, "Take-concoction")) {
                    Rs2Inventory.waitForInventoryChanges(2000);
                }
                return true;
            }
        }
        
        // Get herbs if we need them
        int unfPotions = Rs2Inventory.count(ItemID.REJUVENATION_POTION_UNF);
        int currentHerbs = Rs2Inventory.count(ItemID.BRUMA_HERB);
        int herbsNeeded = Math.max(0, unfPotions - currentHerbs);
        
        if (herbsNeeded > 0) {
            // Walk to herb location
            if (Rs2Player.getWorldLocation().distanceTo(HERB_LOCATION) > 3) {
                Rs2Walker.walkTo(HERB_LOCATION, 2);
                Rs2Player.waitForWalking(2000);
                return true;
            }
            
            GameObject herbs = Rs2GameObject.getGameObject(HERB_LOCATION);
            if (herbs == null) {
                herbs = Rs2GameObject.getGameObject(29311); // Backup ID for herbs
            }
            
            final GameObject finalHerbs = herbs;
            if (finalHerbs != null) {
                Microbot.log("JstWinter: Picking bruma herbs");
                if (Rs2GameObject.interact(finalHerbs, "Pick")) {
                    Rs2Inventory.waitForInventoryChanges(2000);
                }
                return true;
            }
        }
        
        // Combine potions if we have both ingredients
        if (Rs2Inventory.hasItem(ItemID.REJUVENATION_POTION_UNF) && Rs2Inventory.hasItem(ItemID.BRUMA_HERB)) {
            Microbot.log("JstWinter: Mixing rejuvenation potions");
            
            // Combine all available potions
            while (Rs2Inventory.hasItem(ItemID.REJUVENATION_POTION_UNF) && Rs2Inventory.hasItem(ItemID.BRUMA_HERB)) {
                Rs2Inventory.combineClosest(ItemID.REJUVENATION_POTION_UNF, ItemID.BRUMA_HERB);
                Rs2Inventory.waitForInventoryChanges(1000);
            }
            return true;
        }
        
        return true;
    }

    @Override
    public void shutdown() {
        Rs2Antiban.resetAntibanSettings();
        super.shutdown();
    }
}