package net.runelite.client.plugins.microbot.jstvale;

import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.breakhandler.BreakHandlerScript;
import net.runelite.client.plugins.microbot.jstvale.enums.JstValeState;
import net.runelite.client.plugins.microbot.jstvale.enums.TotemLocation;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.antiban.enums.Activity;
import net.runelite.client.plugins.microbot.util.antiban.enums.PlayStyle;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static net.runelite.api.ItemID.*;

public class JstValeScript extends Script {
    public static String version = "1.0.0";
    
    private JstValeConfig config;
    private JstValePlugin plugin;
    private JstValeState state = JstValeState.BANKING;
    private TotemLocation currentTotem;
    private int currentTotemIndex = 0;
    private boolean runCompleted = false;
    
    // Vale Totems locations
    private final WorldPoint AUBURNVALE_BANK = new WorldPoint(1416, 3352, 0); // Center of bank area (1412-1420, 3349-3356)
    
    // Totem construction materials
    private final int[] LOG_IDS = {LOGS, OAK_LOGS, WILLOW_LOGS, MAPLE_LOGS, YEW_LOGS, MAGIC_LOGS};
    private final int[] BOW_IDS = {LONGBOW, OAK_LONGBOW, WILLOW_LONGBOW, MAPLE_LONGBOW, YEW_LONGBOW, MAGIC_LONGBOW};
    
