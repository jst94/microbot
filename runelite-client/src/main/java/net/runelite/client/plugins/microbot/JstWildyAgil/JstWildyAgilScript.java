package net.runelite.client.plugins.microbot.JstWildyAgil;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.JstWildyAgil.enums.WildyAgilState;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank; // Bank import
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard; // Keyboard import
// import net.runelite.client.plugins.microbot.util.npc.Rs2Npc; // Niet direct nodig voor dispenser
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.player.Rs2Pvp;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.microbot.util.Global;

import java.util.concurrent.TimeUnit;

public class JstWildyAgilScript extends Script {

    public static double version = 1.2; // Versie bijgewerkt
    public static WildyAgilState SCRIPT_STATE = WildyAgilState.STARTING;

    // Constants voor Object IDs (gebaseerd op OSRS Wiki)
    private static final int OBSTACLE_PIPE_ID = 23131; // Obstacle pipe
    private static final int ROPE_SWING_ID = 23132; // Rope swing  
    private static final int STEPPING_STONE_ID = 23556; // Stepping stone
    private static final int LOG_BALANCE_ID = 23542; // Log balance
    private static final int ROCKS_ID = 23547; // Climbing rocks
    private static final int GATE_ID = 23555; // Wilderness Agility Course gate
    private static final int TICKET_DISPENSER_ID = 29084; // Agility Arena Ticket Dispenser

    private static final String AGILITY_ARENA_TICKET_NAME = "Agility arena ticket";
    private static final String COINS_NAME = "Coins";

    // WorldPoints
    private static final WorldPoint INSIDE_GATE_START_POINT = new WorldPoint(2998, 3917, 0);
    private static final WorldPoint AFTER_PIPE_LOCATION = new WorldPoint(2998, 3931, 0);
    private static final WorldPoint BEFORE_ROPESWING_LOCATION = new WorldPoint(2993, 3933, 0);
    private static final WorldPoint AFTER_ROPESWING_LOCATION = new WorldPoint(3005, 3953, 0);
    private static final WorldPoint BEFORE_STEPPING_STONES_LOCATION = new WorldPoint(3001, 3958, 0);
    private static final WorldPoint AFTER_STEPPING_STONES_LOCATION = new WorldPoint(3001, 3962, 0);
    private static final WorldPoint BEFORE_LOG_BALANCE_LOCATION = new WorldPoint(2995, 3960, 0);
    private static final WorldPoint AFTER_LOG_BALANCE_LOCATION = new WorldPoint(2994, 3945, 0);
    private static final WorldPoint BEFORE_ROCKS_LOCATION = new WorldPoint(2989, 3945, 0);
    private static final WorldPoint AFTER_ROCKS_LOCATION = new WorldPoint(2989, 3936, 0);
    private static final WorldPoint TICKET_DISPENSER_AREA = new WorldPoint(3050, 3930, 0); // Geschat
    private static final WorldPoint EXIT_GATE_INTERACTION_POINT = new WorldPoint(2998, 3933, 0); // Punt binnen de gate
    private static final WorldPoint EDGEVILLE_BANK_LOCATION = new WorldPoint(3094, 3495, 0); // Edgeville bank


    JstWildyAgilConfig config;
    private boolean initialMovementToObstacle = true;
    private int agilityTicketsInitialCount = -1;
    private int lapStreak = 0;
    private boolean paidFee = false; // Set this to true if player has paid 150k fee

    private int getRewardMultiplier(int streak) {
        if (streak >= 61) return 4;
        if (streak >= 31) return 3;
        if (streak >= 16) return 2;
        return 1;
    }

    private int getRewardValue(int streak) {
        int multiplier = getRewardMultiplier(streak);
        switch (multiplier) {
            case 1: return 8049;
            case 2: return 15935;
            case 3: return 23963;
            case 4: return 32582;
            default: return 0;
        }
    }

    private int getTicketXp(int ticketCount) {
        if (ticketCount >= 101) return 230;
        return 200 + (int)Math.floor((ticketCount - 1) * 30.0 / 100.0);
    }

