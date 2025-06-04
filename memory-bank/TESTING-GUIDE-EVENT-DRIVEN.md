# Wilderness Agility Event-Driven Testing Guide

## 🧪 How to Test the Enhanced Script

### **Pre-Testing Setup**
1. **Position**: Start your character near or inside the Wilderness Agility Course
2. **Inventory**: Ensure you have coins for gate fees if needed
3. **Settings**: Enable auto-run in game settings
4. **Console**: Keep RuneLite console open to monitor debug logs

### **What to Look For**

#### ✅ **Animation Detection Logs**
```
[INFO] Player started PIPE animation (ID: 751)
[INFO] Obstacle completed: PIPE
[INFO] Set pending state change to: CROSSING_ROPE_SWING
[INFO] Applying pending state change: CROSSING_ROPE_SWING
```

#### ✅ **Enhanced Interaction Logs**
```
[INFO] Interacted with Obstacle Pipe - waiting for animation or completion
[INFO] Animation detected for Obstacle Pipe, waiting for completion
```

#### ✅ **Coordinate Validation (if corruption occurs)**
```
[WARN] WARNING: Detected Y-coordinate corruption in walk destination: (2998, 3331, 0)
[INFO] Using corrected destination: (2998, 3931, 0) instead of (2998, 3331, 0)
```

#### ✅ **State Progression**
```
[INFO] Successfully completed obstacle to (2998, 3931, 0)
[INFO] ENTERING_COURSE -> CROSSING_ROPE_SWING
```

### **Expected Behavior**

#### **Normal Operation**:
1. Script walks to obstacle position
2. Interacts with obstacle  
3. Animation detection triggers immediately
4. State changes automatically after completion
5. Continues to next obstacle seamlessly

#### **Error Recovery**:
1. If animation timeout: Script attempts recovery
2. If pathing fails: Alternative routes attempted  
3. If coordinates corrupt: Automatic fixing applied
4. If severe issues: Emergency recovery activated

### **Manual Testing Scenarios**

#### **Test 1: Normal Course Run**
- Start at course entrance
- Let script run through complete lap
- Verify all 5 obstacles are completed
- Check for lap completion message

#### **Test 2: Mid-Course Recovery**  
- Start character at random position in course
- Script should determine current state and continue
- Verify proper state detection

#### **Test 3: Interruption Recovery**
- Manually move character during obstacle interaction
- Script should detect and recover appropriately
- Check emergency recovery mechanisms

#### **Test 4: Animation Timeout**
- Look for any stuck animations (rare)
- Verify 10-second timeout triggers recovery
- Check state reset functionality

### **Performance Metrics**

#### **Success Indicators**:
- ✅ All 5 obstacles completed per lap
- ✅ No infinite loops or stuck states  
- ✅ Smooth state transitions
- ✅ Proper animation detection
- ✅ Coordinate corruption handling

#### **Warning Signs**:
- ⚠️ Repeated obstacle interaction attempts
- ⚠️ Animation timeouts
- ⚠️ Coordinate corruption warnings
- ⚠️ Emergency recovery activations

### **Common Issues & Solutions**

#### **Issue**: Script doesn't detect animations
**Solution**: Check animation IDs may need adjustment for wilderness course

#### **Issue**: Coordinate corruption still occurs  
**Solution**: Validation system should auto-fix, check logs for warnings

#### **Issue**: State transitions too slow
**Solution**: Reduce 2-second delay in pending state changes

#### **Issue**: Script gets stuck at obstacles
**Solution**: Enhanced interaction and recovery should handle this

### **Debug Commands for Advanced Testing**

```java
// Force state change for testing
SCRIPT_STATE = WildyAgilState.CROSSING_ROPE_SWING;

// Check current animation
int currentAnim = Microbot.getClient().getLocalPlayer().getAnimation();

// Verify coordinate validation
WorldPoint testPoint = new WorldPoint(2998, 3331, 0);
WorldPoint fixed = validateAndFixCoordinates(testPoint, "test");
```

### **Expected Output Example**
```
[INFO] Initialized agility animations map with 5 obstacles
[INFO] Starting Wilderness Agility...
[INFO] Player started PIPE animation (ID: 751)
[INFO] Set pending state change to: CROSSING_ROPE_SWING  
[INFO] Obstacle completed: PIPE
[INFO] Applying pending state change: CROSSING_ROPE_SWING
[INFO] Successfully completed obstacle to (2998, 3931, 0)
[INFO] Player started ROPE_SWING animation (ID: 1118)
[INFO] Set pending state change to: CROSSING_STEPPING_STONES
...
[INFO] Rocks completed - course lap finished!
[INFO] Lap 1 reward: 8049 (Alchables: 4024, Noted blighted: 4025)
```

---

## 🎯 **Ready to Test!**

The event-driven system is now fully implemented and ready for real-world testing. The enhanced animation detection should solve all the previous issues with obstacle completion and course navigation.

**Happy Testing!** 🎮