    public boolean run(JstValeConfig config, JstValePlugin plugin) {
        this.config = config;
        this.plugin = plugin;
        
        Rs2Antiban.resetAntibanSettings();
        Rs2Antiban.antibanSetupTemplates.applyGeneralBasicSetup();
        Rs2Antiban.setActivity(Activity.GENERAL_CONSTRUCTION);
        Rs2Antiban.setPlayStyle(PlayStyle.MODERATE);
        
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                if (Rs2AntibanSettings.actionCooldownActive) return;
                
                long startTime = System.currentTimeMillis();
                
                // Handle breaks
                if (config.enableBreaks() && Rs2Antiban.takeMicroBreakByChance()) {
                    return;
                }
                
                switch (state) {
                    case BANKING:
                        handleBanking();
                        break;
                        
                    case WALKING_TO_BANK:
                        walkToBank();
                        break;
                        
                    case WALKING_TO_TOTEM:
                        walkToCurrentTotem();
                        break;
                        
                    case FLECHING_LOGS:
                        fletchLogs();
                        break;
                        
                    case CONSTRUCTING_TOTEM:
                        constructTotem();
                        break;
                        
                    case DECORATING_TOTEM:
                        decorateTotem();
                        break;
                        
                    case COLLECTING_OFFERINGS:
                        collectOfferings();
                        break;
                        
                    case WAITING:
                        handleWaiting();
                        break;
                }
                
                long endTime = System.currentTimeMillis();
                long executionTime = endTime - startTime;
                Microbot.log("JstVale: Loop execution time: " + executionTime + "ms");
                
            } catch (Exception ex) {
                Microbot.log("JstVale error: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
        
        return true;
    }
    
    private void handleBanking() {
        if (!Rs2Bank.isNearBank(10)) {
            state = JstValeState.WALKING_TO_BANK;
            return;
        }
        
        if (!Rs2Bank.isOpen()) {
            Rs2Bank.useBank();
            return;
        }
        
        // Check if we have enough supplies
        int logId = getLogId();
        int logsNeeded = config.logsPerRun();
        
        if (Rs2Inventory.hasItemAmount(logId, logsNeeded)) {
            // We have enough logs, close bank and start run
            Rs2Bank.closeBank();
            initializeNewRun();
            state = JstValeState.WALKING_TO_TOTEM;
            return;
        }
        
        // Deposit everything except knife and log basket
        if (config.useLogBasket()) {
            Rs2Bank.depositAllExcept("knife", "log basket");
        } else {
            Rs2Bank.depositAllExcept("knife");
        }
        
        // Withdraw knife if we don't have one and plan to fletch
        if (config.fletchLogs() && !Rs2Inventory.hasItem("knife")) {
            Rs2Bank.withdrawX("knife", 1);
        }
        
        // Withdraw log basket if configured
        if (config.useLogBasket() && !Rs2Inventory.hasItem("log basket")) {
            Rs2Bank.withdrawX("log basket", 1);
        }
        
        // Withdraw logs
        if (!Rs2Bank.hasBankItem(getLogName(), logsNeeded, true)) {
            Microbot.showMessage("JstVale: Insufficient logs!");
            Microbot.pauseAllScripts.compareAndSet(false, true);
            return;
        }
        
        Rs2Bank.withdrawX(logId, logsNeeded);
        sleepUntil(() -> Rs2Inventory.hasItemAmount(logId, logsNeeded), 5000);
    }
    
    private void walkToBank() {
        Rs2Walker.walkTo(AUBURNVALE_BANK);
        if (Rs2Bank.isNearBank(10)) {
            state = JstValeState.BANKING;
        }
    }
    
    private void walkToCurrentTotem() {
        if (currentTotem == null) {
            initializeNewRun();
        }
        
        WorldPoint totemLocation = currentTotem.getLocation();
        
        if (Rs2Player.getWorldLocation().distanceTo(totemLocation) <= 3) {
            // We're at the totem
            if (config.fletchLogs() && Rs2Inventory.hasItem(getLogId()) && Rs2Inventory.hasItem("knife")) {
                state = JstValeState.FLECHING_LOGS;
            } else {
                state = JstValeState.CONSTRUCTING_TOTEM;
            }
        } else {
            // Walk to totem
            if (config.useAgilityShortcuts()) {
                // TODO: Implement agility shortcuts logic
                Rs2Walker.walkTo(totemLocation);
            } else {
                Rs2Walker.walkTo(totemLocation);
            }
        }
    }
    
    private void fletchLogs() {
        int logId = getLogId();
        
        if (!Rs2Inventory.hasItem(logId)) {
            state = JstValeState.CONSTRUCTING_TOTEM;
            return;
        }
        
        if (!Rs2Inventory.hasItem("knife")) {
            Microbot.log("JstVale: No knife found for fletching!");
            state = JstValeState.CONSTRUCTING_TOTEM;
            return;
        }
        
        if (!Rs2Player.isAnimating()) {
            Rs2Inventory.combineClosest("knife", getLogName());
            sleepUntil(Rs2Player::isAnimating, 3000);
        }
        
        // Wait for fletching to complete or inventory to run out of logs
        sleepUntil(() -> !Rs2Player.isAnimating() || !Rs2Inventory.hasItem(logId), 10000);
        
        if (!Rs2Inventory.hasItem(logId)) {
            state = JstValeState.CONSTRUCTING_TOTEM;
        }
    }
    
    private void constructTotem() {
        // Look for totem construction object
        GameObject totemSite = Rs2GameObject.getGameObject("Totem site"); // This needs actual object name/ID
        
        if (totemSite == null) {
            Microbot.log("JstVale: No totem site found!");
            moveToNextTotem();
            return;
        }
        
        // Check if we have construction materials
        int materialId = config.fletchLogs() ? getBowId() : getLogId();
        
        if (!Rs2Inventory.hasItem(materialId)) {
            Microbot.log("JstVale: No construction materials!");
            moveToNextTotem();
            return;
        }
        
        if (!Rs2Player.isAnimating()) {
            Rs2GameObject.interact(totemSite, "Construct");
            sleepUntil(Rs2Player::isAnimating, 3000);
        }
        
        sleepUntil(() -> !Rs2Player.isAnimating(), 10000);
        
        // Check if totem is constructed
        if (isTotemConstructed()) {
            state = JstValeState.DECORATING_TOTEM;
        }
    }
    
    private void decorateTotem() {
        GameObject constructedTotem = Rs2GameObject.getGameObject("Constructed totem"); // This needs actual object name/ID
        
        if (constructedTotem == null) {
            Microbot.log("JstVale: No constructed totem found!");
            moveToNextTotem();
            return;
        }
        
        int materialId = config.fletchLogs() ? getBowId() : getLogId();
        
        if (!Rs2Inventory.hasItem(materialId)) {
            Microbot.log("JstVale: No decoration materials!");
            moveToNextTotem();
            return;
        }
        
        if (!Rs2Player.isAnimating()) {
            Rs2GameObject.interact(constructedTotem, "Decorate");
            sleepUntil(Rs2Player::isAnimating, 3000);
        }
        
        sleepUntil(() -> !Rs2Player.isAnimating(), 10000);
        
        // Totem completed
        plugin.incrementTotemsCompleted();
        
        if (config.collectOfferings()) {
            state = JstValeState.COLLECTING_OFFERINGS;
        } else {
            moveToNextTotem();
        }
    }
    
    private void collectOfferings() {
        // Look for vale offerings nearby
        GameObject offering = Rs2GameObject.getGameObject("Vale offering"); // This needs actual object name/ID
        
        if (offering != null && !Rs2Inventory.isFull()) {
            Rs2GameObject.interact(offering, "Take");
            plugin.incrementOfferingsCollected();
            sleep(1000, 2000);
        } else {
            moveToNextTotem();
        }
    }
    
    private void handleWaiting() {
        // Handle any waiting logic or transitions
        sleep(1000);
        moveToNextTotem();
    }
    
    private void moveToNextTotem() {
        currentTotemIndex++;
        
        if (currentTotemIndex >= 8) {
            // Completed full run
            plugin.incrementRunsCompleted();
            runCompleted = true;
            
            // Check if we need more materials
            int materialId = config.fletchLogs() ? getBowId() : getLogId();
            if (!Rs2Inventory.hasItem(materialId)) {
                state = JstValeState.BANKING;
                return;
            }
            
            // Start new run
            initializeNewRun();
        }
        
        // Set next totem
        TotemLocation[] totems = TotemLocation.values();
        currentTotem = totems[currentTotemIndex];
        state = JstValeState.WALKING_TO_TOTEM;
    }
    
    private void initializeNewRun() {
        currentTotemIndex = Arrays.asList(TotemLocation.values()).indexOf(config.startingTotem());
        currentTotem = config.startingTotem();
        runCompleted = false;
    }
    
    private boolean isTotemConstructed() {
        // Check if there's a constructed totem at current location
        return Rs2GameObject.getGameObject("Constructed totem") != null;
    }
    
    private int getLogId() {
        switch (config.logType()) {
            case REGULAR_LOGS: return LOGS;
            case OAK_LOGS: return OAK_LOGS;
            case WILLOW_LOGS: return WILLOW_LOGS;
            case MAPLE_LOGS: return MAPLE_LOGS;
            case YEW_LOGS: return YEW_LOGS;
            case MAGIC_LOGS: return MAGIC_LOGS;
            default: return MAGIC_LOGS;
        }
    }
    
    private int getBowId() {
        switch (config.logType()) {
            case REGULAR_LOGS: return LONGBOW;
            case OAK_LOGS: return OAK_LONGBOW;
            case WILLOW_LOGS: return WILLOW_LONGBOW;
            case MAPLE_LOGS: return MAPLE_LONGBOW;
            case YEW_LOGS: return YEW_LONGBOW;
            case MAGIC_LOGS: return MAGIC_LONGBOW;
            default: return MAGIC_LONGBOW;
        }
    }
    
    private String getLogName() {
        switch (config.logType()) {
            case REGULAR_LOGS: return "Logs";
            case OAK_LOGS: return "Oak logs";
            case WILLOW_LOGS: return "Willow logs";
            case MAPLE_LOGS: return "Maple logs";
            case YEW_LOGS: return "Yew logs";
            case MAGIC_LOGS: return "Magic logs";
            default: return "Magic logs";
        }
    }
    
    // Getter methods for overlay
    public JstValeState getState() {
        return state;
    }
    
    public TotemLocation getCurrentTotem() {
        return currentTotem;
    }
    
    @Override
    public void shutdown() {
        Rs2Antiban.resetAntibanSettings();
        super.shutdown();
    }
}