    // Call this when a lap is completed
    private void onLapCompleted() {
        lapStreak++;
        if (paidFee) {
            int reward = getRewardValue(lapStreak);
            // Example: split reward (50% alchables, 50% noted blighted)
            int alchables = reward / 2;
            int blightedNoted = reward - alchables;
            Microbot.log("Lap " + lapStreak + " reward: " + reward + " (Alchables: " + alchables + ", Noted blighted: " + blightedNoted + ")");
            // If inventory has empty slot, give 1 unnoted blighted item
            if (Rs2Inventory.getEmptySlots() > 0) {
                Microbot.log("Gave 1 unnoted blighted item for empty slot");
            }
        }
        // Always give a ticket
        Microbot.log("Gave Wilderness agility ticket");
    }

    // Call this if player dies or leaves course
    private void onCourseExitOrDeath() {
        lapStreak = 0;
        Microbot.log("Lap streak reset to 0 (death or left course)");
    }

    // Call this if player logs out inside course
    private void onLogoutInsideCourse() {
        int before = lapStreak;
        lapStreak = Math.max(0, lapStreak - 10);
        Microbot.log("Lap streak reduced from " + before + " to " + lapStreak + " (logout inside course)");
    }

    public boolean run(JstWildyAgilConfig config) {
        this.config = config;
        Microbot.enableAutoRunOn = true;
        initialMovementToObstacle = true;
        agilityTicketsInitialCount = -1;
        SCRIPT_STATE = WildyAgilState.STARTING;

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!super.run()) return;
                if (!Microbot.isLoggedIn()) return;

