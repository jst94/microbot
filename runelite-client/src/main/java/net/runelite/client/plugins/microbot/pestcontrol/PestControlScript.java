package net.runelite.client.plugins.microbot.pestcontrol;

import com.google.common.collect.ImmutableSet;
import net.runelite.api.NPCComposition;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.Rs2InventorySetup;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.pestcontrol.Portal;
import org.apache.commons.lang3.tuple.Pair;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer.isQuickPrayerEnabled;
import static net.runelite.client.plugins.microbot.util.walker.Rs2Walker.distanceToRegion;
import static net.runelite.client.plugins.pestcontrol.Portal.*;

public class PestControlScript extends Script {
    public static final double VERSION = 2.2;
    
    // Constants
    private static final int PEST_CONTROL_REGION_ID = 10537;
    private static final int DISTANCE_TO_PORTAL = 8;
    private static final int LOW_ACTIVITY_THRESHOLD = 20;
    private static final int CENTER_REGION_X = 32;
    private static final int CENTER_REGION_Y = 17;
    private static final int CENTER_WALK_DISTANCE = 3;
    private static final int CENTER_PROXIMITY_THRESHOLD = 4;
    private static final int BRAWLER_PROXIMITY_THRESHOLD = 3;
    private static final WorldPoint PEST_CONTROL_DOCK = new WorldPoint(2667, 2653, 0);
    
    // State flags
    private boolean isInitializing = true;
    private boolean hasWalkedToCenter = false;
    
    // Dependencies
    private PestControlConfig config;
    private final PestControlPlugin plugin;

    @Inject
    public PestControlScript(PestControlPlugin plugin, PestControlConfig config) {
        this.plugin = plugin;
        this.config = config;
    }    // NPC ID sets
    private static final Set<Integer> SPINNER_IDS = ImmutableSet.of(
            1709, // SPINNER
            1710, // SPINNER_1710
            1711, // SPINNER_1711
            1712, // SPINNER_1712
            1713  // SPINNER_1713
    );

    private static final Set<Integer> BRAWLER_IDS = ImmutableSet.of(
            1734, // BRAWLER
            1736, // BRAWLER_1736
            1738, // BRAWLER_1738
            1737, // BRAWLER_1737
            1735  // BRAWLER_1735
    );    // Portal management
    private static final List<Portal> PORTALS = List.of(PURPLE, BLUE, RED, YELLOW);
    public static final List<Portal> portals = PORTALS; // For compatibility with other classes

    /**
     * Resets all portals to have shields (initial state)
     */
    private void resetPortals() {
        for (Portal portal : PORTALS) {
            portal.setHasShield(true);
        }
    }

    public boolean run(PestControlConfig config) {
        this.config = config;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run()) {
                    return;
                }

