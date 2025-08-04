# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

This is a Microbot repository - an open-source Old School RuneScape (OSRS) client based on RuneLite that uses a plugin system to enable scripting and automation.

## Build and Development Commands

### Building the Project
```bash
# Build the entire project
mvn clean install -DskipTests

# Build with tests
mvn clean install

# Build a specific module
mvn clean install -pl runelite-client -am
```

### Running Microbot
```bash
# Run the client from command line
mvn exec:exec -pl runelite-client

# Run with specific configuration
java -jar runelite-client/target/client-1.11.12-SNAPSHOT.jar
```

### Testing
```bash
# Run all tests
mvn test

# Run tests for a specific module
mvn test -pl runelite-client

# Run a specific test class
mvn test -Dtest=TestClassName
```

### Linting and Code Quality
```bash
# Check code style (when enabled)
mvn checkstyle:check

# Format code
mvn spotless:apply
```

## Architecture and Structure

### Project Structure
- **runelite-api/**: Core API interfaces and data structures
- **runelite-client/**: Main client application and plugin implementations
  - **plugins/microbot/**: All Microbot-specific plugins and utilities
    - **util/**: Utility classes prefixed with "Rs2" (e.g., Rs2Player, Rs2Inventory)
    - Individual plugin folders containing Plugin, Script, Config, and Overlay classes
- **cache/**: Game cache handling
- **runelite-jshell/**: JShell integration for scripting
- **runelite-maven-plugin/**: Maven build plugins

### Plugin Architecture

#### Core Components
1. **Plugin Class**: Main entry point extending `Plugin`
   - Handles lifecycle (startUp/shutDown)
   - Subscribes to game events
   - Manages dependencies via @Inject

2. **Script Class**: Contains automation logic extending `Script`
   - Implements the main bot loop
   - Uses scheduled executor for periodic tasks
   - Manages state machines for complex behaviors

3. **Config Interface**: Plugin configuration extending `Config`
   - Uses @ConfigGroup for grouping
   - @ConfigItem for individual settings
   - @ConfigSection for organizing options

4. **Overlay Class**: Visual feedback extending `OverlayPanel`
   - Renders information on game screen
   - Updates based on plugin state

### Microbot Conventions

#### Naming Conventions
- Utility classes: Prefixed with "Rs2" (e.g., Rs2Bank, Rs2Combat)
- Plugin classes: Suffixed with "Plugin"
- Script classes: Suffixed with "Script"
- Config interfaces: Suffixed with "Config"
- Overlay classes: Suffixed with "Overlay"

#### Plugin Development Pattern
```java
@PluginDescriptor(
    name = PluginDescriptor.Mocrosoft + "PluginName",
    description = "Description",
    tags = {"microbot", "tag1", "tag2"},
    enabledByDefault = false
)
public class MyPlugin extends Plugin {
    @Inject private MyScript script;
    @Inject private MyConfig config;
    @Inject private OverlayManager overlayManager;
    @Inject private MyOverlay overlay;
    
    @Override
    protected void startUp() {
        overlayManager.add(overlay);
        script.run(config, this);
    }
    
    @Override
    protected void shutDown() {
        script.shutdown();
        overlayManager.remove(overlay);
    }
}
```

### Key Utility Classes

- **Microbot**: Central access point for client instance and utilities
- **Rs2Player**: Player actions and state
- **Rs2Inventory**: Inventory management
- **Rs2Bank**: Banking operations
- **Rs2GameObject**: Game object interactions
- **Rs2Walker**: Pathfinding and movement
- **Rs2Combat**: Combat-related utilities
- **Rs2Antiban**: Anti-ban measures

### State Management

Scripts typically use enum-based state machines:
```java
public enum State {
    BANKING, WALKING, SKILLING, WAITING
}
```

State transitions are managed through:
- `changeState()`: Updates current state
- `lockState()`: Prevents state changes during critical operations
- `resetActions`: Flags when actions need to be re-evaluated

### Event Handling

Common event subscriptions:
- `@Subscribe onChatMessage`: Process game messages
- `@Subscribe onStatChanged`: Track skill changes
- `@Subscribe onGameTick`: Game tick updates
- `@Subscribe onHitsplatApplied`: Combat feedback

### Best Practices

1. **Error Handling**: Wrap main loops in try-catch blocks
2. **Sleep Patterns**: Use `sleepUntil()` for condition-based waiting
3. **Anti-ban**: Integrate Rs2Antiban for human-like behavior
4. **Resource Management**: Clean up resources in shutdown()
5. **State Persistence**: Track plugin statistics and state
6. **Configuration**: Provide sensible defaults in Config interfaces

## Development Tips

1. **Testing Plugins**: Use the example scripts as templates
2. **Debugging**: Enable debug logging with `Microbot.log()`
3. **Performance**: Use scheduled executors with appropriate delays
4. **Safety**: Always validate game state before actions
5. **Coordinates**: Use WorldPoint for absolute positions, LocalPoint for relative

## Common Issues and Solutions

1. **Plugin Not Loading**: Check @PluginDescriptor annotations
2. **NullPointerException**: Ensure proper @Inject usage
3. **State Stuck**: Implement timeout mechanisms
4. **Performance Issues**: Optimize loop frequencies and conditions

## JstWinter Plugin TODO List

### Completed High Priority Tasks ✓
- ✓ **Fixed pyromancer healing detection** - Changed from GameObject to NPC detection with proper interaction handling
- ✓ **Improved state machine robustness** - Added 30-second timeout mechanism, death handling, random event detection, region validation
- ✓ **Added error recovery mechanisms** - Implemented region checks, null safety, interaction failure handling, and auto-recovery
- ✓ **Implemented smart brazier selection** - Automatic brazier selection based on player count and brazier status

### Completed Medium Priority Tasks ✓
- ✓ **Added comprehensive statistics tracking** - XP/hr rates for all skills, points/hr, crates/hr, detailed overlay
- ✓ **Implemented solo Wintertodt support** - Specialized strategy for 13500+ points with conservative eating and point targeting
- ✓ **Added world hopping system** - Automatic world hopping based on player count, crash detection, and world preferences

### Medium Priority
5. **Add comprehensive statistics tracking** - Track XP/hr, points/hr, crates/hr, and other metrics
6. **Implement world hopping** - Hop worlds when too crowded or facing competition
7. **Add solo Wintertodt support** - Different strategies and point thresholds for solo mode
8. **Improve snowfall dodging** - Implement predictive movement patterns
9. **Add configurable break patterns** - Wintertodt-specific break timing
10. **Optimize pathfinding** - Reduce downtime between activities
11. **Implement anti-crash features** - Handle interruptions from other players
12. **Better food management** - Support combo foods, brews + restores
13. **Create unit tests** - Test critical game logic and state transitions

### Low Priority
14. **Add potion support** - Stamina, divine super combat for efficiency
15. **Inventory optimization** - Drop empty vials, organize items
16. **Alternative banking methods** - Construction cape, other teleports
17. **Webhook/Discord notifications** - Notify on milestones and errors
18. **Phoenix pet detection** - React appropriately to pet drops
19. **Auto-open supply crates** - Open rewards automatically
20. **Dynamic point targets** - Adjust based on Firemaking level

### Known Issues
- Pyromancer healing uses GameObject instead of NPC detection
- State locking mechanism can cause stuck states
- Rejuvenation potion handling needs optimization
- Missing validation for brazier accessibility

## JstWC Plugin (2-Tick Woodcutting)

### Overview
JstWC is a specialized plugin for 2-tick woodcutting teaks on the Isle of Souls using bird aggression. This advanced technique allows players to potentially receive logs every 2 ticks instead of the normal 4 ticks.

### Implementation Details
- **Location**: Isle of Souls (Area: 2184, 2993, 2190, 2987)
- **Method**: Uses birds (NPC IDs: 5241, 10541, 5240) for aggression timing
- **Target**: Teak trees (Object ID: 40758)
- **Equipment**: Shortbow on rapid (no ammo) or Hunter's crossbow with shield
- **Timing**: 50ms loop intervals for precise tick timing

### Key Features
- Automatic bird aggression setup using Confuse/Weaken spells
- Precise 2-tick timing with hitsplat detection
- Multiple drop patterns (alternating, sequential, random)
- Health monitoring and food consumption
- Regen bracelet support for damage mitigation
- Comprehensive overlay with statistics tracking

### Configuration Options
- Combat settings: health threshold, food type, equipment preferences
- Drop management: pattern selection, log retention amounts
- Antiban integration with break handling

### State Machine
```java
enum JstWCState {
    INITIALIZING, SETTING_UP_AGGRESSION, POSITIONING,
    TWO_TICK_CUTTING, DROPPING_LOGS, EATING_FOOD,
    REGAINING_AGGRESSION, WAITING, ERROR
}
```

### Critical Implementation Notes
- Requires Auto Retaliate to be enabled
- Needs 2+ birds attacking simultaneously for proper timing
- Uses alternating tree clicks on hit ticks and ground clicks on off-ticks
- Monitors aggression status and automatically re-establishes when lost

### Equipment Requirements
- **Essential**: Any axe (equipped or inventory)
- **Combat Gear**: Shortbow on rapid (no ammo) OR Hunter's crossbow + shield
- **Optional**: Regen bracelet for damage mitigation
- **Consumables**: Food (configurable type), runes for Confuse/Weaken spells

### JstWC TODO List - Auto-Buy & Auto-Walk Features

#### High Priority - In Development
1. **Equipment Auto-Buy System**
   - Add states: CHECKING_EQUIPMENT, BANKING_FOR_EQUIPMENT, BUYING_EQUIPMENT, COLLECTING_PURCHASES
   - Integrate Rs2GrandExchange for automatic purchasing
   - Implement equipment validation with fallback options
   - Add budget management and price checking

2. **Auto-Walking to Isle of Souls**
   - Add states: TRAVELING_TO_LOCATION, PREPARING_FOR_TRAVEL
   - Implement banking at nearest location using Rs2Bank
   - Add teleport support via Rs2Walker
   - Create route optimization from common starting points

#### Implementation Guide

##### Equipment Auto-Buy Flow
```java
// Equipment priority lists
List<String> bowOptions = List.of("Shortbow", "Oak shortbow", "Willow shortbow");
List<String> crossbowOptions = List.of("Hunter's crossbow", "Bronze crossbow");
List<String> foodOptions = List.of("Lobster", "Swordfish", "Monkfish");

// State transitions
CHECKING_EQUIPMENT -> BANKING_FOR_EQUIPMENT (if items in bank)
                   -> BUYING_EQUIPMENT (if need to purchase)
BUYING_EQUIPMENT -> COLLECTING_PURCHASES -> PREPARING_FOR_TRAVEL
```

##### Auto-Walk Implementation
```java
// Banking locations by proximity
BankLocation.VARROCK_WEST // For GE access
BankLocation.EDGEVILLE // Alternative with glory teleport
BankLocation.FALADOR_WEST // For ring of wealth users

// Travel methods to Isle of Souls
1. Charter ship from Port Sarim
2. Digsite pendant -> Fossil Island -> Row boat
3. Skills necklace -> Farming Guild -> Walk south
```

##### Configuration Additions
```java
@ConfigSection(name = "Auto-Buy Settings", position = 3)
// Enable auto-buy, budget limits, preferred equipment

@ConfigSection(name = "Travel Settings", position = 4)
// Banking location, teleport methods, travel supplies
```

#### Utility Integration Points
- **Rs2Bank**: Full banking API with location support
- **Rs2GrandExchange**: Buy/sell operations, price checking
- **Rs2Equipment**: Validate equipped items
- **Rs2Walker**: Advanced pathfinding with teleport support
- **Rs2Inventory**: Manage inventory for travel

#### Testing Considerations
- Test from various starting locations (Lumbridge, Varrock, etc.)
- Validate GE buying with different budget constraints
- Ensure proper error handling when equipment unavailable
- Test travel routes with different teleport options