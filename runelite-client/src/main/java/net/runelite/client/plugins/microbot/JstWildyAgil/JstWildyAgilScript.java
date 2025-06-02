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
    private static final int OBSTACLE_PIPE_ID = 23137; // Obstacle pipe (updated)
    private static final int ROPE_SWING_ID = 23132; // Rope swing  
    private static final int STEPPING_STONE_ID = 23556; // Stepping stone
    private static final int LOG_BALANCE_ID = 23542; // Log balance
    private static final int ROCKS_ID = 23640; // Climbing rocks (updated)
    private static final int TICKET_DISPENSER_ID = 53224; // Agility Arena Ticket Dispenser (updated)

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
    private static final WorldPoint OUTSIDE_GATE_POINT = new WorldPoint(2998, 3932, 0); // Just outside the gate

    // Wilderness Agility Course area bounds (approximate)
    private static final int COURSE_MIN_X = 2985;
    private static final int COURSE_MAX_X = 3007;
    private static final int COURSE_MIN_Y = 3930;
    private static final int COURSE_MAX_Y = 3965;

    // Accept all possible gate IDs for entrance/exit
    private static final int[] ENTRANCE_GATE_IDS = {23552, 23554, 23555};

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

    private boolean isInCourseArea() {
        WorldPoint loc = Rs2Player.getWorldLocation();
        return loc.getX() >= COURSE_MIN_X && loc.getX() <= COURSE_MAX_X &&
               loc.getY() >= COURSE_MIN_Y && loc.getY() <= COURSE_MAX_Y;
    }
    
    private WildyAgilState determineCurrentCourseState() {
        if (!isInCourseArea()) {
            return WildyAgilState.WALKING_TO_COURSE_ENTRANCE;
        }
        
        WorldPoint playerLoc = Rs2Player.getWorldLocation();
        
        // Check proximity to each obstacle completion point to determine state
        if (playerLoc.distanceTo(AFTER_ROCKS_LOCATION) < 6) {
            return WildyAgilState.COLLECTING_TOKENS;
        } else if (playerLoc.distanceTo(AFTER_LOG_BALANCE_LOCATION) < 6) {
            return WildyAgilState.CLIMBING_ROCKS;
        } else if (playerLoc.distanceTo(AFTER_STEPPING_STONES_LOCATION) < 6) {
            return WildyAgilState.CROSSING_LOG_BALANCE;
        } else if (playerLoc.distanceTo(AFTER_ROPESWING_LOCATION) < 10) {
            return WildyAgilState.CROSSING_STEPPING_STONES;
        } else if (playerLoc.distanceTo(AFTER_PIPE_LOCATION) < 6) {
            return WildyAgilState.CROSSING_ROPE_SWING;
        } else {
            // Near start of course
            return WildyAgilState.ENTERING_COURSE;
        }
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

                        // If already in course area, determine where we are and continue from there
                        if (isInCourseArea()) {
                            WildyAgilState currentState = determineCurrentCourseState();
                            Microbot.log("Already in course, continuing from: " + currentState);
                            SCRIPT_STATE = currentState;
                            initialMovementToObstacle = true;
                            break;
                        }

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
                        // If already inside the course area, skip gate interaction
                        if (isInCourseArea()) {
                            SCRIPT_STATE = WildyAgilState.ENTERING_COURSE;
                            break;
                        }
                        // Check voor coins als de gate gebruikt moet worden (onder level 52 wilderness)
                        if (Rs2Pvp.getWildernessLevelFrom(Rs2Player.getWorldLocation()) < 52 && Rs2Inventory.itemQuantity(COINS_NAME) < 500) {
                            Microbot.log("Niet genoeg coins voor de gate, banken...");
                            SCRIPT_STATE = WildyAgilState.BANKING;
                            break;
                        }
                        // If already near the gate, try to open it
                        if (Rs2Player.getWorldLocation().distanceTo(INSIDE_GATE_START_POINT) < 5) {
                            if (interactWithAnyGate("Open")) {
                                Global.sleepUntil(() -> isInCourseArea(), 5000);
                                if (isInCourseArea()) {
                                    Microbot.log("Successfully entered course through gate");
                                    SCRIPT_STATE = WildyAgilState.ENTERING_COURSE;
                                } else {
                                    Microbot.log("Kon de gate niet openen of erdoorheen gaan.");
                                    SCRIPT_STATE = WildyAgilState.ERROR;
                                }
                                break;
                            }
                        }
                        // If not near, walk to just outside the gate
                        if (!Rs2Walker.walkTo(OUTSIDE_GATE_POINT, 1)) {
                            Microbot.log("Kon niet naar de gate lopen.");
                            SCRIPT_STATE = WildyAgilState.ERROR;
                        }
                        sleep(1000, 2000);
                        break;

                    case ENTERING_COURSE:
                        Microbot.status = "Starting course...";
                        if (!isInCourseArea()) {
                            Microbot.log("Not in course area, returning to entrance...");
                            SCRIPT_STATE = WildyAgilState.WALKING_TO_COURSE_ENTRANCE;
                            break;
                        }
                        
                        // Check if we're already past the pipe
                        if (Rs2Player.getWorldLocation().distanceTo(AFTER_PIPE_LOCATION) < 4) {
                            SCRIPT_STATE = WildyAgilState.CROSSING_ROPE_SWING;
                            initialMovementToObstacle = true;
                            break;
                        }
                        
                        // Try to squeeze through pipe
                        if (handleObstacle(OBSTACLE_PIPE_ID, "Squeeze-through", AFTER_PIPE_LOCATION)) {
                            SCRIPT_STATE = WildyAgilState.CROSSING_ROPE_SWING;
                        }
                        break;

                    case CROSSING_ROPE_SWING:
                        Microbot.status = "Swinging on rope...";
                        if (!isInCourseArea()) {
                            Microbot.log("Not in course area, returning to entrance...");
                            SCRIPT_STATE = WildyAgilState.WALKING_TO_COURSE_ENTRANCE;
                            break;
                        }
                        
                        // Check if we're already past the rope swing
                        if (Rs2Player.getWorldLocation().distanceTo(AFTER_ROPESWING_LOCATION) < 8) {
                            SCRIPT_STATE = WildyAgilState.CROSSING_STEPPING_STONES;
                            initialMovementToObstacle = true;
                            break;
                        }
                        
                        if (walkAndHandleObstacle(BEFORE_ROPESWING_LOCATION, ROPE_SWING_ID, "Swing-on", AFTER_ROPESWING_LOCATION)) {
                            SCRIPT_STATE = WildyAgilState.CROSSING_STEPPING_STONES;
                        }
                        break;

                    case CROSSING_STEPPING_STONES:
                        Microbot.status = "Crossing stepping stones...";
                        if (!isInCourseArea()) {
                            Microbot.log("Not in course area, returning to entrance...");
                            SCRIPT_STATE = WildyAgilState.WALKING_TO_COURSE_ENTRANCE;
                            break;
                        }
                        
                        // Check if we're already past the stepping stones
                        if (Rs2Player.getWorldLocation().distanceTo(AFTER_STEPPING_STONES_LOCATION) < 4) {
                            SCRIPT_STATE = WildyAgilState.CROSSING_LOG_BALANCE;
                            initialMovementToObstacle = true;
                            break;
                        }
                        
                        if (walkAndHandleObstacle(BEFORE_STEPPING_STONES_LOCATION, STEPPING_STONE_ID, "Cross", AFTER_STEPPING_STONES_LOCATION)) {
                            SCRIPT_STATE = WildyAgilState.CROSSING_LOG_BALANCE;
                        }
                        break;

                    case CROSSING_LOG_BALANCE:
                        Microbot.status = "Crossing log balance...";
                        if (!isInCourseArea()) {
                            Microbot.log("Not in course area, returning to entrance...");
                            SCRIPT_STATE = WildyAgilState.WALKING_TO_COURSE_ENTRANCE;
                            break;
                        }
                        
                        // Check if we're already past the log balance
                        if (Rs2Player.getWorldLocation().distanceTo(AFTER_LOG_BALANCE_LOCATION) < 4) {
                            SCRIPT_STATE = WildyAgilState.CLIMBING_ROCKS;
                            initialMovementToObstacle = true;
                            break;
                        }
                        
                        if (walkAndHandleObstacle(BEFORE_LOG_BALANCE_LOCATION, LOG_BALANCE_ID, "Walk-across", AFTER_LOG_BALANCE_LOCATION)) {
                            SCRIPT_STATE = WildyAgilState.CLIMBING_ROCKS;
                        }
                        break;

                    case CLIMBING_ROCKS:
                        Microbot.status = "Climbing rocks...";
                        if (!isInCourseArea()) {
                            Microbot.log("Not in course area, returning to entrance...");
                            SCRIPT_STATE = WildyAgilState.WALKING_TO_COURSE_ENTRANCE;
                            break;
                        }
                        
                        // Check if we're already past the rocks
                        if (Rs2Player.getWorldLocation().distanceTo(AFTER_ROCKS_LOCATION) < 4) {
                            onLapCompleted();
                            SCRIPT_STATE = WildyAgilState.COLLECTING_TOKENS;
                            break;
                        }
                        
                        if (walkAndHandleObstacle(BEFORE_ROCKS_LOCATION, ROCKS_ID, "Climb", AFTER_ROCKS_LOCATION)) {
                            onLapCompleted();
                            SCRIPT_STATE = WildyAgilState.COLLECTING_TOKENS;
                        }
                        break;

                    case COLLECTING_TOKENS:
                        Microbot.status = "Collecting tokens...";
                        
                        // Try to loot any nearby tickets
                        boolean looted = Rs2GroundItem.loot(AGILITY_ARENA_TICKET_NAME, 15);
                        if (looted) {
                            Global.sleepUntil(() -> !Rs2Player.isAnimating(), 3000);
                        }

                        // Check food status first
                        if (config.useFood() && !Rs2Inventory.hasItem(config.foodName()) && 
                            Rs2Inventory.getEmptySlots() < config.foodAmount()) {
                            Microbot.log("Geen voedsel meer en te weinig ruimte om op te nemen, banken...");
                            SCRIPT_STATE = WildyAgilState.BANKING;
                            break;
                        }

                        // Handle full inventory or ticket threshold
                        if (config.handleTokens() && Rs2Inventory.hasItem(AGILITY_ARENA_TICKET_NAME) &&
                            (Rs2Inventory.isFull() || Rs2Inventory.count(AGILITY_ARENA_TICKET_NAME) >= 260)) {
                            SCRIPT_STATE = WildyAgilState.LEAVING_COURSE;
                        } else if (Rs2Inventory.isFull() && !config.handleTokens()) {
                            Microbot.log("Inventory vol, banken...");
                            SCRIPT_STATE = WildyAgilState.BANKING;
                        } else {
                            // Continue with another lap
                            SCRIPT_STATE = WildyAgilState.ENTERING_COURSE;
                            initialMovementToObstacle = true;
                        }
                        break;

                    case LEAVING_COURSE:
                        Microbot.status = "Leaving course...";
                        onCourseExitOrDeath();
                        
                        // If already outside the course area, go directly to ticket dispenser
                        if (!isInCourseArea()) {
                            SCRIPT_STATE = WildyAgilState.WALKING_TO_TICKET_DISPENSER;
                            break;
                        }
                        
                        // If already near the gate, try to open it
                        if (Rs2Player.getWorldLocation().distanceTo(EXIT_GATE_INTERACTION_POINT) < 5) {
                            if (interactWithAnyGate("Open")) {
                                Global.sleepUntil(() -> !isInCourseArea(), 5000);
                                if (!isInCourseArea()) {
                                    SCRIPT_STATE = WildyAgilState.WALKING_TO_TICKET_DISPENSER;
                                } else {
                                    Microbot.log("Kon de gate niet openen of erdoorheen gaan.");
                                    SCRIPT_STATE = WildyAgilState.ERROR;
                                }
                                break;
                            }
                        }
                        // If not near, walk to just inside the gate
                        if (!Rs2Walker.walkTo(EXIT_GATE_INTERACTION_POINT, 1)) {
                            Microbot.log("Kon niet naar de gate lopen.");
                            SCRIPT_STATE = WildyAgilState.ERROR;
                        }
                        sleep(1000, 2000);
                        break;

                    case WALKING_TO_TICKET_DISPENSER:
                        Microbot.status = "Walking to ticket dispenser...";
                        if (Rs2Player.getWorldLocation().distanceTo(TICKET_DISPENSER_AREA) <= 3) {
                            SCRIPT_STATE = WildyAgilState.HANDING_IN_TICKETS;
                            agilityTicketsInitialCount = Rs2Inventory.count(AGILITY_ARENA_TICKET_NAME);
                        } else {
                            if (!Rs2Walker.walkTo(TICKET_DISPENSER_AREA, 2)) {
                                Microbot.log("Failed to walk to ticket dispenser area");
                                sleep(1000, 2000);
                            }
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
        // If already at or past the expected end point, skip obstacle
        if (Rs2Player.getWorldLocation().distanceTo(expectedEndPoint) < 4) {
            Microbot.log("Already past obstacle at " + expectedEndPoint);
            initialMovementToObstacle = true;
            return true;
        }
        
        // Reset movement flag if we need to walk
        if (initialMovementToObstacle) {
            if (!Rs2Walker.walkTo(beforeObstaclePoint, 1)) {
                Microbot.log("Failed to walk to " + beforeObstaclePoint);
                return false;
            }
            Global.sleepUntil(() -> Rs2Player.getWorldLocation().distanceTo(beforeObstaclePoint) <= 2 || 
                                   Rs2GameObject.getTileObject(objectId) == null ||
                                   Rs2Player.getWorldLocation().distanceTo(expectedEndPoint) < 4, 5000);
            
            // Check if we somehow completed the obstacle during walking
            if (Rs2Player.getWorldLocation().distanceTo(expectedEndPoint) < 4) {
                Microbot.log("Completed obstacle during walk to " + expectedEndPoint);
                initialMovementToObstacle = true;
                return true;
            }
            
            initialMovementToObstacle = false;
        }

        // Try to interact with the obstacle
        if (Rs2GameObject.interact(objectId, action)) {
            Global.sleepUntil(() -> Rs2Player.getWorldLocation().distanceTo(expectedEndPoint) < 4 || 
                                   Rs2Player.isAnimating() || Rs2Player.isMoving(), 12000);
            sleep(600, 1200);
            
            if (Rs2Player.getWorldLocation().distanceTo(expectedEndPoint) < 4) {
                Microbot.log("Successfully completed obstacle to " + expectedEndPoint);
                initialMovementToObstacle = true;
                return true;
            } else {
                Microbot.log("Failed to reach " + expectedEndPoint + " after interacting with " + objectId);
                // Reset if stuck
                if (Rs2Player.getWorldLocation().distanceTo(beforeObstaclePoint) <= 3 && 
                    !Rs2Player.isMoving() && !Rs2Player.isAnimating()) {
                    Microbot.log("Player seems stuck, forcing re-walk");
                    initialMovementToObstacle = true;
                }
            }
        } else {
            Microbot.log("Failed to interact with object: " + objectId + " with action: " + action);
            // If object not found, maybe we're already past it
            if (Rs2GameObject.getTileObject(objectId) == null) {
                Microbot.log("Object " + objectId + " not found, checking if we're past it");
                if (Rs2Player.getWorldLocation().distanceTo(expectedEndPoint) < 8) {
                    initialMovementToObstacle = true;
                    return true;
                } else {
                    initialMovementToObstacle = true; // Try to re-evaluate path
                }
            }
        }
        return false;
    }
    
    private boolean handleObstacle(int objectId, String action, WorldPoint expectedEndPoint) {
        // If already at or past the expected end point, skip obstacle
        if (Rs2Player.getWorldLocation().distanceTo(expectedEndPoint) < 4) {
            Microbot.log("Already past obstacle at " + expectedEndPoint);
            initialMovementToObstacle = true;
            return true;
        }
        
        if (Rs2GameObject.interact(objectId, action)) {
            Global.sleepUntil(() -> Rs2Player.getWorldLocation().distanceTo(expectedEndPoint) < 4 || 
                                   Rs2Player.isAnimating() || Rs2Player.isMoving(), 12000);
            sleep(600, 1200); 
            
            if (Rs2Player.getWorldLocation().distanceTo(expectedEndPoint) < 4) {
                Microbot.log("Successfully completed obstacle to " + expectedEndPoint);
                initialMovementToObstacle = true;
                return true;
            } else {
                Microbot.log("Failed to reach " + expectedEndPoint + " after " + action);
            }
        } else {
            Microbot.log("Failed to interact with object: " + objectId + " with action: " + action);
            // If object not found, maybe we're already past it or need to walk closer
            if (Rs2GameObject.getTileObject(objectId) == null) {
                Microbot.log("Object " + objectId + " not found, checking if we're past it");
                if (Rs2Player.getWorldLocation().distanceTo(expectedEndPoint) < 8) {
                    initialMovementToObstacle = true;
                    return true;
                }
            }
        }
        return false;
    }

    private boolean interactWithAnyGate(String action) {
        for (int gateId : ENTRANCE_GATE_IDS) {
            if (Rs2GameObject.getTileObject(gateId) != null) {
                return Rs2GameObject.interact(gateId, action);
            }
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