                executeMainLoop();
                
            } catch (Exception ex) {
                Microbot.log("Error in PestControl main loop: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, 0, 300, TimeUnit.MILLISECONDS);
        return true;
    }
    
    /**
     * Main execution loop that handles all pest control states
     */
    private void executeMainLoop() {
        final boolean isInPestControl = isInPestControl();
        final boolean isInBoat = isInBoat();
        
        logCurrentState(isInPestControl, isInBoat);

        if (isInitializing && !isInPestControl && !isInBoat) {
            handleInitialization();
        } else if (isInPestControl) {
            handlePestControlGame();
        } else {
            handleWaitingState();
        }
    }
    
    /**
     * Logs the current state for debugging purposes
     */
    private void logCurrentState(boolean isInPestControl, boolean isInBoat) {
        if (Microbot.getClient().getGameCycle() % 10 == 0) { // Log every 3 seconds
            Microbot.log("State - Initializing: " + isInitializing + 
                        ", In Pest Control: " + isInPestControl + 
                        ", In Boat: " + isInBoat);
        }
    }
    
    /**
     * Handles the initialization phase (world hopping, banking, setup)
     */
    private void handleInitialization() {
        Microbot.log("Initializing Pest Control");
        
        if (!isOnCorrectWorld()) {
            switchToConfiguredWorld();
            return;
        }
        
        if (isAtPestControlDock()) {
            handleBankingAndSetup();
        } else {
            travelToPestControlDock();
        }
    }
    
    /**
     * Checks if player is on the correct world
     */
    private boolean isOnCorrectWorld() {
        return Rs2Player.getWorld() == config.world();
    }
    
    /**
     * Switches to the configured world
     */
    private void switchToConfiguredWorld() {
        Microbot.hopToWorld(config.world());
        sleep(1000, 3000);
        Microbot.hopToWorld(config.world());
        sleepUntil(() -> Rs2Player.getWorld() == config.world(), 7000);
    }
    
    /**
     * Checks if player is at the pest control dock
     */
    private boolean isAtPestControlDock() {
        return Rs2Player.getWorldLocation().getRegionID() == PEST_CONTROL_REGION_ID && 
               isOnCorrectWorld();
    }
    
    /**
     * Handles banking and inventory setup at the pest control dock
     */
    private void handleBankingAndSetup() {
        if (!Rs2Bank.isOpen()) {
            Microbot.log("Opening bank for inventory setup");
            Rs2Bank.openBank();
            sleepUntil(Rs2Bank::isOpen, 3000);
            return;
        }
        
        try {
            var inventorySetup = new Rs2InventorySetup(config.inventorySetup(), mainScheduledFuture);
            Microbot.log("Setting up inventory and equipment");
            
            if (!inventorySetup.doesInventoryMatch() || !inventorySetup.doesEquipmentMatch()) {
                if (!inventorySetup.loadEquipment() || !inventorySetup.loadInventory()) {
                    plugin.reportFinished("Failed to load inventory setup", false);
                    return;
                }
            } else {
                Microbot.log("Inventory setup completed");
                Rs2Bank.closeBank();
                sleepUntil(() -> !Rs2Bank.isOpen(), 2000);
                isInitializing = false;
            }
            
        } catch (NullPointerException e) {
            throw new RuntimeException("Invalid inventory setup configuration. Please reconfigure the inventory setup.");
        }
    }
    
    /**
     * Travels to the pest control dock
     */
    private void travelToPestControlDock() {
        Microbot.log("Traveling to Pest Control dock");
        Rs2Walker.walkTo(PEST_CONTROL_DOCK);
    }    /**
     * Handles combat and objectives during the pest control game
     */
    private void handlePestControlGame() {
        plugin.lockCondition.lock();
        isInitializing = false;
        
        enableQuickPrayerIfNeeded();
        
        if (!hasWalkedToCenter) {
            walkToCenterOfMap();
            return;
        }
        
        Rs2Combat.setSpecState(true, config.specialAttackPercentage() * 10);
        
        if (shouldAttackForActivity()) {
            attackAnyNpcForActivity();
            return;
        }
        
        if (shouldAttackNearbyBrawler()) {
            return;
        }
        
        if (Microbot.getClient().getLocalPlayer().isInteracting()) {
            return;
        }
        
        if (executeAttackPriorities()) {
            return;
        }
        
        // Fallback attacks
        attackAnyAvailableTarget();
    }
    
    /**
     * Enables quick prayer if configured and conditions are met
     */
    private void enableQuickPrayerIfNeeded() {
        if (!isQuickPrayerEnabled() && 
            Microbot.getClient().getBoostedSkillLevel(Skill.PRAYER) != 0 && 
            config.quickPrayer()) {
            
            final Widget prayerOrb = Rs2Widget.getWidget(InterfaceID.Orbs.PRAYERBUTTON);
            if (prayerOrb != null) {
                Microbot.getMouse().click(prayerOrb.getCanvasLocation());
                sleep(1000, 1500);
            }
        }
    }
    
    /**
     * Walks to the center of the pest control map
     */
    private void walkToCenterOfMap() {        WorldPoint centerPoint = WorldPoint.fromRegion(
            Rs2Player.getWorldLocation().getRegionID(), 
            CENTER_REGION_X, 
            CENTER_REGION_Y, 
            Microbot.getClient().getTopLevelWorldView().getPlane()
        );
        
        Rs2Walker.walkTo(centerPoint, CENTER_WALK_DISTANCE);
        
        if (centerPoint.distanceTo(Rs2Player.getWorldLocation()) <= CENTER_PROXIMITY_THRESHOLD) {
            hasWalkedToCenter = true;
        }
    }
    
    /**
     * Checks if player should attack any NPC to maintain activity
     */
    private boolean shouldAttackForActivity() {
        Widget activity = Rs2Widget.getWidget(26738700); // Activity widget
        return activity != null && 
               activity.getChild(0).getWidth() <= LOW_ACTIVITY_THRESHOLD && 
               !Rs2Combat.inCombat();
    }
    
    /**
     * Attacks any available NPC to maintain activity level
     */
    private void attackAnyNpcForActivity() {
        Optional<Rs2NpcModel> attackableNpc = Rs2Npc.getAttackableNpcs().findFirst();
        attackableNpc.ifPresent(npc -> Rs2Npc.interact(npc.getId(), "attack"));
    }
    
    /**
     * Checks for and attacks nearby brawlers (priority threat)
     */
    private boolean shouldAttackNearbyBrawler() {
        var brawler = Rs2Npc.getNpc("brawler");
        if (brawler != null && 
            brawler.getWorldLocation().distanceTo(Rs2Player.getWorldLocation()) < BRAWLER_PROXIMITY_THRESHOLD) {
            
            Rs2Npc.interact(brawler, "attack");
            sleepUntil(() -> !Rs2Combat.inCombat());
            return true;
        }
        return false;
    }
    
    /**
     * Executes attack priorities based on configuration
     */
    private boolean executeAttackPriorities() {
        // Priority 1
        if (handleAttackByPriority(config.Priority1(), 1)) {
            return true;
        }
        
        // Priority 2
        if (handleAttackByPriority(config.Priority2(), 2)) {
            return true;
        }
        
        // Priority 3
        if (handleAttackByPriority(config.Priority3(), 3)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Handles attack based on priority configuration
     */
    private boolean handleAttackByPriority(PestControlNpc npcType, int priority) {
        if (npcType == null) return false;
        
        switch (npcType) {
            case BRAWLER:
                return attackBrawler();
            case PORTAL:
                return attackPortals();
            case SPINNER:
                return attackSpinner();
            default:
                return false;
        }
    }
    
    /**
     * Attacks any available target as fallback
     */
    private void attackAnyAvailableTarget() {
        Rs2NpcModel portal = Arrays.stream(Rs2Npc.getPestControlPortals()).findFirst().orElse(null);
        if (portal != null) {
            if (Rs2Npc.interact(portal.getId(), "attack")) {
                sleepUntil(() -> !Microbot.getClient().getLocalPlayer().isInteracting());
            }
        } else if (!Microbot.getClient().getLocalPlayer().isInteracting()) {
            Optional<Rs2NpcModel> attackableNpc = Rs2Npc.getAttackableNpcs().findFirst();
            attackableNpc.ifPresent(npc -> Rs2Npc.interact(npc.getId(), "attack"));
        }
    }    /**
     * Handles the waiting state (between games, in boat, etc.)
     */
    private void handleWaitingState() {
        plugin.lockCondition.unlock();
        Rs2Walker.setTarget(null);
        resetPortals();
        hasWalkedToCenter = false;
        
        sleep(Rs2Random.between(1600, 1800));
        
        if (!isInBoat() && !isInitializing) {
            enterBoat();
        } else if (config.alchInBoat() && !config.alchItem().isEmpty()) {
            Rs2Magic.alch(config.alchItem());
        }
    }
    
    /**
     * Enters the appropriate boat based on combat level
     */
    private void enterBoat() {
        int combatLevel = Microbot.getClient().getLocalPlayer().getCombatLevel();
        
        if (combatLevel >= 100) {
            Rs2GameObject.interact(25632);
        } else if (combatLevel >= 70) {
            Rs2GameObject.interact(25631);
        } else {
            Rs2GameObject.interact(14315);
        }
        
        sleepUntil(() -> Rs2Widget.getWidget(InterfaceID.PestLanderOverlay.INFO) != null, 3000);
    }

    // State checking methods
    public boolean isOutside() {
        return Microbot.getClient().getLocalPlayer().getWorldLocation()
                .distanceTo(new WorldPoint(2644, 2644, 0)) < 20;
    }

    public boolean isInBoat() {
        return Rs2Widget.getWidget(InterfaceID.PestLanderOverlay.INFO) != null;
    }

    public boolean isInPestControl() {
        return Rs2Widget.getWidget(InterfaceID.PestStatusOverlay.PEST_STATUS_PORT2) != null;
    }

    /**
     * Exits the boat based on combat level
     */
    public void exitBoat() {
        int combatLevel = Microbot.getClient().getLocalPlayer().getCombatLevel();
        
        if (combatLevel >= 100) {
            Rs2GameObject.interact(25630);
        } else if (combatLevel >= 70) {
            Rs2GameObject.interact(25629);
        } else {
            Rs2GameObject.interact(14314);
        }
        
        sleepUntil(() -> Rs2Widget.getWidget(InterfaceID.PestLanderOverlay.INFO) == null, 3000);
    }    /**
     * Finds the closest attackable portal
     */
    public Portal getClosestAttackablePortal() {
        List<Pair<Portal, Integer>> distancesToPortal = new ArrayList<>();
        
        for (Portal portal : PORTALS) {
            if (!portal.isHasShield() && !portal.getHitPoints().getText().trim().equals("0")) {
                distancesToPortal.add(Pair.of(portal, distanceToRegion(portal.getRegionX(), portal.getRegionY())));
            }
        }

        return distancesToPortal.stream()
                .min(Map.Entry.comparingByValue())
                .map(Pair::getKey)
                .orElse(null);
    }

    /**
     * Attacks a portal NPC
     */
    private static boolean attackPortal() {
        if (Microbot.getClient().getLocalPlayer().isInteracting()) {
            return false;
        }
        
        Rs2NpcModel npcPortal = Rs2Npc.getNpc("portal");
        if (npcPortal == null) {
            return false;
        }
        
        NPCComposition npc = Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getClient().getNpcDefinition(npcPortal.getId())).orElse(null);
        
        if (npc == null) {
            return false;
        }

        return Arrays.stream(npc.getActions())
                .anyMatch(action -> action != null && action.equalsIgnoreCase("attack")) &&
                Rs2Npc.interact(npcPortal, "attack");
    }

    /**
     * Attacks the closest available portal
     */
    private boolean attackPortals() {
        Portal closestAttackablePortal = getClosestAttackablePortal();
        if (closestAttackablePortal == null) {
            return false;
        }
        
        for (Portal portal : PORTALS) {
            if (isPortalAttackable(portal) && closestAttackablePortal == portal) {
                if (!Rs2Walker.isCloseToRegion(DISTANCE_TO_PORTAL, portal.getRegionX(), portal.getRegionY())) {                    WorldPoint portalLocation = WorldPoint.fromRegion(
                        Rs2Player.getWorldLocation().getRegionID(), 
                        portal.getRegionX(), 
                        portal.getRegionY(), 
                        Microbot.getClient().getTopLevelWorldView().getPlane()
                    );
                    Rs2Walker.walkTo(portalLocation, 5);
                }
                attackPortal();
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if a portal is attackable
     */
    private boolean isPortalAttackable(Portal portal) {
        return !portal.isHasShield() && !portal.getHitPoints().getText().trim().equals("0");
    }

    /**
     * Attacks any available spinner
     */
    private boolean attackSpinner() {
        for (int spinnerId : SPINNER_IDS) {
            if (Rs2Npc.interact(spinnerId, "attack")) {
                sleepUntil(() -> !Microbot.getClient().getLocalPlayer().isInteracting());
                return true;
            }
        }
        return false;
    }

    /**
     * Attacks any available brawler
     */
    private boolean attackBrawler() {
        for (int brawlerId : BRAWLER_IDS) {
            if (Rs2Npc.interact(brawlerId, "attack")) {
                sleepUntil(() -> !Microbot.getClient().getLocalPlayer().isInteracting());
                return true;
            }
        }
        return false;
    }

    @Override
    public void shutdown() {
        Microbot.log("Pest Control script shutting down");
        isInitializing = true;
        hasWalkedToCenter = false;
        super.shutdown();
    }
}
