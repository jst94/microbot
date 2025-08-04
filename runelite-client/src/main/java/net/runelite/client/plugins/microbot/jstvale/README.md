# JstVale - Vale Totems Automation Script

## Overview
JstVale is an automation script for the Vale Totems minigame in Old School RuneScape. It automates the process of constructing and decorating totems around the valley to earn Construction and Fletching experience.

## Features
- **Automated Totem Construction**: Builds totems at all 8 locations around the valley
- **Fletching Integration**: Optional log fletching before decoration for maximum XP
- **Smart Routing**: Configurable clockwise routes to maximize ent trail activations
- **Statistics Tracking**: Tracks totems completed, runs finished, and hourly rates
- **Break System**: Integrated with Microbot's break handler
- **Log Basket Support**: Uses log basket to carry more materials per trip
- **Offering Collection**: Automatically collects vale offerings

## Configuration Options

### General Settings
- **Enable Breaks**: Random breaks during script execution
- **Use Graceful Outfit**: Wear graceful for stamina efficiency  
- **Use Log Basket**: Carry more logs per trip
- **Collect Offerings**: Gather vale offerings during runs

### Materials
- **Log Type**: Choose from Regular to Magic logs
- **Fletch Logs**: Convert logs to longbows before decorating
- **Logs Per Run**: Number of logs to take (minimum 40 for full run)

### Routes & Strategy
- **Starting Totem**: Which totem to begin the route from
- **Clockwise Route**: Recommended for maximizing ent trails
- **Agility Shortcuts**: Use shortcuts between totems
- **Min Agility Level**: Minimum level for continuous running

## Experience Rates
Based on wiki data:
- **Fletching**: 20,000-350,000 XP/hour (depending on log type)
- **Construction**: 5,750-11,385 XP/hour (based on Construction level)
- **Completion Rate**: ~90 totems per hour

## Requirements
- Access to Vale Totems minigame
- Logs (type depends on configuration)
- Knife (if fletching enabled)
- Log basket (optional but recommended)
- Graceful outfit (recommended for stamina)
- Agility level ~70 recommended for best efficiency

## Important Notes

### Coordinates and Object IDs
⚠️ **REQUIRES MANUAL SETUP**: The following need to be updated with actual game data:

1. **Totem Locations** (`TotemLocation.java`):
   - Currently contains placeholder coordinates
   - Need actual WorldPoint coordinates for all 8 totem sites

2. **Bank Location** (`JstValeScript.java`):
   - `AUBURNVALE_BANK` needs correct Auburnvale bank coordinates

3. **Game Object Names/IDs**:
   - "Totem site" - actual construction object
   - "Constructed totem" - completed totem object  
   - "Vale offering" - offering collection object
   - Log basket object interaction

### Setup Instructions
1. Visit Vale Totems minigame area in-game
2. Record coordinates for all 8 totem locations
3. Find Auburnvale bank coordinates
4. Identify correct object names/IDs using developer tools
5. Update the placeholder values in the code

## File Structure
```
jstvale/
├── JstValePlugin.java       # Main plugin class
├── JstValeScript.java       # Core automation logic
├── JstValeConfig.java       # Configuration interface
├── JstValeOverlay.java      # UI overlay
├── enums/
│   ├── JstValeState.java    # Script states
│   └── TotemLocation.java   # Totem positions
└── README.md               # This documentation
```

## Usage
1. Configure desired settings in plugin panel
2. Ensure you have required materials in bank
3. Start near Auburnvale bank or first totem
4. Enable the plugin to begin automation

## Safety Features
- Anti-ban integration with Rs2Antiban
- Break handler compatibility
- Error handling and recovery
- State timeout mechanisms
- Inventory validation

## Future Enhancements
- Auto-detection of totem coordinates
- Advanced agility shortcut routing
- Ent trail optimization
- Performance analytics
- Multiple route strategies

---
*Created for the Microbot automation framework*