                // Main loop
                switch (SCRIPT_STATE) {
                    case STARTING:
                        Microbot.status = "Starting Wilderness Agility...";

                        // Check 1: Need to bank for food?
                        // This check is done if food usage is enabled, a valid food name is configured,
                        // and the player doesn't currently have that food.
                        if (config.useFood() && config.foodName() != null && !config.foodName().trim().isEmpty() &&
                            !Rs2Inventory.hasItem(config.foodName())) {
                            Microbot.log("No " + config.foodName() + " in inventory and food usage is enabled. Proceeding to BANKING state.");
                            SCRIPT_STATE = WildyAgilState.BANKING;
                            break; // Important: exit switch for this tick to ensure banking state is processed next
                        }

                        // Check 2: Inventory status (full with tickets, full with other items, or not full)
                        // This is evaluated if banking for food wasn't triggered.
                        if (Rs2Inventory.isFull()) {
                            if (config.handleTokens() && Rs2Inventory.hasItem(AGILITY_ARENA_TICKET_NAME)) {
                                // Inventory is full, token handling is enabled, and player has tickets.
                                // Assume the inventory is full of tickets to be handed in.
                                Microbot.log("Inventory is full with Agility Tickets. Leaving course to hand them in.");
                                SCRIPT_STATE = WildyAgilState.LEAVING_COURSE;
                            } else {
                                // Inventory is full, but either:
                                // 1. Token handling is disabled.
                                // 2. Token handling is enabled, but the inventory isn't full of tickets (e.g., other items).
                                // In these scenarios, bank to clear unwanted items.
                                Microbot.log("Inventory is full (not with tickets for hand-in, or token handling is disabled). Proceeding to BANKING state.");
                                SCRIPT_STATE = WildyAgilState.BANKING;
                            }
                        } else {
                            // Inventory is not full, and banking for food is not needed.
                            // Proceed to the agility course.
                            SCRIPT_STATE = WildyAgilState.WALKING_TO_COURSE_ENTRANCE;
                        }
                        break;

                    case WALKING_TO_COURSE_ENTRANCE:
                        Microbot.status = "Walking to course entrance...";
                        // Check voor coins als de gate gebruikt moet worden (onder level 52 wilderness)
                        if (Rs2Pvp.getWildernessLevelFrom(Rs2Player.getWorldLocation()) < 52 && Rs2Inventory.itemQuantity(COINS_NAME) < 500) {
                            Microbot.log("Niet genoeg coins voor de gate, banken...");
                            SCRIPT_STATE = WildyAgilState.BANKING;
                            break;
                        }
                        if (Rs2Player.getWorldLocation().distanceTo(INSIDE_GATE_START_POINT) < 15) {
                             SCRIPT_STATE = WildyAgilState.ENTERING_COURSE;
                        } else {
                            Microbot.log("Te ver van het parcourse, loop er handmatig heen of gebruik teleport.");
                            if (!Rs2Walker.walkTo(new WorldPoint(2998, 3916, 0))) { // Punt net buiten de gate
                                Microbot.log("Kon niet naar de gate lopen.");
                                SCRIPT_STATE = WildyAgilState.ERROR;
                            }
                             sleep(1000, 2000);
                        }
                        break;

                    case ENTERING_COURSE:
                         Microbot.status = "Squeezing through pipe...";
                         if (handleObstacle(OBSTACLE_PIPE_ID, "Squeeze-through", AFTER_PIPE_LOCATION)) {
                             SCRIPT_STATE = WildyAgilState.CROSSING_PIPE;
                         }
                        break;

                    case CROSSING_PIPE:
                        Microbot.status = "Crossing pipe...";
                        // After completing the pipe, move to rope swing
                        SCRIPT_STATE = WildyAgilState.CROSSING_ROPE_SWING;
                        break;

                    case CROSSING_ROPE_SWING:
                        Microbot.status = "Swinging on rope...";
                        if (walkAndHandleObstacle(BEFORE_ROPESWING_LOCATION, ROPE_SWING_ID, "Swing-on", AFTER_ROPESWING_LOCATION)) {
                             SCRIPT_STATE = WildyAgilState.CROSSING_STEPPING_STONES;
                        }
                        break;

                    case CROSSING_STEPPING_STONES:
                        Microbot.status = "Crossing stepping stones...";
                        if (walkAndHandleObstacle(BEFORE_STEPPING_STONES_LOCATION, STEPPING_STONE_ID, "Cross", AFTER_STEPPING_STONES_LOCATION)) {
                            SCRIPT_STATE = WildyAgilState.CROSSING_LOG_BALANCE;
                        }
                        break;

                    case CROSSING_LOG_BALANCE:
                        Microbot.status = "Crossing log balance...";
                        if (walkAndHandleObstacle(BEFORE_LOG_BALANCE_LOCATION, LOG_BALANCE_ID, "Walk-across", AFTER_LOG_BALANCE_LOCATION)) {
                            SCRIPT_STATE = WildyAgilState.CLIMBING_ROCKS;
                            // Lap is completed after log balance and rocks
                        }
                        break;

                    case CLIMBING_ROCKS:
                        Microbot.status = "Climbing rocks...";
                        if (walkAndHandleObstacle(BEFORE_ROCKS_LOCATION, ROCKS_ID, "Climb", AFTER_ROCKS_LOCATION)) {
                            // Lap completed here
                            onLapCompleted();
                            SCRIPT_STATE = WildyAgilState.COLLECTING_TOKENS;
                        }
                        break;

                    case COLLECTING_TOKENS:
                        Microbot.status = "Collecting tokens...";
                        boolean looted = Rs2GroundItem.loot(AGILITY_ARENA_TICKET_NAME, 15);
                        if (looted) {
                            Global.sleepUntil(() -> !Rs2Player.isAnimating(), 3000);
                        }

                        // Logica om te banken als voedsel op is
                        if (config.useFood() && !Rs2Inventory.hasItem(config.foodName()) && Rs2Inventory.getEmptySlots() < config.foodAmount()) {
                            Microbot.log("Geen voedsel meer en te weinig ruimte om op te nemen, banken...");
                            SCRIPT_STATE = WildyAgilState.BANKING;
                            break;
                        }


                        if (config.handleTokens() && Rs2Inventory.hasItem(AGILITY_ARENA_TICKET_NAME) &&
                            (Rs2Inventory.isFull() || Rs2Inventory.count(AGILITY_ARENA_TICKET_NAME) >= 260)) {
                             SCRIPT_STATE = WildyAgilState.LEAVING_COURSE;
                        } else if (Rs2Inventory.isFull() && !config.handleTokens()) { // Inventory vol en we gaan geen tickets inleveren
                            Microbot.log("Inventory vol, banken...");
                            SCRIPT_STATE = WildyAgilState.BANKING;
                        }
                        else {
                             SCRIPT_STATE = WildyAgilState.ENTERING_COURSE;
                            initialMovementToObstacle = true;
                        }
                        break;

                    case LEAVING_COURSE:
                         Microbot.status = "Leaving course...";
                         // If player leaves course, reset lap streak
                        onCourseExitOrDeath();
                        if (Rs2Walker.walkTo(EXIT_GATE_INTERACTION_POINT, 1)) {
                            if (Rs2GameObject.interact(GATE_ID, "Open")) {
                                Global.sleepUntil(() -> Rs2Player.getWorldLocation().getY() < EXIT_GATE_INTERACTION_POINT.getY() -1, 5000);
                                if (Rs2Player.getWorldLocation().getY() < EXIT_GATE_INTERACTION_POINT.getY() -1) {
                                    SCRIPT_STATE = WildyAgilState.WALKING_TO_TICKET_DISPENSER;
                                } else {
                                    Microbot.log("Kon de gate niet openen of erdoorheen gaan.");
                                    SCRIPT_STATE = WildyAgilState.ERROR;
                                }
                            }
                        }
                        break;

                    case WALKING_TO_TICKET_DISPENSER:
                        Microbot.status = "Walking to ticket dispenser...";
                        if (Rs2Walker.walkTo(TICKET_DISPENSER_AREA, 2)) {
                            SCRIPT_STATE = WildyAgilState.HANDING_IN_TICKETS;
                            agilityTicketsInitialCount = Rs2Inventory.count(AGILITY_ARENA_TICKET_NAME);
                        }
                        break;

                    case HANDING_IN_TICKETS:
                        Microbot.status = "Handing in tickets...";
                        if (!Rs2Inventory.hasItem(AGILITY_ARENA_TICKET_NAME)) {
                            Microbot.log("Geen tickets om in te leveren.");
                            SCRIPT_STATE = WildyAgilState.STARTING; // Terug naar start
                            break;
                        }
                        if (Rs2GameObject.interact(TICKET_DISPENSER_ID, "Exchange")) {
                            Global.sleepUntil(() -> Rs2Inventory.count(AGILITY_ARENA_TICKET_NAME) < agilityTicketsInitialCount || Rs2Widget.getWidget(219, 1) != null, 8000);
                            if (Rs2Inventory.count(AGILITY_ARENA_TICKET_NAME) < agilityTicketsInitialCount) {
                                // Calculate XP per ticket
                                int turnedIn = agilityTicketsInitialCount - Rs2Inventory.count(AGILITY_ARENA_TICKET_NAME);
                                int xpPerTicket = getTicketXp(turnedIn);
                                Microbot.log("Tickets handed in! XP per ticket: " + xpPerTicket);
                                agilityTicketsInitialCount = -1;
                                SCRIPT_STATE = WildyAgilState.STARTING;
                            } else if (Rs2Widget.getWidget(219,1) != null) {
                                Rs2Keyboard.keyPress('1');
                                Global.sleepUntil(() -> Rs2Inventory.count(AGILITY_ARENA_TICKET_NAME) < agilityTicketsInitialCount, 5000);
                                if (Rs2Inventory.count(AGILITY_ARENA_TICKET_NAME) < agilityTicketsInitialCount) {
                                    int turnedIn = agilityTicketsInitialCount - Rs2Inventory.count(AGILITY_ARENA_TICKET_NAME);
                                    int xpPerTicket = getTicketXp(turnedIn);
                                    Microbot.status = "Tickets handed in via dialogue! XP per ticket: " + xpPerTicket;
                                    agilityTicketsInitialCount = -1;
                                } else {
                                    Microbot.log("Failed to hand in tickets via dialogue.");
                                }
                                SCRIPT_STATE = WildyAgilState.STARTING;
                            }
                        } else {
                            Microbot.log("Could not find/interact with Ticket Dispenser.");
                            SCRIPT_STATE = WildyAgilState.ERROR;
                        }
                        break;

                    case BANKING:
                        Microbot.status = "Banking...";
                        if (Rs2Player.getWorldLocation().distanceTo(EDGEVILLE_BANK_LOCATION) > 6) {
                            Rs2Walker.walkTo(EDGEVILLE_BANK_LOCATION, 1);
                            sleep(1000, 1500); // Geef wat tijd om te lopen
                            break;
                        }

                        if (!Rs2Bank.isOpen()) {
                            Rs2Bank.openBank();
                            Global.sleepUntil(Rs2Bank::isOpen, 5000);
                            break; // Wacht tot bank open is in volgende iteratie
                        }

                        // Stort alles (Graceful wordt gedragen, dus niet in inventory normaal gesproken)
                        if (!Rs2Inventory.isEmpty()) {
                            Rs2Bank.depositAll();
                            Global.sleepUntil(Rs2Inventory::isEmpty, 3000);
                        }
                        
                        // Neem food op als nodig
                        if (config.useFood() && !config.foodName().isEmpty() && !config.foodName().isBlank()) {
                            int foodInInventory = Rs2Inventory.itemQuantity(config.foodName());
                            if (foodInInventory < config.foodAmount()) {
                                int amountToWithdraw = config.foodAmount() - foodInInventory;
                                if (Rs2Bank.hasItem(config.foodName())) {
                                    Rs2Bank.withdrawX(config.foodName(), amountToWithdraw);
                                    Global.sleepUntil(() -> Rs2Inventory.itemQuantity(config.foodName()) >= config.foodAmount(), 3000);
                                } else {
                                    Microbot.log("Bank: Geen " + config.foodName() + " meer! Script stopt.");
                                    shutdown(); // Stoppen als food op is en nodig
                                    break;
                                }
                            }
                        }

                        // Zorg voor genoeg coins voor de gate (minimaal 500)
                        if (Rs2Inventory.itemQuantity(COINS_NAME) < 500) {
                            if (Rs2Bank.hasItem(COINS_NAME)) {
                                Rs2Bank.withdrawX(COINS_NAME, 1000); // Neem wat extra op
                                Global.sleepUntil(() -> Rs2Inventory.itemQuantity(COINS_NAME) >= 500, 3000);
                            } else {
                                Microbot.log("Bank: Geen coins meer! Script stopt.");
                                shutdown(); // Stoppen als geen coins en nodig
                                break;
                            }
                        }

                        if (Rs2Bank.isOpen()) {
                            Rs2Bank.closeBank();
                            Global.sleepUntil(() -> !Rs2Bank.isOpen(), 3000);
                        }
                        SCRIPT_STATE = WildyAgilState.WALKING_TO_COURSE_ENTRANCE; // Terug naar het parcourse
                        initialMovementToObstacle = true;
                        break;

                    case HANDLING_DANGER:
                        Microbot.status = "Handling danger...";
                        // Basic anti-PK logic: try to teleport out, else logout
                        boolean teleported = false;
                        // Example: try to use an amulet of glory or other teleport (pseudo-code, replace with real logic)
                        if (Rs2Inventory.hasItem("Amulet of glory(6)") || Rs2Inventory.hasItem("Amulet of glory(5)") || Rs2Inventory.hasItem("Amulet of glory(4)") || Rs2Inventory.hasItem("Amulet of glory(3)") || Rs2Inventory.hasItem("Amulet of glory(2)") || Rs2Inventory.hasItem("Amulet of glory(1)")) {
                            Microbot.log("PK detected! Attempting to teleport with Amulet of glory...");
                            teleported = Rs2Inventory.interact("Amulet of glory", "Edgeville");
                            sleep(2000, 3000);
                        }
                        // If not teleported, try logout as a last resort
                        if (!teleported) {
                            Microbot.log("Teleport failed or unavailable. Attempting to logout...");
                            Rs2Widget.clickWidget(182, 6); // Click the logout button
                            sleep(2000, 3000);
                        }
                        SCRIPT_STATE = WildyAgilState.STARTING; // Or a safer state
                        break;
                    case FINISHED:
                        Microbot.status = "Script finished.";
                        shutdown();
                        break;
                    case ERROR:
                        Microbot.status = "Error state, stopping script.";
                        shutdown();
                        break;
                }
            } catch (Exception e) {
                System.out.println("Error in script: " + e.getMessage());
                e.printStackTrace();
                SCRIPT_STATE = WildyAgilState.ERROR;
            }
        }, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }

    private boolean walkAndHandleObstacle(WorldPoint beforeObstaclePoint, int objectId, String action, WorldPoint expectedEndPoint) {
        if (initialMovementToObstacle) {
            if (!Rs2Walker.walkTo(beforeObstaclePoint, 1)) {
                 Microbot.log("Failed to walk to " + beforeObstaclePoint);
                return false;
            }
            Global.sleepUntil(() -> Rs2Player.getWorldLocation().distanceTo(beforeObstaclePoint) <= 1 || Rs2GameObject.getTileObject(objectId) == null, 3000);
            // Check if obstacle is still there, otherwise assume it was done or disappeared.
            if (Rs2GameObject.getTileObject(objectId) == null && Rs2Player.getWorldLocation().distanceTo(expectedEndPoint) < 4) {
                initialMovementToObstacle = true;
                return true;
            }
            initialMovementToObstacle = false;
        }

        if (Rs2GameObject.interact(objectId, action)) {
            Global.sleepUntil(() -> Rs2Player.getWorldLocation().distanceTo(expectedEndPoint) < 4 || Rs2Player.isAnimating() || Rs2Player.isMoving(), 10000);
            sleep(600, 1200);
             if (Rs2Player.getWorldLocation().distanceTo(expectedEndPoint) < 4) {
                initialMovementToObstacle = true;
                return true;
            } else {
                Microbot.log("Failed to reach " + expectedEndPoint + " after interacting with " + objectId);
                // Reset if stuck
                 if (Rs2Player.getWorldLocation().distanceTo(beforeObstaclePoint) <=2 && !Rs2Player.isMoving() && !Rs2Player.isAnimating()) {
                    initialMovementToObstacle = true; // Force re-walk
                 }
            }
        } else {
            Microbot.log("Failed to interact with object: " + objectId + " with action: " + action);
            // If object not found after trying to walk to it, maybe it's done, or we are stuck
            if (Rs2GameObject.getTileObject(objectId) == null) {
                 initialMovementToObstacle = true; // Try to re-evaluate path
            }
        }
        return false;
    }
    
    private boolean handleObstacle(int objectId, String action, WorldPoint expectedEndPoint) {
        if (Rs2GameObject.interact(objectId, action)) {
            Global.sleepUntil(() -> Rs2Player.getWorldLocation().distanceTo(expectedEndPoint) < 4 || Rs2Player.isAnimating() || Rs2Player.isMoving(), 10000);
            sleep(600, 1200); 
            if (Rs2Player.getWorldLocation().distanceTo(expectedEndPoint) < 4) {
                initialMovementToObstacle = true;
                return true;
            } else {
                Microbot.log("Failed to reach " + expectedEndPoint + " after Squeeze-through pipe");
            }
        } else {
             Microbot.log("Failed to interact with pipe: " + objectId + " with action: " + action);
        }
        return false;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        Microbot.status = "JstWildyAgil script SHUTDOWN";
        // SCRIPT_STATE = WildyAgilState.STARTING; // Geen reset hier, laat het main script dit doen bij opnieuw opstarten
    }
}