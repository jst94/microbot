package net.runelite.client.plugins.microbot.JstWildyAgil;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.StatChanged;
import net.runelite.api.MenuAction;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Skill;
import net.runelite.api.Player;
import net.runelite.client.eventbus.Subscribe;
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
import java.util.HashMap;
import java.util.Map;

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
    private static final WorldPoint BEFORE_PIPE_LOCATION = new WorldPoint(2998, 3916, 0); // Precise position before pipe
    private static final WorldPoint AFTER_PIPE_LOCATION = new WorldPoint(2998, 3931, 0); // Fixed coordinate
    private static final WorldPoint BEFORE_ROPESWING_LOCATION = new WorldPoint(2993, 3933, 0);
    private static final WorldPoint AFTER_ROPESWING_LOCATION = new WorldPoint(3005, 3953, 0);
    private static final WorldPoint BEFORE_STEPPING_STONES_LOCATION = new WorldPoint(3001, 3958, 0);
    private static final WorldPoint AFTER_STEPPING_STONES_LOCATION = new WorldPoint(3001, 3962, 0);
    private static final WorldPoint BEFORE_LOG_BALANCE_LOCATION = new WorldPoint(2995, 3960, 0);
    private static final WorldPoint AFTER_LOG_BALANCE_LOCATION = new WorldPoint(2994, 3945, 0);
    private static final WorldPoint BEFORE_ROCKS_LOCATION = new WorldPoint(2989, 3945, 0);
    private static final WorldPoint AFTER_ROCKS_LOCATION = new WorldPoint(2989, 3936, 0);
    
    // Memory/packet system position constants
    private static final WorldPoint PIPE_LOCATION = new WorldPoint(2998, 3916, 0);
    private static final WorldPoint ROPESWING_LOCATION = new WorldPoint(2993, 3933, 0);
    private static final WorldPoint STEPPING_STONE_LOCATION = new WorldPoint(3001, 3958, 0);
    private static final WorldPoint LOG_BALANCE_LOCATION = new WorldPoint(2995, 3960, 0);
    private static final WorldPoint ROCKS_LOCATION = new WorldPoint(2989, 3945, 0);
    
    private static final WorldPoint ROPE_SWING_START_POINT = new WorldPoint(2993, 3933, 0);
    private static final WorldPoint STEPPING_STONES_START_POINT = new WorldPoint(3001, 3958, 0);
    private static final WorldPoint LOG_BALANCE_START_POINT = new WorldPoint(2995, 3960, 0);
    private static final WorldPoint ROCKS_START_POINT = new WorldPoint(2989, 3945, 0);
    private static final WorldPoint ROCKS_END_POINT = new WorldPoint(2989, 3936, 0);
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
    
    // Updated object IDs for memory/packet system compatibility  
    private static final int PIPE_OBSTACLE_ID = 23137;
    private static final int ROPESWING_OBSTACLE_ID = 23132;
    private static final int STEPPING_STONE_OBSTACLE_ID = 23556;
    private static final int LOG_BALANCE_OBSTACLE_ID = 23542;
    private static final int ROCKS_OBSTACLE_ID = 23640;
    
    // Memory/Packet-based obstacle detection system
    private String lastMenuTarget = "";
    private String lastMenuOption = "";
    private long lastMenuClickTime = 0;
    private boolean waitingForObstacleCompletion = false;
    private WildyAgilState expectedStateAfterObstacle = null;
    private long obstacleInteractionStartTime = 0;
    private int lastAgilityExperience = -1;
    
    // Direct game state monitoring
    private WorldPoint lastPlayerPosition = null;
    
    // Obstacle interaction state tracking
    private Map<String, Long> obstacleLastAttempt = new HashMap<>();
    private Map<String, Integer> obstacleAttemptCount = new HashMap<>();

    JstWildyAgilConfig config;
    private boolean initialMovementToObstacle = true;
    private int agilityTicketsInitialCount = -1;
    private int lapStreak = 0;
    private boolean paidFee = false; // Set this to true if player has paid 150k fee
    private int bankingAttempts = 0; // Track banking attempts to prevent infinite loops
    
    // Pathing failure tracking
    private int pathingFailureCount = 0; // Track consecutive pathing failures
    private int obstacleFailureCount = 0; // Track consecutive obstacle interaction failures
    private String lastFailedObstacle = ""; // Track which obstacle is causing issues
    private long lastPathingFailureTime = 0; // Track timing between attempts

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
        bankingAttempts = 0; // Reset banking attempts
        resetPathingCounters(); // Reset pathing failure counters
        // Initialize memory/packet-based obstacle tracking
        resetObstacleWaitingState();
        lastAgilityExperience = Microbot.getClient().getRealSkillLevel(Skill.AGILITY);
        SCRIPT_STATE = WildyAgilState.STARTING;

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!super.run()) return;
                if (!Microbot.isLoggedIn()) return;

                // Emergency recovery check - handle severe pathing issues
                if (needsEmergencyRecovery()) {
                    handleSeverePathingIssues();
                    return; // Skip this tick to let recovery take effect
                }

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

                        // Reset banking attempts when starting fresh
                        bankingAttempts = 0;

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
                        // Only check for coins if we're actually at the lower wilderness level where gate fee is required
                        try {
                            int wildernessLevel = Rs2Pvp.getWildernessLevelFrom(Rs2Player.getWorldLocation());
                            if (wildernessLevel > 0 && wildernessLevel < 52 && Rs2Inventory.itemQuantity(COINS_NAME) < 500) {
                                Microbot.log("Niet genoeg coins voor de gate (wilderness level " + wildernessLevel + "), banken...");
                                SCRIPT_STATE = WildyAgilState.BANKING;
                                break;
                            }
                        } catch (Exception e) {
                            // If wilderness level check fails, assume we might need coins anyway
                            if (Rs2Inventory.itemQuantity(COINS_NAME) < 500) {
                                Microbot.log("Cannot determine wilderness level, but missing coins - banking for safety...");
                                SCRIPT_STATE = WildyAgilState.BANKING;
                                break;
                            }
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
                        
                        if (waitingForObstacleCompletion) {
                            // Don't advance state - wait for memory/packet confirmation
                            break;
                        }
                        
                        if (!handleObstacleWithMemoryPackets()) {
                            // Method handles the obstacle interaction and sets waiting state
                            // We won't advance until memory/packet system confirms completion
                            break;
                        }
                        
                        // Old immediate advancement logic removed - now handled by memory/packet system
                        break;

                    case CROSSING_ROPE_SWING:
                        Microbot.status = "Swinging on rope...";
                        if (!isInCourseArea()) {
                            Microbot.log("Not in course area, returning to entrance...");
                            SCRIPT_STATE = WildyAgilState.WALKING_TO_COURSE_ENTRANCE;
                            break;
                        }
                        
                        if (waitingForObstacleCompletion) {
                            // Don't advance state - wait for memory/packet confirmation
                            break;
                        }
                        
                        if (!handleObstacleWithMemoryPackets()) {
                            // Method handles the obstacle interaction and sets waiting state
                            break;
                        }
                        
                        // Old immediate advancement logic removed
                        break;

                    case CROSSING_STEPPING_STONES:
                        Microbot.status = "Crossing stepping stones...";
                        if (!isInCourseArea()) {
                            Microbot.log("Not in course area, returning to entrance...");
                            SCRIPT_STATE = WildyAgilState.WALKING_TO_COURSE_ENTRANCE;
                            break;
                        }
                        
                        if (waitingForObstacleCompletion) {
                            // Don't advance state - wait for memory/packet confirmation
                            break;
                        }
                        
                        if (!handleObstacleWithMemoryPackets()) {
                            // Method handles the obstacle interaction and sets waiting state
                            break;
                        }
                        
                        // Old immediate advancement logic removed
                        break;

                    case CROSSING_LOG_BALANCE:
                        Microbot.status = "Crossing log balance...";
                        if (!isInCourseArea()) {
                            Microbot.log("Not in course area, returning to entrance...");
                            SCRIPT_STATE = WildyAgilState.WALKING_TO_COURSE_ENTRANCE;
                            break;
                        }
                        
                        if (waitingForObstacleCompletion) {
                            // Don't advance state - wait for memory/packet confirmation
                            break;
                        }
                        
                        if (!handleObstacleWithMemoryPackets()) {
                            // Method handles the obstacle interaction and sets waiting state
                            break;
                        }
                        
                        // Old immediate advancement logic removed
                        break;

                    case CLIMBING_ROCKS:
                        Microbot.status = "Climbing rocks...";
                        if (!isInCourseArea()) {
                            Microbot.log("Not in course area, returning to entrance...");
                            SCRIPT_STATE = WildyAgilState.WALKING_TO_COURSE_ENTRANCE;
                            break;
                        }
                        
                        if (waitingForObstacleCompletion) {
                            // Don't advance state - wait for memory/packet confirmation
                            break;
                        }
                        
                        if (!handleObstacleWithMemoryPackets()) {
                            // Method handles the obstacle interaction and sets waiting state
                            break;
                        }
                        
                        // Old immediate advancement logic removed
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
                            bankingAttempts = 0; // Reset banking attempts when continuing normally
                            resetPathingCounters(); // Reset pathing counters when starting new lap
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
                        
                        // Prevent infinite banking loops
                        bankingAttempts++;
                        if (bankingAttempts > 5) {
                            Microbot.log("Too many banking attempts (" + bankingAttempts + "), stopping script to prevent infinite loop");
                            SCRIPT_STATE = WildyAgilState.ERROR;
                            break;
                        }
                        
                        // First, try to walk to bank if we're not close enough
                        if (Rs2Player.getWorldLocation().distanceTo(EDGEVILLE_BANK_LOCATION) > 6) {
                            Microbot.log("Walking to Edgeville bank... (attempt " + bankingAttempts + ")");
                            if (!Rs2Walker.walkTo(EDGEVILLE_BANK_LOCATION, 1)) {
                                Microbot.log("Failed to walk to bank, trying again...");
                                sleep(2000, 3000);
                                break;
                            }
                            sleep(1000, 1500);
                            break;
                        }

                        // Try to open bank if it's not already open
                        if (!Rs2Bank.isOpen()) {
                            Microbot.log("Opening bank...");
                            if (!Rs2Bank.openBank()) {
                                Microbot.log("Failed to open bank, trying again...");
                                sleep(1000, 2000);
                                break;
                            }
                            Global.sleepUntil(Rs2Bank::isOpen, 5000);
                            if (!Rs2Bank.isOpen()) {
                                Microbot.log("Bank didn't open, trying again...");
                                sleep(1000, 2000);
                                break;
                            }
                            // Give the bank interface time to fully load
                            sleep(500, 1000);
                        }

                        try {
                            // Deposit all items except what we need (keep graceful equipped)
                            if (!Rs2Inventory.isEmpty()) {
                                Microbot.log("Depositing all items...");
                                Rs2Bank.depositAll();
                                Global.sleepUntil(Rs2Inventory::isEmpty, 3000);
                                if (!Rs2Inventory.isEmpty()) {
                                    Microbot.log("Failed to deposit items, trying again...");
                                    sleep(1000, 2000);
                                    break;
                                }
                            }
                            
                            // Handle food withdrawal if needed
                            boolean needFood = config.useFood() && !config.foodName().isEmpty() && !config.foodName().isBlank();
                            if (needFood) {
                                int foodInInventory = Rs2Inventory.itemQuantity(config.foodName());
                                if (foodInInventory < config.foodAmount()) {
                                    int amountToWithdraw = config.foodAmount() - foodInInventory;
                                    Microbot.log("Need " + amountToWithdraw + " " + config.foodName() + " from bank...");
                                    
                                    if (Rs2Bank.hasItem(config.foodName())) {
                                        Rs2Bank.withdrawX(config.foodName(), amountToWithdraw);
                                        Global.sleepUntil(() -> Rs2Inventory.itemQuantity(config.foodName()) >= config.foodAmount(), 3000);
                                        
                                        if (Rs2Inventory.itemQuantity(config.foodName()) < config.foodAmount()) {
                                            Microbot.log("Failed to withdraw enough food, trying again...");
                                            sleep(1000, 2000);
                                            break;
                                        }
                                    } else {
                                        Microbot.log("Bank: Geen " + config.foodName() + " meer! Script stopt.");
                                        SCRIPT_STATE = WildyAgilState.ERROR;
                                        break;
                                    }
                                }
                            }

                            // Handle coins withdrawal - only if we actually need them
                            int coinsInInventory = Rs2Inventory.itemQuantity(COINS_NAME);
                            boolean needCoins = false;
                            
                            // Check if we might need coins for the wilderness agility course gate
                            try {
                                WorldPoint currentLoc = Rs2Player.getWorldLocation();
                                int wildernessLevel = Rs2Pvp.getWildernessLevelFrom(currentLoc);
                                // Need coins if in lower wilderness (below level 52) or if we can't determine location
                                needCoins = (wildernessLevel > 0 && wildernessLevel < 52) || wildernessLevel == 0;
                            } catch (Exception e) {
                                // If we can't determine wilderness level, assume we might need coins
                                needCoins = true;
                            }
                            
                            if (needCoins && coinsInInventory < 500) {
                                Microbot.log("Need coins from bank (have " + coinsInInventory + ", need 500)...");
                                
                                if (Rs2Bank.hasItem(COINS_NAME)) {
                                    int amountToWithdraw = Math.max(1000, 500 - coinsInInventory); // Withdraw at least 1000 for buffer
                                    Rs2Bank.withdrawX(COINS_NAME, amountToWithdraw);
                                    Global.sleepUntil(() -> Rs2Inventory.itemQuantity(COINS_NAME) >= 500, 3000);
                                    
                                    if (Rs2Inventory.itemQuantity(COINS_NAME) < 500) {
                                        Microbot.log("Failed to withdraw enough coins, trying again...");
                                        sleep(1000, 2000);
                                        break;
                                    }
                                } else {
                                    Microbot.log("Bank: Geen coins meer! Script stopt.");
                                    SCRIPT_STATE = WildyAgilState.ERROR;
                                    break;
                                }
                            } else if (!needCoins) {
                                Microbot.log("Coins not needed for current location, skipping coin withdrawal");
                            } else {
                                Microbot.log("Already have enough coins (" + coinsInInventory + "/500)");
                            }

                            // Close bank and continue
                            Microbot.log("Banking completed successfully, returning to course...");
                            Rs2Bank.closeBank();
                            Global.sleepUntil(() -> !Rs2Bank.isOpen(), 3000);
                            
                            // Reset banking attempts on successful banking
                            bankingAttempts = 0;
                            
                            // Return to course entrance
                            SCRIPT_STATE = WildyAgilState.WALKING_TO_COURSE_ENTRANCE;
                            initialMovementToObstacle = true;
                            
                        } catch (Exception e) {
                            Microbot.log("Error during banking: " + e.getMessage());
                            // Close bank if it's still open and retry
                            if (Rs2Bank.isOpen()) {
                                Rs2Bank.closeBank();
                                Global.sleepUntil(() -> !Rs2Bank.isOpen(), 2000);
                            }
                            sleep(2000, 3000);
                        }
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

    // Event-driven obstacle detection system
    
    /**
     * Handle menu option clicks to detect obstacle interactions
     */
    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        // Only track interactions with game objects (obstacles)
        if (event.getMenuAction() == MenuAction.GAME_OBJECT_FIRST_OPTION ||
            event.getMenuAction() == MenuAction.GAME_OBJECT_SECOND_OPTION ||
            event.getMenuAction() == MenuAction.GAME_OBJECT_THIRD_OPTION ||
            event.getMenuAction() == MenuAction.GAME_OBJECT_FOURTH_OPTION ||
            event.getMenuAction() == MenuAction.GAME_OBJECT_FIFTH_OPTION) {
            
            lastMenuTarget = event.getMenuTarget();
            lastMenuOption = event.getMenuOption();
            lastMenuClickTime = System.currentTimeMillis();
            
            // Check if this is an agility obstacle interaction
            String obstacleType = identifyObstacleFromMenuTarget(lastMenuTarget);
            if (obstacleType != null) {
                Microbot.log("Interacted with " + obstacleType + " obstacle: " + lastMenuTarget);
                waitingForObstacleCompletion = true;
                expectedStateAfterObstacle = getNextStateAfterObstacle(obstacleType);
                obstacleInteractionStartTime = System.currentTimeMillis();
                
                // Record attempt for timeout tracking
                String obstacleKey = obstacleType + "_" + lastMenuOption;
                obstacleLastAttempt.put(obstacleKey, System.currentTimeMillis());
                obstacleAttemptCount.put(obstacleKey, obstacleAttemptCount.getOrDefault(obstacleKey, 0) + 1);
                
                Microbot.log("Expecting state change to: " + expectedStateAfterObstacle);
            }
        }
    }
    
    /**
     * Handle chat messages to detect obstacle completion confirmations
     */
    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() == ChatMessageType.GAMEMESSAGE || 
            event.getType() == ChatMessageType.SPAM) {
            
            String message = event.getMessage().toLowerCase();
            
            // Check for agility-related messages that indicate completion
            if (message.contains("you swing across") || 
                message.contains("you carefully") ||
                message.contains("you climb") ||
                message.contains("you balance") ||
                message.contains("you step across") ||
                message.contains("you squeeze through") ||
                message.contains("you squeeze-through") ||
                message.contains("squeeze through")) {
                
                Microbot.log("Obstacle completion detected via chat message: " + message);
                handleObstacleCompletionConfirmation();
            }
        }
    }
    
    /**
     * Handle stat changes to detect agility experience gains
     */
    @Subscribe
    public void onStatChanged(StatChanged event) {
        if (event.getSkill() == Skill.AGILITY) {
            int currentExperience = event.getXp();
            
            if (lastAgilityExperience > 0 && currentExperience > lastAgilityExperience) {
                int expGain = currentExperience - lastAgilityExperience;
                Microbot.log("Agility experience gained: " + expGain + " XP");
                
                // Experience gain confirms obstacle completion
                if (waitingForObstacleCompletion) {
                    handleObstacleCompletionConfirmation();
                }
            }
            
            lastAgilityExperience = currentExperience;
        }
    }
    
    /**
     * Enhanced game tick handler for memory/packet-based obstacle detection
     */
    @Subscribe
    public void onGameTick(GameTick event) {
        long currentTime = System.currentTimeMillis();
        Player localPlayer = Microbot.getClient().getLocalPlayer();
        
        if (localPlayer == null) return;
        
        WorldPoint currentPosition = localPlayer.getWorldLocation();
        
        // Track player movement for position-based completion detection
        if (lastPlayerPosition != null) {
            boolean hasMovedSignificantly = currentPosition.distanceTo(lastPlayerPosition) > 2; // Reduced from 3 to 2
            
            if (hasMovedSignificantly && waitingForObstacleCompletion && expectedStateAfterObstacle != null) {
                Microbot.log("Significant movement detected - checking if obstacle completed");
                Microbot.log("Current position: " + currentPosition + ", Expected state: " + expectedStateAfterObstacle);
                if (isPlayerInExpectedPositionForState(currentPosition, expectedStateAfterObstacle)) {
                    Microbot.log("Obstacle completion detected via position change");
                    handleObstacleCompletionConfirmation();
                } else {
                    Microbot.log("Player not yet in expected position for state: " + expectedStateAfterObstacle);
                }
            }
        }
        
        lastPlayerPosition = currentPosition;
        
        // Timeout handling for stuck obstacle attempts
        if (waitingForObstacleCompletion && 
            currentTime - obstacleInteractionStartTime > 8000) { // 8 second timeout (reduced from 15)
            
            Microbot.log("Obstacle attempt timeout after 8 seconds - checking position and forcing completion");
            
            // Check if player has actually moved past the obstacle
            if (expectedStateAfterObstacle != null && 
                isPlayerInExpectedPositionForState(currentPosition, expectedStateAfterObstacle)) {
                Microbot.log("Player is in correct position despite timeout - forcing completion");
                handleObstacleCompletionConfirmation();
            } else {
                Microbot.log("Player not in expected position - resetting state");
                resetObstacleWaitingState();
                
                // Try to recover by re-determining current state
                if (isInCourseArea()) {
                    WildyAgilState recoveryState = determineCurrentCourseState();
                    Microbot.log("Recovery state determined: " + recoveryState);
                    SCRIPT_STATE = recoveryState;
                }
            }
        }
        
        // Clean up old attempt records (older than 5 minutes)
        long cleanupThreshold = currentTime - 300000;
        obstacleLastAttempt.entrySet().removeIf(entry -> entry.getValue() < cleanupThreshold);
        
        // Reset attempt counts for obstacles that haven't been tried recently
        obstacleAttemptCount.entrySet().removeIf(entry -> {
            String obstacleKey = entry.getKey();
            return !obstacleLastAttempt.containsKey(obstacleKey) || 
                   obstacleLastAttempt.get(obstacleKey) < cleanupThreshold;
        });
    }
    
    /**
     * Memory/packet-based helper methods for obstacle detection
     */
    
    /**
     * Identify obstacle type from menu target text
     */
    private String identifyObstacleFromMenuTarget(String menuTarget) {
        if (menuTarget == null) return null;
        
        String target = menuTarget.toLowerCase();
        
        if (target.contains("pipe")) {
            return "PIPE";
        } else if (target.contains("rope") && target.contains("swing")) {
            return "ROPE_SWING";
        } else if (target.contains("stone") || target.contains("step")) {
            return "STEPPING_STONES";
        } else if (target.contains("log") || target.contains("balance")) {
            return "LOG_BALANCE";
        } else if (target.contains("rock") || target.contains("climb")) {
            return "ROCKS";
        }
        
        return null;
    }
    
    /**
     * Get the next state after completing an obstacle
     */
    private WildyAgilState getNextStateAfterObstacle(String obstacleType) {
        switch (obstacleType) {
            case "PIPE":
                return WildyAgilState.CROSSING_ROPE_SWING;
            case "ROPE_SWING":
                return WildyAgilState.CROSSING_STEPPING_STONES;
            case "STEPPING_STONES":
                return WildyAgilState.CROSSING_LOG_BALANCE;
            case "LOG_BALANCE":
                return WildyAgilState.CLIMBING_ROCKS;
            case "ROCKS":
                return WildyAgilState.COLLECTING_TOKENS;
            default:
                return null;
        }
    }
    
    /**
     * Handle confirmation that an obstacle has been completed
     */
    private void handleObstacleCompletionConfirmation() {
        if (!waitingForObstacleCompletion || expectedStateAfterObstacle == null) {
            return;
        }
        
        Microbot.log("Obstacle completion confirmed - transitioning to: " + expectedStateAfterObstacle);
        
        // Apply the expected state change
        SCRIPT_STATE = expectedStateAfterObstacle;
        
        // Check if this was the rocks (final obstacle) to trigger lap completion
        if (expectedStateAfterObstacle == WildyAgilState.COLLECTING_TOKENS) {
            onLapCompleted();
        }
        
        // Reset waiting state
        resetObstacleWaitingState();
        resetPathingCounters();
        initialMovementToObstacle = true;
    }
    
    /**
     * Check if player is in expected position for a given state
     */
    private boolean isPlayerInExpectedPositionForState(WorldPoint playerPos, WildyAgilState state) {
        switch (state) {
            case CROSSING_ROPE_SWING:
                return playerPos.distanceTo(AFTER_PIPE_LOCATION) < 6;
            case CROSSING_STEPPING_STONES:
                return playerPos.distanceTo(AFTER_ROPESWING_LOCATION) < 8;
            case CROSSING_LOG_BALANCE:
                return playerPos.distanceTo(AFTER_STEPPING_STONES_LOCATION) < 6;
            case CLIMBING_ROCKS:
                return playerPos.distanceTo(AFTER_LOG_BALANCE_LOCATION) < 6;
            case COLLECTING_TOKENS:
                return playerPos.distanceTo(AFTER_ROCKS_LOCATION) < 8;
            default:
                return false;
        }
    }
    
    /**
     * Reset obstacle waiting state variables
     */
    private void resetObstacleWaitingState() {
        waitingForObstacleCompletion = false;
        expectedStateAfterObstacle = null;
        lastMenuTarget = "";
        lastMenuOption = "";
        obstacleInteractionStartTime = 0;
    }
    
    /**
     * Enhanced obstacle interaction with memory/packet-based detection
     */
    private boolean interactWithObstacleEnhanced(int objectId, String objectName, String action) {
        // Perform the interaction
        boolean interactionResult = Rs2GameObject.interact(objectId, action);
        
        if (!interactionResult) {
            Microbot.log("Failed to interact with " + objectName);
            return false;
        }
        
        Microbot.log("Interacted with " + objectName + " - using memory/packet detection");
        
        // The memory/packet system will handle obstacle completion detection
        // through the event handlers (onMenuOptionClicked, onChatMessage, etc.)
        
        // Brief wait to allow the interaction to register
        Global.sleep(200, 400);
        
        return true;
    }

    /**
     * Memory/packet-based obstacle handling method
     * This method integrates with the event system to properly detect obstacle completion
     */
    private boolean handleObstacleWithMemoryPackets() {
        try {
            // Don't interact if already waiting for completion
            if (waitingForObstacleCompletion) {
                return false;
            }
            
            // Determine current obstacle based on state
            int objectId;
            String objectName;
            String action = "Cross"; // Default action
            WorldPoint expectedLocation;
            WildyAgilState nextState;
            
            switch (SCRIPT_STATE) {
                case ENTERING_COURSE:
                    objectId = PIPE_OBSTACLE_ID;
                    objectName = "Pipe";
                    action = "Squeeze-through";
                    expectedLocation = AFTER_PIPE_LOCATION;
                    nextState = WildyAgilState.CROSSING_ROPE_SWING;
                    break;
                case CROSSING_ROPE_SWING:
                    objectId = ROPESWING_OBSTACLE_ID;
                    objectName = "Rope swing";
                    action = "Swing-on";
                    expectedLocation = AFTER_ROPESWING_LOCATION;
                    nextState = WildyAgilState.CROSSING_STEPPING_STONES;
                    break;
                case CROSSING_STEPPING_STONES:
                    objectId = STEPPING_STONE_OBSTACLE_ID;
                    objectName = "Stepping stones";
                    expectedLocation = AFTER_STEPPING_STONES_LOCATION;
                    nextState = WildyAgilState.CROSSING_LOG_BALANCE;
                    break;
                case CROSSING_LOG_BALANCE:
                    objectId = LOG_BALANCE_OBSTACLE_ID;
                    objectName = "Log balance";
                    action = "Walk-across";
                    expectedLocation = AFTER_LOG_BALANCE_LOCATION;
                    nextState = WildyAgilState.CLIMBING_ROCKS;
                    break;
                case CLIMBING_ROCKS:
                    objectId = ROCKS_OBSTACLE_ID;
                    objectName = "Rocks";
                    action = "Climb";
                    expectedLocation = AFTER_ROCKS_LOCATION;
                    nextState = WildyAgilState.COLLECTING_TOKENS;
                    break;
                default:
                    Microbot.log("handleObstacleWithMemoryPackets called from invalid state: " + SCRIPT_STATE);
                    return false;
            }
            
            // Check if we're already past this obstacle
            WorldPoint playerPos = Rs2Player.getWorldLocation();
            if (isPlayerInExpectedPositionForState(playerPos, nextState)) {
                Microbot.log("Player already past obstacle, advancing to: " + nextState);
                SCRIPT_STATE = nextState;
                resetPathingCounters();
                initialMovementToObstacle = true;
                return true;
            }
            
            // Try to interact with the obstacle
            if (interactWithObstacleEnhanced(objectId, objectName, action)) {
                // Set up waiting state for memory/packet confirmation
                waitingForObstacleCompletion = true;
                expectedStateAfterObstacle = nextState;
                obstacleInteractionStartTime = System.currentTimeMillis();
                
                Microbot.log("Started obstacle interaction: " + objectName + ", waiting for memory/packet confirmation");
                return false; // Don't advance state immediately
            } else {
                // Failed to interact, try pathing closer
                WorldPoint obstacleLocation = getExpectedLocationForState(SCRIPT_STATE);
                if (obstacleLocation != null && playerPos.distanceTo(obstacleLocation) > 3) {
                    Rs2Walker.walkTo(obstacleLocation, 1);
                }
                return false;
            }
            
        } catch (Exception e) {
            Microbot.log("Error in handleObstacleWithMemoryPackets: " + e.getMessage());
            resetObstacleWaitingState();
            return false;
        }
    }
    
    /**
     * Get expected player location for each state (where obstacles are)
     */
    private WorldPoint getExpectedLocationForState(WildyAgilState state) {
        switch (state) {
            case ENTERING_COURSE:
                return PIPE_LOCATION;
            case CROSSING_ROPE_SWING:
                return ROPESWING_LOCATION;
            case CROSSING_STEPPING_STONES:
                return STEPPING_STONE_LOCATION;
            case CROSSING_LOG_BALANCE:
                return LOG_BALANCE_LOCATION;
            case CLIMBING_ROCKS:
                return ROCKS_LOCATION;
            default:
                return null;
        }
    }

    // Missing helper methods that were referenced in the original code
    private void resetPathingCounters() {
        pathingFailureCount = 0;
        obstacleFailureCount = 0;
        lastFailedObstacle = "";
        lastPathingFailureTime = 0;
    }

    private boolean needsEmergencyRecovery() {
        return pathingFailureCount > 10 || obstacleFailureCount > 5;
    }

    private void handleSeverePathingIssues() {
        Microbot.log("Emergency recovery triggered - severe pathing issues detected");
        resetPathingCounters();
        resetObstacleWaitingState();
        
        // Try to recover by determining current state
        if (isInCourseArea()) {
            SCRIPT_STATE = determineCurrentCourseState();
        } else {
            SCRIPT_STATE = WildyAgilState.WALKING_TO_COURSE_ENTRANCE;
        }
        
        initialMovementToObstacle = true;
    }

    private boolean interactWithAnyGate(String action) {
        for (int gateId : ENTRANCE_GATE_IDS) {
            if (Rs2GameObject.interact(gateId, action)) {
                return true;
            }
        }
        return false;
    }
}
